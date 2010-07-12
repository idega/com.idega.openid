package com.idega.openid.server.presentation;

import javax.faces.context.FacesContext;

import com.idega.block.login.presentation.Login2;
import com.idega.openid.OpenIDConstants;
import com.idega.presentation.IWContext;


public class OpenIDLogin extends Login2 {

	@Override
	public void initializeComponent(FacesContext context) {
		IWContext iwc = IWContext.getIWContext(context);
		
		if (iwc.getSessionAttribute(OpenIDConstants.ATTRIBUTE_DO_REDIRECT) != null) {
			this.setURLToRedirectToOnLogon((String) iwc.getSessionAttribute(OpenIDConstants.ATTRIBUTE_SERVER_URL));
		}
		super.initializeComponent(context);
	}
}