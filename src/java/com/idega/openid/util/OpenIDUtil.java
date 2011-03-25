package com.idega.openid.util;

import com.idega.core.builder.data.ICDomain;
import com.idega.idegaweb.IWMainApplication;

public class OpenIDUtil {

	private static final String WWW = "www";
	private static final String HTTP_PROTOCOL = "http://";
	private static final String HTTPS_PROTOCOL = "https://";

	public String getSubDomain(String serverName) {
		ICDomain domain = IWMainApplication.getDefaultIWApplicationContext().getDomain();
		if (domain.getServerName().equals(serverName)) {
			return null;
		}

		String subdomain = null;
		if (serverName.indexOf(HTTPS_PROTOCOL) != -1) {
			serverName = serverName.substring(serverName.indexOf(HTTPS_PROTOCOL) + 8);
		}
		if (serverName.indexOf(HTTP_PROTOCOL) != -1) {
			serverName = serverName.substring(serverName.indexOf(HTTP_PROTOCOL) + 7);
		}
		if (serverName.indexOf(".") != -1 && serverName.indexOf(".") != serverName.lastIndexOf(".") && serverName.indexOf(WWW) == -1) {
			subdomain = serverName.substring(0, serverName.indexOf("."));
		}
		
		return subdomain;
	}
}