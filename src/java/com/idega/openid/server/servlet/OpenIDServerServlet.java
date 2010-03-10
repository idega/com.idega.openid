package com.idega.openid.server.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openid4java.association.AssociationException;
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
import org.openid4java.message.sreg.SRegMessage;
import org.openid4java.message.sreg.SRegRequest;
import org.openid4java.message.sreg.SRegResponse;
import org.openid4java.server.InMemoryServerAssociationStore;
import org.openid4java.server.ServerException;
import org.openid4java.server.ServerManager;

import com.idega.idegaweb.IWMainApplication;
import com.idega.openid.OpenIDConstants;

public class OpenIDServerServlet extends HttpServlet {

	private static final long serialVersionUID = -7832846183877408861L;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpServletRequest httpReq = (HttpServletRequest) req;
		HttpServletResponse httpResp = (HttpServletResponse) resp;

		ServerManager manager = getServerManager();
		
		// extract the parameters from the request
        ParameterList request = new ParameterList(httpReq.getParameterMap());

        String mode = request.hasParameter("openid.mode") ? request.getParameterValue("openid.mode") : null;

        Message response;
        String responseText = null;

        try {
	        if ("associate".equals(mode)) {
	            // --- process an association request ---
	            response = manager.associationResponse(request);
	            responseText = response.keyValueFormEncoding();
	        }
	        else if ("checkid_setup".equals(mode) || "checkid_immediate".equals(mode)) {
	            // interact with the user and obtain data needed to continue
	            List userData = userInteraction(request);
	
	            String userSelectedClaimedId = (String) userData.get(0);
	            Boolean authenticatedAndApproved = (Boolean) userData.get(1);
	            String email = (String) userData.get(2);
	
	            // --- process an authentication request ---
	            AuthRequest authReq = AuthRequest.createAuthRequest(request, manager.getRealmVerifier());
	
	            String opLocalId = null;
	            // if the user chose a different claimed_id than the one in request
	            if (userSelectedClaimedId != null && userSelectedClaimedId.equals(authReq.getClaimed())) {
	                //opLocalId = lookupLocalId(userSelectedClaimedId);
	            }
	
	            response = manager.authResponse(request,
	                    opLocalId,
	                    userSelectedClaimedId,
	                    authenticatedAndApproved.booleanValue(),
	                    false); // Sign after we added extensions.
	
	            if (response instanceof DirectError) {
	            	directResponse(httpResp, response.keyValueFormEncoding());
	            }
	            else {
	                if (authReq.hasExtension(AxMessage.OPENID_NS_AX)) {
	                    MessageExtension ext = authReq.getExtension(AxMessage.OPENID_NS_AX);
	                    if (ext instanceof FetchRequest) {
	                        FetchRequest fetchReq = (FetchRequest) ext;
	                        Map required = fetchReq.getAttributes(true);
	                        //Map optional = fetchReq.getAttributes(false);
	                        if (required.containsKey("email")) {
	                            Map userDataExt = new HashMap();
	                            //userDataExt.put("email", userData.get(3));
	
	                            FetchResponse fetchResp = FetchResponse.createFetchResponse(fetchReq, userDataExt);
	                            // (alternatively) manually add attribute values
	                            fetchResp.addAttribute("email", "http://schema.openid.net/contact/email", email);
	                            response.addExtension(fetchResp);
	                        }
	                    }
	                    else /*if (ext instanceof StoreRequest)*/ {
	                        throw new UnsupportedOperationException("TODO");
	                    }
	                }
	                if (authReq.hasExtension(SRegMessage.OPENID_NS_SREG)) {
	                    MessageExtension ext = authReq.getExtension(SRegMessage.OPENID_NS_SREG);
	                    if (ext instanceof SRegRequest) {
	                        SRegRequest sregReq = (SRegRequest) ext;
	                        List required = sregReq.getAttributes(true);
	                        //List optional = sregReq.getAttributes(false);
	                        if (required.contains("email")) {
	                            // data released by the user
	                            Map userDataSReg = new HashMap();
	                            //userData.put("email", "user@example.com");
	
	                            SRegResponse sregResp = SRegResponse.createSRegResponse(sregReq, userDataSReg);
	                            // (alternatively) manually add attribute values
	                            sregResp.addAttribute("email", email);
	                            response.addExtension(sregResp);
	                        }
	                    }
	                    else {
	                        throw new UnsupportedOperationException("TODO");
	                    }
	                }
	
	                // Sign the auth success message.
	                // This is required as AuthSuccess.buildSignedList has a `todo' tag now.
	                manager.sign((AuthSuccess) response);
	
	                // caller will need to decide which of the following to use:
	
	                // option1: GET HTTP-redirect to the return_to URL
	                httpResp.sendRedirect(response.getDestinationUrl(true));
	
	                // option2: HTML FORM Redirection
	                //RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("formredirection.jsp");
	                //httpReq.setAttribute("prameterMap", response.getParameterMap());
	                //httpReq.setAttribute("destinationUrl", response.getDestinationUrl(false));
	                //dispatcher.forward(request, response);
	                //return null;
	            }
	        }
	        else if ("check_authentication".equals(mode)) {
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
        }
        catch (AssociationException ae) {
        	ae.printStackTrace();
        }
        catch (ServerException se) {
			se.printStackTrace();
		}
        
        // return the result to the user
        directResponse(httpResp, responseText);
    }

    protected List<?> userInteraction(ParameterList request) throws ServerException {
        throw new ServerException("User-interaction not implemented.");
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

			manager = new ServerManager();
			manager.setSharedAssociations(new InMemoryServerAssociationStore());
			manager.setPrivateAssociations(new InMemoryServerAssociationStore());
			manager.setOPEndpointUrl(endPointUrl);
			IWMainApplication.getDefaultIWApplicationContext().setApplicationAttribute(OpenIDConstants.ATTRIBUTE_SERVER_MANAGER, manager);
		}
		
		return manager;
	}
}