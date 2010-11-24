package com.idega.openid.client.filter;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import com.idega.core.accesscontrol.business.AuthenticationBusiness;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
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
import com.idega.user.data.User;

public class OpenIDClientResponseFilter extends BaseFilter {

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
			LoginBusinessBean bean = new LoginBusinessBean();

			if (isReturn.equals(Boolean.TRUE.toString())) {
				Identifier identifier = verifyResponse(request);
				if (identifier == null) {
					loginFailed = true;
				}
				else {
					String personalID = (String) request.getAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_PERSONAL_ID);
					
					try {
						loginFailed = !bean.logInByPersonalID(request, personalID);
					}
					catch (Exception e) {
						e.printStackTrace();
						loginFailed = true;
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
	
}