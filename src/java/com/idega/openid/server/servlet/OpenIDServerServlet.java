package com.idega.openid.server.servlet;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.idega.core.contact.data.Email;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.openid.OpenIDConstants;
import com.idega.openid.server.dao.OpenIDServerDAO;
import com.idega.openid.server.data.AuthenticatedRealm;
import com.idega.openid.util.OpenIDUtil;
import com.idega.presentation.IWContext;
import com.idega.user.business.NoEmailFoundException;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.expression.ELUtil;
import com.idega.util.text.TextSoap;

public class OpenIDServerServlet extends HttpServlet {

	private static final long serialVersionUID = -7832846183877408861L;
	
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
        
        Map parameterMap = (Map) req.getSession().getAttribute(OpenIDConstants.ATTRIBUTE_PARAMETER_MAP);
        if (request.getParameters().size() == 0 && parameterMap != null) {
        	request = new ParameterList(parameterMap);
        }

        String mode = request.hasParameter(OpenIDConstants.PARAMETER_OPENID_MODE) ? request.getParameterValue(OpenIDConstants.PARAMETER_OPENID_MODE) : null;
        String realm = request.hasParameter(OpenIDConstants.PARAMETER_REALM) ? request.getParameterValue(OpenIDConstants.PARAMETER_REALM) : null;
        if (realm != null) {
			req.getSession().setAttribute(OpenIDConstants.ATTRIBUTE_RETURN_URL, realm);
			if (realm.indexOf("http://") != -1) {
				realm = TextSoap.findAndCut(realm, "http://");
			}
			if (realm.indexOf("/") != -1) {
				realm = realm.substring(0, realm.indexOf("/"));
			}
			req.getSession().setAttribute(OpenIDConstants.PARAMETER_REALM, realm);
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
	        	if (iwc.isLoggedOn()) {
	        		req.getSession().removeAttribute(OpenIDConstants.ATTRIBUTE_PARAMETER_MAP);
	        		req.getSession().removeAttribute(OpenIDConstants.ATTRIBUTE_SERVER_URL);
	        		req.getSession().removeAttribute(OpenIDConstants.ATTRIBUTE_DO_REDIRECT);
	        		req.getSession().removeAttribute(OpenIDConstants.ATTRIBUTE_SUBDOMAIN);
	        		
		            // interact with the user and obtain data needed to continue
		            List userData = userInteraction(iwc, request);
		
		            String userSelectedClaimedId = (String) userData.get(0);
		            Boolean authenticatedAndApproved = (Boolean) userData.get(1);
		            String email = (String) userData.get(2);
		            String personalID = (String) userData.get(3);
		            String fullname = (String) userData.get(4);
		            String dateOfBirth = (String) userData.get(5);
		            String gender = (String) userData.get(6);
		            
		            // --- process an authentication request ---
		            AuthRequest authReq = AuthRequest.createAuthRequest(request, manager.getRealmVerifier());
		
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
		        		req.getSession().setAttribute(OpenIDConstants.ATTRIBUTE_PARAMETER_MAP, req.getParameterMap());
		        		
		        		String URL = req.getScheme() + "://" + req.getServerName() + (req.getServerPort() != 80 ? ":" + req.getServerPort() : "") + req.getRequestURI() + "?" + req.getQueryString();
		        		req.getSession().setAttribute(OpenIDConstants.ATTRIBUTE_SERVER_URL, URL);

		        		String authenticateURL = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_AUTHENTICATION_URL, "http://www.elykill.is/pages/profile/mypage/authenticate/");
		        		resp.sendRedirect(authenticateURL);
		        		return;
		            }
		            else {
		                if (authReq.hasExtension(AxMessage.OPENID_NS_AX)) {
		                    MessageExtension ext = authReq.getExtension(AxMessage.OPENID_NS_AX);
		                    if (ext instanceof FetchRequest) {
		                        FetchRequest fetchReq = (FetchRequest) ext;
		                        Map required = fetchReq.getAttributes(true);
		                        if (required.containsKey("email")) {
		                            Map userDataExt = new HashMap();
		
		                            FetchResponse fetchResp = FetchResponse.createFetchResponse(fetchReq, userDataExt);
		                            fetchResp.addAttribute("email", "http://schema.openid.net/contact/email", email);
		                            fetchResp.addAttribute("personalID", "http://www.elykill.is/contact/personalID", personalID);
		                            fetchResp.addAttribute("fullname", "http://schema.openid.net/contact/fullname", fullname);
		                            fetchResp.addAttribute("dob", "http://schema.openid.net/contact/dob", dateOfBirth);
		                            fetchResp.addAttribute("gender", "http://schema.openid.net/contact/gender", gender);
		                            
		                            response.addExtension(fetchResp);
		                        }
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
		        		req.getSession().removeAttribute(OpenIDConstants.ATTRIBUTE_SUBDOMAIN);
		        		req.getSession().removeAttribute(OpenIDConstants.ATTRIBUTE_ALLOWED_REALM);
		        		
		        		User user = iwc.getCurrentUser();
		        		getDAO().createLogEntry(user.getUniqueId(), realm);
		        		
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
	        		req.getSession().setAttribute(OpenIDConstants.ATTRIBUTE_PARAMETER_MAP, req.getParameterMap());
	        		
	        		String URL = req.getScheme() + "://" + req.getServerName() + (req.getServerPort() != 80 ? ":" + req.getServerPort() : "") + req.getRequestURI() + "?" + req.getQueryString();
	        		req.getSession().setAttribute(OpenIDConstants.ATTRIBUTE_SERVER_URL, URL);
	        		req.getSession().setAttribute(OpenIDConstants.ATTRIBUTE_DO_REDIRECT, Boolean.TRUE.toString());
	        		req.getSession().setAttribute(OpenIDConstants.ATTRIBUTE_SUBDOMAIN, new OpenIDUtil().getSubDomain(request.getParameterValue("openid.claimed_id")));
	        		
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

	protected List<?> userInteraction(IWContext iwc, ParameterList request) throws ServerException {
		try {
			String realm = (String) iwc.getSessionAttribute(OpenIDConstants.PARAMETER_REALM);
			String allowedRealm = (String) iwc.getSessionAttribute(OpenIDConstants.ATTRIBUTE_ALLOWED_REALM);

			User user = iwc.getCurrentUser();
			Email email = null;
			try {
				email = getUserBusiness(iwc).getUsersMainEmail(user);
			}
			catch (NoEmailFoundException e) { /*No action...*/ }
			
			List<Object> list = new ArrayList<Object>();
			list.add("http://" + getUserBusiness(iwc).getUserLogin(user) + ".elykill.is/pages/profile/mypage/");

			boolean allowed = false;
			AuthenticatedRealm authRealm = getDAO().getAuthenticatedRealm(user.getUniqueId(), realm);
			if (authRealm != null) {
				allowed = true;
			}
			else if (allowedRealm != null && allowedRealm.equals(realm)) {
				allowed = true;
			}
			list.add(new Boolean(allowed));
			
			list.add(email != null ? email.getEmailAddress() : "");
			list.add(user.getPersonalID() != null ? user.getPersonalID() : "");
			list.add(user.getName());
			list.add(user.getDateOfBirth() != null ? Long.toString(user.getDateOfBirth().getTime()) : "");
			list.add(user.getGender() != null ? (user.getGender().isMaleGender() ? "M" : "F") : "");
			
			return list;
		}
		catch (RemoteException re) {
			throw new IBORuntimeException(re);
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
			manager.setOPEndpointUrl(endPointUrl);
			manager.setUserSetupUrl(userSetupUrl);
			IWMainApplication.getDefaultIWApplicationContext().setApplicationAttribute(OpenIDConstants.ATTRIBUTE_SERVER_MANAGER, manager);
		}
		
		return manager;
	}
}