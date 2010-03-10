package com.idega.openid.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openid4java.message.AuthSuccess;
import org.openid4java.message.DirectError;
import org.openid4java.message.Message;
import org.openid4java.message.ParameterList;
import org.openid4java.server.InMemoryServerAssociationStore;
import org.openid4java.server.ServerManager;

import com.idega.idegaweb.IWMainApplication;
import com.idega.openid.OpenIDConstants;

public class OpenIDServerServlet2 extends HttpServlet {

	private static final long serialVersionUID = 8194988842332608019L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession();

		ServerManager manager = getServerManager();
		
		ParameterList requestp;

		if ("complete".equals(request.getParameter("_action"))) // Completing the authz and authn process by redirecting here
		{
			requestp = (ParameterList) session.getAttribute("parameterlist"); // On a redirect from the OP authn & authz sequence
		} else {
			requestp = new ParameterList(request.getParameterMap());
		}

		String mode = requestp.hasParameter("openid.mode") ? requestp.getParameterValue("openid.mode") : null;

		Message responsem;
		String responseText;

		if ("associate".equals(mode)) {
			// --- process an association request ---
			responsem = manager.associationResponse(requestp);
			responseText = responsem.keyValueFormEncoding();
		} else if ("checkid_setup".equals(mode) || "checkid_immediate".equals(mode)) {
			// interact with the user and obtain data needed to continue
			// List userData = userInteraction(requestp);
			String userSelectedId = null;
			String userSelectedClaimedId = null;
			Boolean authenticatedAndApproved = Boolean.FALSE;

			if ((session.getAttribute("authenticatedAndApproved") == null) || (((Boolean) session.getAttribute("authenticatedAndApproved")) == Boolean.FALSE)) {
				session.setAttribute("parameterlist", requestp);
				response.sendRedirect("provider_authorization.jsp");
			} else {
				userSelectedId = (String) session.getAttribute("openid.claimed_id");
				userSelectedClaimedId = (String) session.getAttribute("openid.identity");
				authenticatedAndApproved = (Boolean) session.getAttribute("authenticatedAndApproved");
				// Remove the parameterlist so this provider can accept requests from elsewhere
				session.removeAttribute("parameterlist");
				session.setAttribute("authenticatedAndApproved", Boolean.FALSE); // Makes you authorize each and every time
			}

			// --- process an authentication request ---
			responsem = manager.authResponse(requestp, userSelectedId, userSelectedClaimedId, authenticatedAndApproved.booleanValue());

			// caller will need to decide which of the following to use:
			// - GET HTTP-redirect to the return_to URL
			// - HTML FORM Redirection
			// responseText = response.wwwFormEncoding();
			if (responsem instanceof AuthSuccess) {
				response.sendRedirect(((AuthSuccess) responsem).getDestinationUrl(true));
				return;
			} else {
				responseText = "<pre>" + responsem.keyValueFormEncoding() + "</pre>";
			}
		} else if ("check_authentication".equals(mode)) {
			// --- processing a verification request ---
			responsem = manager.verify(requestp);
			responseText = responsem.keyValueFormEncoding();
		} else {
			// --- error response ---
			responsem = DirectError.createDirectError("Unknown request");
			responseText = responsem.keyValueFormEncoding();
		}
		
		response.getWriter().print(responseText);
	}
	
	private ServerManager getServerManager() {
		ServerManager manager = (ServerManager) IWMainApplication.getDefaultIWApplicationContext().getApplicationAttribute(OpenIDConstants.ATTRIBUTE_SERVER_MANAGER);
		if (manager == null) {
			String endPointUrl = IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_END_POINT_URL, "http://localhost:8080/");

			manager = new ServerManager();
			manager.setSharedAssociations(new InMemoryServerAssociationStore());
			manager.setPrivateAssociations(new InMemoryServerAssociationStore());
			manager.setOPEndpointUrl(endPointUrl);
			IWMainApplication.getDefaultIWApplicationContext().setApplicationAttribute(OpenIDConstants.ATTRIBUTE_SERVER_MANAGER, manager);
		}
		
		return manager;
	}
}