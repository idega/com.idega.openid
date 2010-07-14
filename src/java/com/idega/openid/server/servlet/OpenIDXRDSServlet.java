package com.idega.openid.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.sreg.SRegMessage;

import com.idega.idegaweb.IWMainApplication;
import com.idega.openid.OpenIDConstants;
import com.idega.openid.util.XrdsDocumentBuilder;

public class OpenIDXRDSServlet extends HttpServlet {

	private static final long serialVersionUID = -2869611557380428794L;

	private static final String WWW = "www";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String serverName = request.getServerName();
		String subDomain = getSubDomain(serverName);

		XrdsDocumentBuilder documentBuilder = null;
		if (subDomain == null) {
			documentBuilder = new XrdsDocumentBuilder();
			documentBuilder.addServiceElement(getOpEndpointUrl(), "0", "0", null, null, "http://specs.openid.net/auth/2.0/server", SRegMessage.OPENID_NS_SREG, AxMessage.OPENID_NS_AX);
			documentBuilder.addFriendlyName("eLykill");
			documentBuilder.addImage("http://www.elykill.is/content/files/public/elykill.png", "60", "68");
		}
		else {
			String subdomainURL = request.getScheme() + "://" + request.getServerName() + "/";
			
			documentBuilder = new XrdsDocumentBuilder("2.0");
			documentBuilder.addServiceElement(getOpEndpointUrl(), "0", null, null, subdomainURL, "http://specs.openid.net/auth/2.0/signon", SRegMessage.OPENID_NS_SREG, SRegMessage.OPENID_NS_SREG11, "http://schemas.openid.net/pape/policies/2007/06/phishing-resistant", AxMessage.OPENID_NS_AX);
			documentBuilder.addServiceElement(getOpEndpointUrl(), "1", null, subdomainURL, null, "http://openid.net/signon/1.1", SRegMessage.OPENID_NS_SREG, SRegMessage.OPENID_NS_SREG11, "http://schemas.openid.net/pape/policies/2007/06/phishing-resistant", AxMessage.OPENID_NS_AX);
			documentBuilder.addServiceElement(getOpEndpointUrl(), "2", null, subdomainURL, null, "http://openid.net/signon/1.0", SRegMessage.OPENID_NS_SREG, SRegMessage.OPENID_NS_SREG11, "http://schemas.openid.net/pape/policies/2007/06/phishing-resistant", AxMessage.OPENID_NS_AX);
		}
		
		ServletOutputStream os = response.getOutputStream();
        os.write(documentBuilder.toXmlString().getBytes());
        os.close();
	}

	private String getSubDomain(String serverName) {
		String subdomain = null;
		if (serverName.indexOf(".") != -1 && serverName.indexOf(".") != serverName.lastIndexOf(".") && serverName.indexOf(WWW) == -1) {
			subdomain = serverName.substring(0, serverName.indexOf("."));
		}
		
		return subdomain;
	}
	
	public String getOpEndpointUrl() {
		return IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_END_POINT_URL, "http://www.elykill.is/openid-server");
	}
}