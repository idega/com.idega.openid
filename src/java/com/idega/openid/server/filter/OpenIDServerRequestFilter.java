package com.idega.openid.server.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.idega.openid.OpenIDConstants;
import com.idega.servlet.filter.BaseFilter;

public class OpenIDServerRequestFilter extends BaseFilter {

	private static final String WWW = "www";
	
	@Override
	public void destroy() {

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain arg2) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		
		String serverName = request.getServerName();
		String subDomain = getSubDomain(serverName);
		if (serverName.indexOf(".") != serverName.lastIndexOf(".") && !subDomain.equals(WWW)) {
			System.out.println("Found subdomain: " + subDomain);
			httpRequest.getSession().setAttribute(OpenIDConstants.ATTRIBUTE_SUBDOMAIN, subDomain);
		}
		
		arg2.doFilter(request, response);
	}

	private String getSubDomain(String serverName) {
		if (serverName.indexOf(".") != -1) {
			return serverName.substring(0, serverName.indexOf("."));
		}
		
		return null;
	}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

}