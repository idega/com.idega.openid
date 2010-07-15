package com.idega.openid.util;

public class OpenIDUtil {

	private static final String WWW = "www";
	private static final String HTTP_PROTOCOL = "http://";
	private static final String HTTPS_PROTOCOL = "https://";

	public String getSubDomain(String serverName) {
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
	
	public static void main(String[] args) {
		OpenIDUtil util = new OpenIDUtil();
		System.out.println(util.getSubDomain("https://laddi.local.is:8080/pages/profile/mypage/"));
	}
}
