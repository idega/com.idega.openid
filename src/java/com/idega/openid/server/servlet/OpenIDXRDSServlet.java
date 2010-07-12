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

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		XrdsDocumentBuilder documentBuilder = new XrdsDocumentBuilder();
		documentBuilder.addServiceElement("http://specs.openid.net/auth/2.0/server", getOpEndpointUrl(), "10");
		documentBuilder.addServiceElement("http://specs.openid.net/auth/2.0/signon", getOpEndpointUrl(), "20");
		documentBuilder.addServiceElement(AxMessage.OPENID_NS_AX, getOpEndpointUrl(), "30");
		documentBuilder.addServiceElement(SRegMessage.OPENID_NS_SREG, getOpEndpointUrl(), "40");
		
		ServletOutputStream os = resp.getOutputStream();
        os.write(documentBuilder.toXmlString().getBytes());
        os.close();
	}

	public String getOpEndpointUrl() {
		return IWMainApplication.getDefaultIWApplicationContext().getApplicationSettings().getProperty(OpenIDConstants.PROPERTY_END_POINT_URL, "http://www.elykill.is/openid-server");
	}
}