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
		if (!iwc.isLoggedOn() && iwc.getSessionAttribute(OpenIDConstants.ATTRIBUTE_DO_REDIRECT) != null) {
			OpenIDServerBean bean = getBeanInstance("openIDServerBean");
			
			String subdomain = (String) iwc.getSessionAttribute(OpenIDConstants.ATTRIBUTE_SUBDOMAIN);
			bean.setUsername(subdomain);
			iwc.setSessionAttribute(LoginBusinessBean.PARAMETER_USERNAME, subdomain);
			
			this.setURLToRedirectToOnLogon((String) iwc.getSessionAttribute(OpenIDConstants.ATTRIBUTE_SERVER_URL));
		}

		super.initializeComponent(context);
	}
}