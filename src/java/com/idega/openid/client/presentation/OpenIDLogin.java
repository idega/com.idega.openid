package com.idega.openid.client.presentation;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import com.idega.core.accesscontrol.business.LoginBusinessBean;
import com.idega.core.accesscontrol.business.LoginState;
import com.idega.facelets.ui.FaceletComponent;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.openid.OpenIDConstants;
import com.idega.openid.client.bean.OpenIDClientBean;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.util.PresentationUtil;

public class OpenIDLogin extends IWBaseComponent {

	private String styleClass;
	
	private String unAuthenticatedFaceletPath;
	private String authenticatedFaceletPath;
	private String authenticationFailedFaceletPath;

	public OpenIDLogin() {
		setStyleClass("openIDLogin");
		setTransient(false);
	}

	public String getBundleIdentifier() {
		return OpenIDConstants.IW_BUNDLE_IDENTIFIER;
	}
	
	@Override
	protected void initializeComponent(FacesContext context) {
		IWContext iwc = IWContext.getIWContext(context);
		
		PresentationUtil.addStyleSheetToHeader(iwc, getBundle(context, getBundleIdentifier()).getVirtualPathWithFileNameString("style/openid.css"));
		
		if (this.unAuthenticatedFaceletPath == null) {
			this.unAuthenticatedFaceletPath = getBundle(context, getBundleIdentifier()).getFaceletURI("client/loggedOut.xhtml");
		}
		if (this.authenticatedFaceletPath == null) {
			this.authenticatedFaceletPath = getBundle(context, getBundleIdentifier()).getFaceletURI("client/loggedIn.xhtml");
		}
		if (this.authenticationFailedFaceletPath == null) {
			this.authenticationFailedFaceletPath = getBundle(context, getBundleIdentifier()).getFaceletURI("client/loginFailed.xhtml");
		}

		OpenIDClientBean bean = getBeanInstance("openIDClientBean");
		bean.setAction(iwc.getRequestURI());
		bean.setStyleClass(getStyleClass());
		bean.setLocaleStyle(getCurrentLocaleLanguage(iwc));

		if (iwc.isLoggedOn()) {
			add(getLoggedInPart(context, bean));
		}
		else {
			LoginState state = LoginBusinessBean.internalGetState(iwc);
			if (state.equals(LoginState.LOGGED_OUT) || state.equals(LoginState.NO_STATE)) {
				add(getLoggedOutPart(context));
			}
			else {
				add(getLoginFailedPart(context, bean));
			}
		}
	}
	
	private UIComponent getLoggedOutPart(FacesContext context) {
		IWContext iwc = IWContext.getIWContext(context);
		
		FaceletComponent facelet = (FaceletComponent) iwc.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(this.unAuthenticatedFaceletPath);	
		
		return facelet;
	}
	
	private UIComponent getLoggedInPart(FacesContext context, OpenIDClientBean bean) {
		IWContext iwc = IWContext.getIWContext(context);
		
		bean.addParameter(LoginBusinessBean.LoginStateParameter, LoginBusinessBean.LOGIN_EVENT_LOGOFF);
		bean.setOutput(iwc.getCurrentUser().getName());

		FaceletComponent facelet = (FaceletComponent) iwc.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(this.authenticatedFaceletPath);	
		
		return facelet;
	}
	
	private UIComponent getLoginFailedPart(FacesContext context, OpenIDClientBean bean) {
		IWContext iwc = IWContext.getIWContext(context);
		IWResourceBundle iwrb = getBundle(context, getBundleIdentifier()).getResourceBundle(iwc);

		bean.addParameter(LoginBusinessBean.LoginStateParameter, LoginBusinessBean.LOGIN_EVENT_TRYAGAIN);
		bean.setOutput(iwrb.getLocalizedString("login_failed", "Login failed"));

		FaceletComponent facelet = (FaceletComponent) iwc.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(this.authenticationFailedFaceletPath);	
		
		return facelet;
	}
	
	private String getCurrentLocaleLanguage(IWContext iwc) {
		return iwc.getLocale().getLanguage();
	}

	private String getStyleClass() {
		return this.styleClass;
	}
	
	public void setStyleClass(String style) {
		this.styleClass = style;
	}
	
	public void setUnAuthenticatedFaceletPath(String pathToFacelet) {
		this.unAuthenticatedFaceletPath = pathToFacelet;
	}
	
	public void setAuthenticatedFaceletPath(String pathToFacelet) {
		this.authenticatedFaceletPath = pathToFacelet;
	}
	
	public void setAuthenticationFailedFaceletPath(String pathToFacelet) {
		this.authenticationFailedFaceletPath = pathToFacelet;
	}
}