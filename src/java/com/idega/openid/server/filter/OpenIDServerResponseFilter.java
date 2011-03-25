package com.idega.openid.server.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.idega.idegaweb.IWMainApplication;
import com.idega.openid.OpenIDConstants;
import com.idega.openid.util.OpenIDUtil;
import com.idega.servlet.filter.BaseFilter;

public class OpenIDServerResponseFilter extends BaseFilter {

	FilterConfig filterConfig = null;
	
	public void destroy() {
		//No action...
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		
		httpResponse.addHeader("X-XRDS-Location", httpRequest.getScheme() + "://" + httpRequest.getServerName() + (httpRequest.getServerPort() != 80 ? ":" + httpRequest.getServerPort() : "") + "/xrds");
		
		String serverName = request.getServerName();
		String requestUri = httpRequest.getRequestURI();
		String subDomain = new OpenIDUtil().getSubDomain(serverName);
		if (subDomain != null && requestUri.equals("/")) {
			IWMainApplication iwma = IWMainApplication.getIWMainApplication(httpRequest.getSession().getServletContext());

			RequestDispatcher dispatch = httpRequest.getRequestDispatcher(iwma.getBundle(OpenIDConstants.IW_BUNDLE_IDENTIFIER).getResourcesVirtualPath() + "/jsp/userPage.jsp");
			dispatch.forward(request, response);
		}
		else {
			chain.doFilter(request, response);
		}
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}
}