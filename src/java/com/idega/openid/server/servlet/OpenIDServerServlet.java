package com.idega.openid.server.servlet;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openid4java.association.AssociationException;
import org.openid4java.message.AuthFailure;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.DirectError;
import org.openid4java.message.Message;
import org.openid4java.message.MessageException;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.server.InMemoryServerAssociationStore;
import org.openid4java.server.ServerException;
import org.openid4java.server.ServerManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.contact.data.Email;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.openid.OpenIDConstants;
import com.idega.openid.server.bean.OpenIDServerBean;
import com.idega.openid.server.dao.OpenIDServerDAO;
import com.idega.openid.server.data.AuthorizedAttribute;
import com.idega.openid.server.data.ExchangeAttribute;
import com.idega.openid.util.OpenIDUtil;
import com.idega.presentation.IWContext;
import com.idega.user.business.NoEmailFoundException;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.expression.ELUtil;
import com.idega.util.text.TextSoap;

public class OpenIDServerServlet extends HttpServlet {

	private static final long serialVersionUID = -7832846183877408861L;
	
	private static Logger LOGGER = Logger.getLogger(OpenIDServerServlet.class.getName());
	
	@Autowired
	private OpenIDServerDAO dao;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ServerManager manager = getServerManager();

		// extract the parameters from the request
        ParameterList request = new ParameterList(req.getParameterMap());
        
        OpenIDServerBean serverBean = ELUtil.getInstance().getBean("openIDServerBean");
        Map parameterMap = serverBean.getParameterMap();
        if (request.getParameters().size() == 0 && parameterMap != null) {
        	request = new ParameterList(parameterMap);
        }

        String mode = request.hasParameter(OpenIDConstants.PARAMETER_OPENID_MODE) ? request.getParameterValue(OpenIDConstants.PARAMETER_OPENID_MODE) : null;
        String realm = request.hasParameter(OpenIDConstants.PARAMETER_REALM) ? request.getParameterValue(OpenIDConstants.PARAMETER_REALM) : null;
        if (realm != null) {
        	serverBean.setReturnUrl(realm);
			if (realm.indexOf("http://") != -1) {
				realm = TextSoap.findAndCut(realm, "http://");
			} else if (realm.indexOf("https://") != -1) {
				realm = TextSoap.findAndCut(realm, "https://");
			}
			if (realm.indexOf("/") != -1) {
				realm = realm.substring(0, realm.indexOf("/"));
			}
			serverBean.setRealm(realm);
        }
        
        Message response;
        String responseText = null;

        try {
	        if (OpenIDConstants.PARAMETER_ASSOCIATE.equals(mode)) {
	            // --- process an association request ---
	            response = manager.associationResponse(request);
	            responseText = response.keyValueFormEncoding();
	        }
	        else if (OpenIDConstants.PARAMETER_CHECKID_SETUP.equals(mode) || OpenIDConstants.PARAMETER_CHECKID_IMMEDIATE.equals(mode)) {
	        	IWContext iwc = new IWContext(req, resp, getServletContext());
	        	
	        	boolean goToLogin = true;
	        	//Log user out if this is an authentication request for a new association 
	        	//(i.e. another Relying Party or an expired association) or if this is a new request
	        	//after a completed successful one (loginExpireTime is removed on successful login)
	        	String loginExpireHandle = request.hasParameter(OpenIDConstants.PARAMETER_ASSOCIATE_HANDLE) ? "login-"+request.getParameterValue(OpenIDConstants.PARAMETER_ASSOCIATE_HANDLE) : null;
	        	if(loginExpireHandle==null){
	        		throw new UnsupportedOperationException("Missing openid.assoc_handle");
	        	}
	        	Date loginExpirationTime = (Date)iwc.getSessionAttribute(loginExpireHandle);
	        	
	        	Date currentTime = new Date();
	        	if(loginExpirationTime == null || currentTime.after(loginExpirationTime)){
	        		if(iwc.isLoggedOn()){
	        			//Make user log in again
	        			LoginBusinessBean loginBusiness = getLoginBusiness(iwc.getRequest());
	        			loginBusiness.logOutUser(iwc);
	        		}
	        		int expireInMilliSeconds = manager.getExpireIn()*1000;
	        		iwc.setSessionAttribute(loginExpireHandle, new Date(currentTime.getTime()+expireInMilliSeconds));
	        		goToLogin = true;
	        	} else {
	        		//coming here again in the same request/association
	        		goToLogin = !iwc.isLoggedOn();
	        	}
	        	
	        	if (!goToLogin) {
	        		serverBean.setParameterMap(null);
	        		serverBean.setServerUrl(null);
	        		serverBean.setDoRedirect(null);
	        		serverBean.setUsername(null);
	        		
		            // interact with the user and obtain data needed to continue
	        		User user = iwc.getCurrentUser();		
		            String userSelectedClaimedId = getUserSelectedClaimedId(iwc, user);
		            
		            // --- process an authentication request ---
		            AuthRequest authReq = AuthRequest.createAuthRequest(request, manager.getRealmVerifier());
		
		            storeRequestedAttributesToSession(iwc, authReq);
		            
		            Boolean authenticatedAndApproved = isAuthenticatedAndApproved(iwc, user, authReq);
		            
		            String opLocalId = null;
		            // if the user chose a different claimed_id than the one in request
		            if (userSelectedClaimedId != null && !userSelectedClaimedId.equals(authReq.getClaimed())) {
		                opLocalId = userSelectedClaimedId;
		            }
		
		            response = manager.authResponse(request,
		                    opLocalId,
		                    userSelectedClaimedId,
		                    authenticatedAndApproved.booleanValue(),
		                    false); // Sign after we added extensions.
		
		            if (response instanceof DirectError) {
		            	directResponse(resp, response.keyValueFormEncoding());
		            }
		            else if (response instanceof AuthFailure) {
		            	serverBean.setParameterMap(req.getParameterMap());
		        		
		        		String URL = req.getScheme() + "://" + req.getServerName() + (req.getServerPort() != 80 ? ":" + req.getServerPort() : "") + req.getRequestURI() + "?" + req.getQueryString();
		        		serverBean.setServerUrl(URL);

		        		String authenticateURL = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_AUTHENTICATION_URL, "http://www.elykill.is/pages/profile/mypage/authenticate/");
		        		
		        		
		        		
//		        		OpenIDAttributesBean attr = ELUtil.getInstance().getBean("openIDAttributesBean");
//			            List userData = userInteraction(iwc, user);
//		        		attr.setEmail((String) userData.get(0));
//			            attr.setPersonalID((String) userData.get(1));
//			            attr.setFullName((String) userData.get(2));
//			            attr.setDateOfBirth((String) userData.get(3));
//			            attr.setGender((String) userData.get(4));
		        		
		        		resp.sendRedirect(authenticateURL);
		        		return;
		            }
		            else {
		            	StringBuffer attributesLog = new StringBuffer();
		                if (authReq.hasExtension(AxMessage.OPENID_NS_AX)) {
		                    MessageExtension ext = authReq.getExtension(AxMessage.OPENID_NS_AX);
		                    if (ext instanceof FetchRequest) {
		                        FetchRequest fetchReq = (FetchRequest) ext;
		                        Map<String,String> requestedAttributes = fetchReq.getAttributes();
		                        Map userDataExt = new HashMap();
		                		
	                            FetchResponse fetchResp = FetchResponse.createFetchResponse(fetchReq, userDataExt);
	                            
//		                        for(String alias : requestedAttributes.keySet()){
//		                        	String attrType = requestedAttributes.get(alias);
//		                        	if(serverBean.getIncludeAttributeInResponce(alias,attrType)){
//			                        	String value = getAttributeValue(iwc,user,alias,attrType);
//			                        	if(value==null){
//			                        		value="";
//			                        	}
//			                        	fetchResp.addAttribute(alias, attrType, value);
//			                        	//write log entry
//			                        	attributesLog.append('[').append(alias).append(';').append(attrType)
//			                        	//.append(';').append(value)
//			                        	.append("],");
//		                        	} 
//		                        }
//		                        //If FetchRequest is now, for any reason, asking for more 
	                            //attributes than have been processed it will not be returned
	                            //Only known attributes should have been processed
		                        List<AuthorizedAttribute> s = serverBean.getRequestedAttributes();
		                        Set<String> keys = requestedAttributes.keySet();
		                        for(AuthorizedAttribute a : s){
		                        	ExchangeAttribute attr = a.getExchangeAttribute();
		                        	String alias = attr.getName();
		                        	String type = attr.getType();
		                        	if(keys.contains(alias) && type.equals(requestedAttributes.get(alias))){
		                        		String value = getAttributeValue(iwc,user,alias,type);
			                        	if(value==null){
			                        		value="";
			                        	}
			                        	fetchResp.addAttribute(alias, type, value);
			                        	//write log entry
			                        	attributesLog.append('[').append(alias).append(';').append(type)
			                        	//.append(';').append(value)
			                        	.append("],");
		                        	} else {
		                        		//FetchRequest not asking for this attribute
		                        		throw new UnsupportedOperationException("Processed and requested attributes do not match.");
		                        	}
		                        }
		                        
		                        response.addExtension(fetchResp);
		                    }
		                    else /*if (ext instanceof StoreRequest)*/ {
		                        throw new UnsupportedOperationException("TODO");
		                    }
		                }
		
		                // Sign the auth success message.
		                // This is required as AuthSuccess.buildSignedList has a `todo' tag now.
		                manager.sign((AuthSuccess) response);
		
		                // caller will need to decide which of the following to use:
		
		                // option1: GET HTTP-redirect to the return_to URL
//		                cleanUpBeforeReturning(iwc, loginExpireHandle);
		                //Clean up before returning
		                serverBean.invalidate();
		        		
		        		getDAO().createLogEntry(user.getUniqueId(), realm, attributesLog.toString());
		        		
		                resp.sendRedirect(response.getDestinationUrl(true));
		                return;
		
		                // option2: HTML FORM Redirection
		                //RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("formredirection.jsp");
		                //httpReq.setAttribute("prameterMap", response.getParameterMap());
		                //httpReq.setAttribute("destinationUrl", response.getDestinationUrl(false));
		                //dispatcher.forward(request, response);
		                //return null;
		            }
	        	}
	        	else {
	        		
	        		String URL = req.getScheme() + "://" + req.getServerName() + (req.getServerPort() != 80 ? ":" + req.getServerPort() : "") + req.getRequestURI() + "?" + req.getQueryString();
	        		String subdomain = new OpenIDUtil().getSubDomain(request.getParameterValue("openid.claimed_id"));
	        		
	        		serverBean.setParameterMap(req.getParameterMap());
	        		serverBean.setServerUrl(URL);
	        		serverBean.setDoRedirect(Boolean.TRUE);
	        		serverBean.setUsername(subdomain);

	        		resp.sendRedirect(manager.getUserSetupUrl());
	        		return;
	        	}
	        }
	        else if (OpenIDConstants.PARAMETER_CHECK_AUTHENTICATION.equals(mode)) {
	            // --- processing a verification request ---
	            response = manager.verify(request);
	            responseText = response.keyValueFormEncoding();
	        }
	        else {
	            // --- error response ---
	            response = DirectError.createDirectError("Unknown request");
	            responseText = response.keyValueFormEncoding();
	        }
        }
        catch (MessageException me) {
        	me.printStackTrace();
        	responseText = me.getMessage();
        }
        catch (AssociationException ae) {
        	ae.printStackTrace();
        	responseText = ae.getMessage();
        }
        catch (ServerException se) {
			se.printStackTrace();
        	responseText = se.getMessage();
		}
        
        // return the result to the user
        directResponse(resp, responseText);
    }

	private String getUserSelectedClaimedId(IWContext iwc, User user) {
		String identityFormat = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_OPENID_IDENTITY_FORMAT, "http://{0}.elykill.is/pages/profile/mypage/");
		String identity = MessageFormat.format(identityFormat, getUserBusiness(iwc).getUserLogin(user));
		return identity;
	}

//	private void cleanUpBeforeReturning(IWContext iwc, String loginExpireHandle) {
//		//Make user log in again next time
//		LoginBusinessBean loginBusiness = getLoginBusiness(iwc.getRequest());
//		loginBusiness.logOutUser(iwc);
//		
//		
//		//Session is invalidated when logged out, hence no need to do this
////		iwc.getSession().removeAttribute(OpenIDConstants.ATTRIBUTE_SUBDOMAIN);
////		iwc.getSession().removeAttribute(OpenIDConstants.ATTRIBUTE_ALLOWED_REALM);
////
////		iwc.getSession().removeAttribute(OpenIDConstants.PARAMETER_REQUIRED_ATTRIBUTES);
////		iwc.getSession().removeAttribute(OpenIDConstants.PARAMETER_ALL_ATTRIBUTES);
////		
////		iwc.getSession().removeAttribute(loginExpireHandle);
//	}

	protected String getAttributeValue(IWContext iwc, User user, String alias, String type){
		
		if(OpenIDConstants.ATTRIBUTE_ALIAS_EMAIL.equals(alias) && OpenIDConstants.ATTRIBUTE_TYPE_EMAIL.equals(type)){
			Email email = null;
			try {
				email = getUserBusiness(iwc).getUsersMainEmail(user);
			}
			catch (NoEmailFoundException e) { /*No action...*/ } 
			catch (RemoteException e) {
				e.printStackTrace();
			}
			
			return (email != null ? email.getEmailAddress() : "");
		} else if(OpenIDConstants.ATTRIBUTE_ALIAS_PERSONAL_ID.equals(alias) && OpenIDConstants.ATTRIBUTE_TYPE_PERSONAL_ID.equals(type)){
			return (user.getPersonalID() != null ? user.getPersonalID() : "");
		} else if(OpenIDConstants.ATTRIBUTE_ALIAS_FULL_NAME.equals(alias) && OpenIDConstants.ATTRIBUTE_TYPE_FULL_NAME.equals(type)){
			return user.getName();
		} else if(OpenIDConstants.ATTRIBUTE_ALIAS_DATE_OF_BIRTH.equals(alias) && OpenIDConstants.ATTRIBUTE_TYPE_DATE_OF_BIRTH.equals(type)){
			return (user.getDateOfBirth() != null ? Long.toString(user.getDateOfBirth().getTime()) : "");
		} else if(OpenIDConstants.ATTRIBUTE_ALIAS_GENDER.equals(alias) && OpenIDConstants.ATTRIBUTE_TYPE_GENDER.equals(type)){
			return (user.getGender() != null ? (user.getGender().isMaleGender() ? "M" : "F") : "");
		}
		return "";	
    }

	private boolean isAuthenticatedAndApproved(IWContext iwc, User user, AuthRequest authReq) {
		
        OpenIDServerBean serverBean = ELUtil.getInstance().getBean("openIDServerBean");
        List<AuthorizedAttribute> required = serverBean.getRequiredAttributes();
        List<AuthorizedAttribute> allAttributes = serverBean.getRequestedAttributes();

		// allowedRealm value set in presentation layer
		boolean allowAction = isAllowAction(iwc);
		
        //Deny by default if no attributes are exchanged
		boolean allowed = allAttributes != null && !allAttributes.isEmpty();
		
		// Check each allowed attribute
		// If it has been stored, it is always allow, otherwise it is not
		if(allowAction){
			allowed = true; //allow by default if allow action
			if(allAttributes != null){
				for(AuthorizedAttribute attr : allAttributes){
					if(!attr.getIsAllowed() && required.contains(attr)){
						//If not allowed but required
						return false;
					}
				}
			}
		} else {
			//denied by default if no attributes are requested, i.e. no always allow option
			//if there are no attributes requested
			if(allAttributes != null){
				for(AuthorizedAttribute attr : allAttributes){
					//Check if not always allowed
					if(attr.isNotYetStored()){
						//Not always-allow and not an allow-action, hence not allowed
						return false;
					}
					if(!attr.getIsAllowed() && required.contains(attr)){
						//If not allowed but required
						return false;
					} else {
						allowed = true;
					}
				}
			}
		}
		
		return allowed;
	}
	
	private boolean isAllowAction(IWContext iwc){
		Object allowValue = iwc.getRequest().getAttribute(OpenIDConstants.PARAMETER_ALLOWED);
		if(allowValue!=null){
			return true;
		} else {
			String paramValue = iwc.getParameter(OpenIDConstants.PARAMETER_ALLOWED);
			String sessionValue = (String)iwc.getSessionAttribute(OpenIDConstants.PARAMETER_ALLOWED);
			iwc.removeSessionAttribute(OpenIDConstants.PARAMETER_ALLOWED);
			if(paramValue!=null && paramValue.equals(sessionValue)){
				iwc.getRequest().setAttribute(OpenIDConstants.PARAMETER_ALLOWED,"true");
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private void storeRequestedAttributesToSession(IWContext iwc, AuthRequest authReq) {
		if (authReq.hasExtension(AxMessage.OPENID_NS_AX)) {
            MessageExtension ext;
			try {
				ext = authReq.getExtension(AxMessage.OPENID_NS_AX);
				if (ext instanceof FetchRequest) {
	                FetchRequest fetchReq = (FetchRequest) ext;
                    OpenIDServerBean serverBean = ELUtil.getInstance().getBean("openIDServerBean");
	                Map<String,String> allAttributes = (Map<String,String>)fetchReq.getAttributes();
                    Map<String,String> required = (Map<String,String>)fetchReq.getAttributes(true);
                    
                    List<AuthorizedAttribute> allAttributesList = new ArrayList<AuthorizedAttribute>();
                    List<AuthorizedAttribute> requiredAttributesList = new ArrayList<AuthorizedAttribute>();
                    List<AuthorizedAttribute> optionalAttributesList = new ArrayList<AuthorizedAttribute>();
                    
                    String realm = serverBean.getRealm();
                    
                    for(String alias : allAttributes.keySet()){
                    	ExchangeAttribute attribute = getDAO().getExchangeAttribute(alias, allAttributes.get(alias));
                    	if(attribute != null){
                    		User user = iwc.getCurrentUser();
                    		AuthorizedAttribute aattr = getDAO().getAuthorizedAttributes(user.getUniqueId(), realm, attribute);
                    		if(aattr == null){
                    			aattr = new AuthorizedAttribute();
                    			aattr.setExchangeAttribute(attribute);
                    			aattr.setRealm(realm);
                    			aattr.setUserUUID(user.getUniqueId());
                    			aattr.setIsAllowed(true);
                    		}
                    		allAttributesList.add(aattr);
                    		if(required.containsKey(alias) && attribute.getType().equals(required.get(alias))){
                    			requiredAttributesList.add(aattr);
                    		} else {
                    			optionalAttributesList.add(aattr);
                    		}
                    	} else {
                    		LOGGER.warning("Requesting unknown exchange attribute: "+alias+ " : " + allAttributes.get(alias));
                    		//throw new UnsupportedOperationException("Requesting unknown exchange attribute.");
                    	}
                    }
                    

                    serverBean.setRequestedAttributes(allAttributesList);
                    serverBean.setRequiredAttributes(requiredAttributesList);
                    serverBean.setOptionalAttributes(optionalAttributesList);
                }
                else /*if (ext instanceof StoreRequest)*/ {
                	//TODO implement?
                    throw new UnsupportedOperationException("TODO");
                }
			} catch (MessageException e) {
				e.printStackTrace();
			}
		}
	}
	
	private UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}

	private OpenIDServerDAO getDAO() {
		if (dao == null) {
			ELUtil.getInstance().autowire(this);
		}
		
		return dao;
	}

	private void directResponse(HttpServletResponse httpResp, String response) throws IOException {
        ServletOutputStream os = httpResp.getOutputStream();
        os.write(response.getBytes());
        os.close();
    }

	private ServerManager getServerManager() {
		ServerManager manager = (ServerManager) IWMainApplication.getDefaultIWApplicationContext().getApplicationAttribute(OpenIDConstants.ATTRIBUTE_SERVER_MANAGER);
		if (manager == null) {
			String endPointUrl = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_END_POINT_URL, "http://localhost:8080/");
			String userSetupUrl = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_USER_SETUP_URL, "http://localhost:8080/pages/profile/?doRedirect=true");

			manager = new ServerManager();
			manager.setSharedAssociations(new InMemoryServerAssociationStore());
			manager.setPrivateAssociations(new InMemoryServerAssociationStore());
//			
//			log("OpenIDServerServlet: Only for testing - Remove code: manager.setExpireIn(...);");
//			manager.setExpireIn(30);
//			
			manager.setOPEndpointUrl(endPointUrl);
			manager.setUserSetupUrl(userSetupUrl);
			IWMainApplication.getDefaultIWApplicationContext().setApplicationAttribute(OpenIDConstants.ATTRIBUTE_SERVER_MANAGER, manager);
		}
		
		return manager;
	}
	
	protected LoginBusinessBean getLoginBusiness(HttpServletRequest request){
		return LoginBusinessBean.getLoginBusinessBean(request);
	}
}