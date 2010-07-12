package com.idega.openid.server.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.idega.servlet.filter.BaseFilter;

public class OpenIDServerResponseFilter extends BaseFilter {

	public void destroy() {
		//No action...
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		
		httpResponse.addHeader("X-XRDS-Location", httpRequest.getScheme() + "://" + httpRequest.getServerName() + (httpRequest.getServerPort() != 80 ? ":" + httpRequest.getServerPort() : "") + "/xrds");
		
		chain.doFilter(request, response);
	}

	public void init(FilterConfig arg0) throws ServletException {
		//No action...
	}
}