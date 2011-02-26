package com.idega.openid.client.filter;

import java.io.IOException;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.InMemoryConsumerAssociationStore;
import org.openid4java.consumer.InMemoryNonceVerifier;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.discovery.yadis.YadisResolver;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.message.sreg.SRegMessage;
import org.openid4java.message.sreg.SRegResponse;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.accesscontrol.business.AuthenticationBusiness;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.core.accesscontrol.business.LoginState;
import com.idega.core.accesscontrol.business.ServletFilterChainInterruptException;
import com.idega.core.accesscontrol.jaas.IWCallbackHandler;
import com.idega.core.accesscontrol.jaas.IWJAASAuthenticationRequestWrapper;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.openid.OpenIDConstants;
import com.idega.presentation.IWContext;
import com.idega.repository.data.ImplementorRepository;
import com.idega.servlet.filter.BaseFilter;
import com.idega.servlet.filter.IWAuthenticator;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.GroupBMPBean;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;
import com.idega.util.RequestUtil;
import com.idega.util.StringHandler;
import com.idega.util.text.Name;
import com.idega.util.text.SocialSecurityNumber;

public class OpenIDClientResponseFilter extends BaseFilter {
	
	private static Logger LOGGER = Logger.getLogger(OpenIDClientResponseFilter.class.getName());

	private ConsumerManager manager;

	public void destroy() {
		//No action...
	}

	public void doFilter(ServletRequest srequest, ServletResponse sresponse, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) srequest;
		HttpServletResponse response = (HttpServletResponse) sresponse;
		HttpSession session = request.getSession();

		boolean loginFailed = false;
		
		String isReturn = request.getParameter(OpenIDConstants.PARAMETER_RETURN);
		if (isReturn != null) {
			LoginBusinessBean bean = LoginBusinessBean.getLoginBusinessBean(request);

			if (isReturn.equals(Boolean.TRUE.toString())) {
				Identifier identifier = verifyResponse(request);
				if (identifier == null) {
					loginFailed = true;
				}
				else {
					String personalID = (String) request.getAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_PERSONAL_ID);
					String login = getLoginName(identifier);
					String loginType = OpenIDConstants.LOGIN_TYPE;
					
					try {
						loginFailed = !bean.logInByPersonalID(request, personalID, login, null, loginType);
					}
					catch (Exception e) {
						e.printStackTrace();
						loginFailed = true;
					}
					
					boolean updateUserInfo = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getBoolean(OpenIDConstants.PROPERTY_OPENID_CLIENT_UPDATE_USER_INFO, true);
					
					if(loginFailed){ 
						//try again if autoCreateUser is true
						boolean autoCreateUser = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getBoolean(OpenIDConstants.PROPERTY_OPENID_AUTO_CREATE_USERS, true);
						if(autoCreateUser){
							IWApplicationContext iwac = IWMainApplication.getIWApplicationContext(request);
							UserBusiness usrBusiness = getUserBusiness(iwac);
							User usr = null;
							try {
								usr = usrBusiness.getUser(personalID);
								if(usr != null){
									//user exists, try to create login
									autoCreateLoginForAuthenticatedUser(usr, login, iwac, request);
								}
							} catch (Exception e1) {
								e1.printStackTrace();
							}
							if(usr == null){
								//Create user since not existing, and then login
								try{
									usr = autoCreateAuthenticatedUser(iwac, request);
									autoCreateLoginForAuthenticatedUser(usr, login, iwac,request);
								} catch (Exception ex){
									ex.printStackTrace();
									loginFailed = true;
								}
							}
							
							if(usr != null){ //try logging in again
								if(usr.getPersonalID().equals(personalID)){ //extra check
									try {
										loginFailed = !bean.logInByPersonalID(request, personalID, login, null, loginType);
									} catch (Exception e1) {
										e1.printStackTrace();
									}
								}
							}
						}
					} else if(updateUserInfo){
						//TODO Check if user info has changed and if so then update
					}
				}
			}
			if (loginFailed) {
				bean.internalSetState(request, LoginState.Failed);
			}
		}
		
		if (!loginFailed) {
			chain.doFilter(request, response);
		}
		else {
			LoginBusinessBean loginBusiness = getLoginBusiness(request);

			if (loginBusiness.isLoggedOn(request)) {
				processJAASLogin(request);
				
				boolean didInterrupt = processAuthenticationListeners(request, response, session, loginBusiness);
				if (didInterrupt) {
					return;
				}
			}			
			chain.doFilter(new IWJAASAuthenticationRequestWrapper(request), response);
		}
	}
	
	private String getLoginName(Identifier identifier) {
		String login = identifier.getIdentifier();
		if(login != null && login.contains("/")){
			int fromIndex = 0;
			if(login.startsWith("http://")){
				fromIndex = 7;
			} else if (login.startsWith("https://")){
				fromIndex = 8;
			}
			int toIndex = login.indexOf("/", fromIndex);
			if(toIndex < 0){
				toIndex = login.length();
			}
			if(toIndex > 31){
				LOGGER.warning("Openid identifier \""+login.substring(fromIndex,toIndex)+"\" is too long to fit login name column.  Cutting string after index 31.");
				toIndex = 31;
			}
			login = login.substring(fromIndex,toIndex);
		}
		return login;
	}

	private void autoCreateLoginForAuthenticatedUser(User usr, String login, IWApplicationContext iwac, HttpServletRequest request) throws Exception {
		//Password cannot be null or empty string
		String password = StringHandler.getRandomString(16);
		
		Boolean accountEnabled = Boolean.TRUE;
		int daysOfValidity = 10000;
		Boolean passwordExpires = Boolean.FALSE;
		Boolean userAllowedToChangePassw = Boolean.TRUE;
		Boolean changeNextTime = Boolean.FALSE;
		String encryptionType = null;
		String loginType = OpenIDConstants.LOGIN_TYPE;
		IWTimestamp modified = IWTimestamp.RightNow();

		LoginDBHandler.createLogin(usr, login, password, accountEnabled, modified, daysOfValidity, passwordExpires, userAllowedToChangePassw, changeNextTime, encryptionType, loginType);
		
	}

	private User autoCreateAuthenticatedUser(IWApplicationContext iwac, HttpServletRequest request) throws RemoteException, CreateException {
		String email = (String) request.getAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_EMAIL);
		String fullName = (String) request.getAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_FULL_NAME);
		String dob = (String) request.getAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_DATE_OF_BIRTH);
		String gender = (String) request.getAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_GENDER);
		String personalID = (String) request.getAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_PERSONAL_ID);

		
		IWContext iwc = IWContext.getCurrentInstance();
		String ssn = personalID;
		IWTimestamp dateOfBirth = ssn != null ? new IWTimestamp(SocialSecurityNumber.getDateFromSocialSecurityNumber(ssn)) : null;
		
		Name name = new Name(fullName);
		String displayName = fullName;
		String description = null;
		
		Integer primaryGroup =  new Integer(GroupBMPBean.GROUP_ID_USERS);
		Integer genderID = null;

		User usr = getUserBusiness(iwc).createUser(
				name.getFirstName(), 
				name.getMiddleName(), 
				name.getLastName(), 
				displayName, 
				ssn, 
				description, 
				genderID, 
				dateOfBirth, 
				primaryGroup, 
				fullName);
		
//		User usr = getUserBusiness(iwc).createUserWithLogin(
//				name.getFirstName(), 
//				name.getMiddleName(), 
//				name.getLastName(), 
//				ssn, 
//				displayName, 
//				description, 
//				genderInt, 
//				dateOfBirth, 
//				null, 
//				login, 
//				password, 
//				accountEnabled, 
//				IWTimestamp.RightNow(), 
//				daysOfValidity, 
//				passwordExpires, 
//				userAllowedToChangePassw, 
//				changeNextTime, 
//				encryptionType, 
//				fullName);
		
		return usr;
	}

	protected boolean processAuthenticationListeners(HttpServletRequest request, HttpServletResponse response, HttpSession session, LoginBusinessBean loginBusiness) throws RemoteException {
		try {
			AuthenticationBusiness authenticationBusiness = getAuthenticationBusiness(request);
			User currentUser = loginBusiness.getCurrentUser(session);
			IWContext iwc = getIWContext(request, response);
			authenticationBusiness.callOnLogonMethodInAllAuthenticationListeners(iwc, currentUser);
		}
		catch (ServletFilterChainInterruptException e) {
			//this is normal behaviour if e.g. the listener issues a response.sendRedirect(...)
			System.out.println("[IWAuthenticator] - Filter chain interrupted. The reason was: " + e.getMessage());
			return true;
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	protected void processJAASLogin(HttpServletRequest request) {
		List loginModules = ImplementorRepository.getInstance().newInstances(LoginModule.class, this.getClass());
		// just a shortcut 
		if (loginModules.isEmpty()) {
			return;
		}
		CallbackHandler callbackHandler = new IWCallbackHandler(request);
		Map sharedState = new HashMap(3);
		HttpSession session = request.getSession();
		sharedState.put(IWAuthenticator.REQUEST_KEY, request);
		sharedState.put(IWAuthenticator.SESSION_KEY, session);
		Iterator iteratorFirst = loginModules.iterator();
		while (iteratorFirst.hasNext()) {
			LoginModule loginModule = (LoginModule) iteratorFirst.next();
			try {
				loginModule.initialize(null, callbackHandler, sharedState, null);
				loginModule.login();
			}
			catch (LoginException e) {
				e.printStackTrace();
			}
		}
		Iterator iteratorSecond = loginModules.iterator();
		while (iteratorSecond.hasNext()) {
			LoginModule loginModule = (LoginModule) iteratorSecond.next();
			try {
				loginModule.commit();
			}
			catch (LoginException e) {
				e.printStackTrace();
			}
		}
	}

	protected AuthenticationBusiness getAuthenticationBusiness(HttpServletRequest request) {
		AuthenticationBusiness authenticationBusiness = null;
		try {
			IWApplicationContext iwac = getIWMainApplication(request).getIWApplicationContext();
			authenticationBusiness = (AuthenticationBusiness) IBOLookup.getServiceInstance(iwac, AuthenticationBusiness.class);
		}
		catch (IBOLookupException e) {
			e.printStackTrace();
		}
		return authenticationBusiness;
	}

	public Identifier verifyResponse(HttpServletRequest request) {
		try {
			// extract the parameters from the authentication response
			// (which comes in as a HTTP request from the OpenID provider)
			ParameterList response = new ParameterList(request
					.getParameterMap());

			// retrieve the previously stored discovery information
			DiscoveryInformation discovered = (DiscoveryInformation) request
					.getSession().getAttribute("openid-disc");

			// extract the receiving URL from the HTTP request
			StringBuffer receivingURL = request.getRequestURL();
			String queryString = request.getQueryString();
			if (queryString != null && queryString.length() > 0)
				receivingURL.append("?").append(request.getQueryString());

			// verify the response; ConsumerManager needs to be the same
			// (static) instance used to place the authentication request
			VerificationResult verification = this.manager.verify(receivingURL
					.toString(), response, discovered);

			// examine the verification result and extract the verified
			// identifier
			Identifier verified = verification.getVerifiedId();
			if (verified != null) {
				AuthSuccess authSuccess = (AuthSuccess) verification
						.getAuthResponse();

				if (authSuccess.hasExtension(SRegMessage.OPENID_NS_SREG)) {
					MessageExtension ext = authSuccess
							.getExtension(SRegMessage.OPENID_NS_SREG);
					if (ext instanceof SRegResponse) {
						SRegResponse sregResp = (SRegResponse) ext;
						for (Iterator<?> iter = sregResp.getAttributeNames()
								.iterator(); iter.hasNext();) {
							String name = (String) iter.next();
							String value = sregResp.getParameterValue(name);
							request.setAttribute(name, value);
						}
					}
				}
				if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
					FetchResponse fetchResp = (FetchResponse) authSuccess
							.getExtension(AxMessage.OPENID_NS_AX);

					// List emails = fetchResp.getAttributeValues("email");
					// String email = (String) emails.get(0);
					
					List<?> aliases = fetchResp.getAttributeAliases();
					for (Iterator<?> iter = aliases.iterator(); iter.hasNext();) {
						String alias = (String) iter.next();
						List<?> values = fetchResp.getAttributeValues(alias);
						if (values.size() > 0) {
							request.setAttribute(alias, values.get(0));
						}
					}
				}

				return verified; // success
			}
		} catch (OpenIDException e) {
			// present error to the user
		}

		return null;
	}

	public void init(FilterConfig config) throws ServletException {
		this.manager = (ConsumerManager) IWMainApplication.getDefaultIWApplicationContext().getApplicationAttribute(OpenIDConstants.ATTRIBUTE_CONSUMER_MANAGER);
		if (this.manager == null) {
			initializeConsumerManager();
		}
	}

	private void initializeConsumerManager() throws ServletException {
		try {
			this.manager = new ConsumerManager();
			this.manager.setAssociations(new InMemoryConsumerAssociationStore());
			String timout = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_CONSUMER_MANAGER_TIMEOUT, "5000");
			int t = 5000; //in seconds
			try {
				t=Integer.parseInt(timout);
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.manager.setNonceVerifier(new InMemoryNonceVerifier(t));
			
			String ConnectionTimout = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_CONSUMER_MANAGER_CONNECTION_TIMEOUT, "10000");
			int ct = 10000; //in milliseconds
			try {
				ct=Integer.parseInt(ConnectionTimout);
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.manager.setConnectTimeout(ct);
			
			String SocketTimout = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_CONSUMER_MANAGER_SOCKET_TIMEOUT, "10000");
			int st = 10000; //in milliseconds
			try {
				st=Integer.parseInt(SocketTimout);
			} catch (Exception e) {
				e.printStackTrace();
			}
			this.manager.setSocketTimeout(st);
			
			String maxRedirects = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_CONSUMER_MANAGER_MAX_REDIRECTS, "10");
			int mrd = 10;
			try {
				mrd=Integer.parseInt(maxRedirects);
			} catch (Exception e) {
				e.printStackTrace();
			}

			YadisResolver resolver = new YadisResolver();
			resolver.setMaxRedirects(mrd);
			this.manager.getDiscovery().setYadisResolver(resolver);
			
			IWMainApplication.getDefaultIWApplicationContext().setApplicationAttribute(OpenIDConstants.ATTRIBUTE_CONSUMER_MANAGER, this.manager);	
		}
		catch (ConsumerException e) {
			throw new ServletException(e);
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
	
}