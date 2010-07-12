package com.idega.openid.util;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

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

	public XrdsDocumentBuilder() {
		Namespace xrdNS = Namespace.getNamespace(MASTER_XRDS_NAMESPACE);
		baseElement = new Element("XRD", xrdNS);
	}

	public void addServiceElement(String type, String uri, String priority) {
		addServiceElement(type, uri, priority, null);
	}

	private void addServiceElement(String type, String uri, String priority, String delegate) {
		Namespace xrdNS = Namespace.getNamespace(MASTER_XRDS_NAMESPACE);
		Namespace openidNS = Namespace.getNamespace("openid", "http://openid.net/xmlns/1.0");
		Element serviceElement = new Element("Service", xrdNS);
		Element typeElement = new Element("Type", xrdNS);
		typeElement.addContent(type);
		Element uriElement = new Element("URI", xrdNS);
		uriElement.addContent(uri);
		serviceElement.addContent(typeElement);
		serviceElement.addContent(uriElement);
		if (StringUtils.isNotEmpty(delegate)) {
			Element delegateElement = new Element("Delegate", openidNS);
			delegateElement.addContent(delegate);
			serviceElement.addContent(delegateElement);
		}
		if (StringUtils.isNotEmpty(priority)) {
			serviceElement.setAttribute("priority", priority);
		}
		baseElement.addContent(serviceElement);
	}

	public String toXmlString() {
		Namespace xrdsNS = Namespace.getNamespace("xrds", "xri://$xrds");
		Element rootElement = new Element("XRDS", xrdsNS);
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
}