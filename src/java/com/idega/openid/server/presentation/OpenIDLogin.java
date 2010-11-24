package com.idega.openid.server.presentation;

import javax.faces.context.FacesContext;

import com.idega.block.login.presentation.Login2;
import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.openid.OpenIDConstants;
import com.idega.openid.server.bean.OpenIDServerBean;
import com.idega.presentation.IWContext;


public class OpenIDLogin extends Login2 {

	@Override
	public void initializeComponent(FacesContext context) {
		IWContext iwc = IWContext.getIWContext(context);

		setUnAuthenticatedFaceletPath(getBundle(context, OpenIDConstants.IW_BUNDLE_IDENTIFIER).getFaceletURI("server/login/loggedOut.xhtml"));
		OpenIDServerBean bean = getBeanInstance("openIDServerBean");
		if (!iwc.isLoggedOn() && bean.getDoRedirect() != null) {
			
			iwc.setSessionAttribute(LoginBusinessBean.PARAMETER_USERNAME, bean.getUsername());
			
			this.setURLToRedirectToOnLogon(bean.getServerUrl());
			this.setURLToRedirectToOnLogonFailed(bean.getServerUrl());
		}

		super.initializeComponent(context);
	}
}