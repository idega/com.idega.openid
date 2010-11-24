package com.idega.openid.util;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.sreg.SRegMessage;

/**
 * Use this class to build a XRDS document in an object oriented way. Instantiate the class, add one or more service elements, then call toXmlString() to get a String representation of the document (for sending back to the caller).
 * 
 * @author J Steven Perry
 * @author Makoto Consulting Group, Inc.
 * @author Oscar Pearce - many thanks to Oscar for the guts of the code you see below to build the proper XRDS response document.
 * 
 */
public class XrdsDocumentBuilder {

	private Element baseElement;

	private static final String MASTER_XRDS_NAMESPACE = "xri://$xrd*($v*2.0)";
	
	private Namespace addedNamespace;

	public XrdsDocumentBuilder() {
		this(null);
	}
	
	public XrdsDocumentBuilder(String version) {
		Namespace xrdNS = Namespace.getNamespace(MASTER_XRDS_NAMESPACE);
		baseElement = new Element("XRD", xrdNS);
		
		if (version != null) {
			baseElement.setAttribute("version", version);
		}
	}

	public void addServiceElement(String uri, String priority, String uriPriority, String delegate, String localID, String... types) {
		Namespace xrdNS = Namespace.getNamespace(MASTER_XRDS_NAMESPACE);
		Namespace openidNS = Namespace.getNamespace("openid", "http://openid.net/xmlns/1.0");
		addedNamespace = openidNS;
		
		Element serviceElement = new Element("Service", xrdNS);
		Element uriElement = new Element("URI", xrdNS);
		if (uriPriority != null) {
			uriElement.setAttribute("priority", uriPriority);
		}
		uriElement.addContent(uri);
		for (String type : types) {
			Element typeElement = new Element("Type", xrdNS);
			typeElement.addContent(type);
			serviceElement.addContent(typeElement);
		}
		serviceElement.addContent(uriElement);
		if (StringUtils.isNotEmpty(delegate)) {
			Element delegateElement = new Element("Delegate", openidNS);
			delegateElement.addContent(delegate);
			serviceElement.addContent(delegateElement);
		}
		if (StringUtils.isNotEmpty(localID)) {
			Element localIDElement = new Element("LocalID", xrdNS);
			localIDElement.addContent(localID);
			serviceElement.addContent(localIDElement);
		}
		if (StringUtils.isNotEmpty(priority)) {
			serviceElement.setAttribute("priority", priority);
		}
		baseElement.addContent(serviceElement);
	}
	
	public void addFriendlyName(String name) {
		Namespace xrdNS = Namespace.getNamespace(MASTER_XRDS_NAMESPACE);
		Namespace ux = Namespace.getNamespace("ux", "http://specs.openid.net/extensions/ux/1.0");
		addedNamespace = ux;
		
		Element serviceElement = new Element("Service", xrdNS);
		baseElement.addContent(serviceElement);

		Element typeElement = new Element("Type", xrdNS);
		typeElement.addContent("http://specs.openid.net/extensions/ux/1.0/friendlyname");
		serviceElement.addContent(typeElement);
		
		Element nameElement = new Element("friendlyname", ux);
		nameElement.addContent(name);
		serviceElement.addContent(nameElement);
	}
	
	public void addImage(String url, String width, String height) {
		Namespace xrdNS = Namespace.getNamespace(MASTER_XRDS_NAMESPACE);
		Namespace ux = Namespace.getNamespace("ux", "http://specs.openid.net/extensions/ux/1.0");
		addedNamespace = ux;
		
		Element serviceElement = new Element("Service", xrdNS);
		baseElement.addContent(serviceElement);

		Element typeElement = new Element("Type", xrdNS);
		typeElement.addContent("http://specs.openid.net/extensions/ux/1.0/friendlyname");
		serviceElement.addContent(typeElement);
		
		Element imageElement = new Element("img", ux);
		imageElement.setAttribute("url", url);
		imageElement.setAttribute("width", width);
		imageElement.setAttribute("height", height);
		serviceElement.addContent(imageElement);
	}

	public String toXmlString() {
		Namespace xrdsNS = Namespace.getNamespace("xrds", "xri://$xrds");
		Element rootElement = new Element("XRDS", xrdsNS);
		if (addedNamespace != null) {
			rootElement.addNamespaceDeclaration(addedNamespace);
		}
		rootElement.addNamespaceDeclaration(Namespace.getNamespace(MASTER_XRDS_NAMESPACE));
		rootElement.addContent(baseElement);
		Document doc = new Document(rootElement);
		StringWriter w = new StringWriter();
		XMLOutputter o = new XMLOutputter(Format.getPrettyFormat());
		try {
			o.output(doc, w);
			w.close();
		}
		catch (IOException e) {
		}
		return w.toString();
	}

	/**
	 * Test method
	 */
	public static void main(String[] arguments) {
		XrdsDocumentBuilder documentBuilder = new XrdsDocumentBuilder("2.0");
		documentBuilder.addServiceElement("http://www.elykill.is/openid-server", "0", null, null, "http://laddi.elykill.is", "http://specs.openid.net/auth/2.0/signon", SRegMessage.OPENID_NS_SREG, SRegMessage.OPENID_NS_SREG11, "http://schemas.openid.net/pape/policies/2007/06/phishing-resistant", AxMessage.OPENID_NS_AX);
		documentBuilder.addServiceElement("http://www.elykill.is/openid-server", "1", null, "http://laddi.elykill.is", null, "http://openid.net/signon/1.1", SRegMessage.OPENID_NS_SREG, SRegMessage.OPENID_NS_SREG11, "http://schemas.openid.net/pape/policies/2007/06/phishing-resistant", AxMessage.OPENID_NS_AX);
		documentBuilder.addServiceElement("http://www.elykill.is/openid-server", "2", null, "http://laddi.elykill.is", null, "http://openid.net/signon/1.0", SRegMessage.OPENID_NS_SREG, SRegMessage.OPENID_NS_SREG11, "http://schemas.openid.net/pape/policies/2007/06/phishing-resistant", AxMessage.OPENID_NS_AX);
		
		System.out.println(documentBuilder.toXmlString());

		documentBuilder = new XrdsDocumentBuilder();
		documentBuilder.addServiceElement("http://www.elykill.is/openid-server", "0", "0", null, null, "http://specs.openid.net/auth/2.0/server", SRegMessage.OPENID_NS_SREG, AxMessage.OPENID_NS_AX);
		documentBuilder.addFriendlyName("eLykill");
		documentBuilder.addImage("http://www.elykill.is/content/files/public/elykill.png", "60", "68");
		
		System.out.println(documentBuilder.toXmlString());
	}
}