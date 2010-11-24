package com.idega.openid.client.filter;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

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
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.yadis.YadisResolver;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.ax.FetchRequest;

import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.accesscontrol.business.LoginState;
import com.idega.idegaweb.IWMainApplication;
import com.idega.openid.OpenIDConstants;
import com.idega.openid.client.bean.OpenIDClientBean;
import com.idega.servlet.filter.BaseFilter;
import com.idega.util.expression.ELUtil;

public class OpenIDClientRequestFilter extends BaseFilter {

	private ConsumerManager manager;

	public void destroy() {
		// No action...
	}

	public void doFilter(ServletRequest srequest, ServletResponse sresponse, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) srequest;
		HttpServletResponse response = (HttpServletResponse) sresponse;
		HttpSession session = request.getSession();

		String identifier = request.getParameter(OpenIDConstants.PARAMETER_IDENTIFIER);
		if (identifier != null) {
			try {
				// configure the return_to URL where your application will receive
				// the authentication responses from the OpenID provider
				// String returnToUrl = "http://example.com/openid";
				String returnToUrl = request.getRequestURL().toString() + "?" + OpenIDConstants.PARAMETER_RETURN + "=" + Boolean.TRUE.toString();

				Object[] arguments = { identifier };
				String providerURL = getIWMainApplication(request).getIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_OPENID_PROVIDER, "{0}");

				// perform discovery on the user-supplied identifier
				List<?> discoveries = this.manager.discover(MessageFormat.format(providerURL, arguments));

				// attempt to associate with the OpenID provider
				// and retrieve one service endpoint for authentication
				DiscoveryInformation discovered = this.manager.associate(discoveries);

				// store the discovery information in the user's session
				session.setAttribute("openid-disc", discovered);

				// obtain a AuthRequest message to be sent to the OpenID provider
				AuthRequest authReq = this.manager.authenticate(discovered, returnToUrl);

				// Attribute Exchange example: fetching the 'email' attribute
				FetchRequest fetch = FetchRequest.createFetchRequest();

				fetch.addAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_EMAIL, OpenIDConstants.ATTRIBUTE_TYPE_EMAIL, true);
				fetch.addAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_FULL_NAME, OpenIDConstants.ATTRIBUTE_TYPE_FULL_NAME, true);
				fetch.addAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_DATE_OF_BIRTH, OpenIDConstants.ATTRIBUTE_TYPE_DATE_OF_BIRTH, true);
				fetch.addAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_GENDER, OpenIDConstants.ATTRIBUTE_TYPE_GENDER, false);
				fetch.addAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_PERSONAL_ID, OpenIDConstants.ATTRIBUTE_TYPE_PERSONAL_ID, true);

				// attach the extension to the authentication request
				if (!fetch.getAttributes().isEmpty()) {
					authReq.addExtension(fetch);
				}

				// Option 1: GET HTTP-redirect to the OpenID Provider endpoint
				// The only method supported in OpenID 1.x
				// redirect-URL usually limited ~2048 bytes
				response.sendRedirect(authReq.getDestinationUrl(true));
			}
			catch (OpenIDException e) {
				LoginBusinessBean bean = new LoginBusinessBean();
				bean.internalSetState(request, LoginState.Failed);
			}
		}

		chain.doFilter(request, response);
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