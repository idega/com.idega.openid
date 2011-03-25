package com.idega.openid.server.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.idega.openid.util.OpenIDUtil;
import com.idega.servlet.filter.util.PagesRedirectHandler;

public class OpenIDPagesRedirectHandler implements PagesRedirectHandler {

	@Override
	public boolean isForwardOnRootURIRequest(HttpServletRequest request, HttpServletResponse response) {
		String serverName = request.getServerName();
		String subDomain = new OpenIDUtil().getSubDomain(serverName);
		
		if (subDomain == null) {
			return true;
		}
		return false;
	}
}