package com.idega.openid.server.presentation;

import java.io.IOException;

import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.web2.business.JQuery;
import com.idega.facelets.ui.FaceletComponent;
import com.idega.openid.OpenIDConstants;
import com.idega.openid.server.bean.OpenIDSignUpBean;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.servlet.filter.IWBundleResourceFilter;
import com.idega.util.PresentationUtil;
import com.idega.util.expression.ELUtil;

public class OpenIDRegistration extends IWBaseComponent {
	
	@Autowired
	private JQuery jQuery;
	
	@Autowired
	private OpenIDSignUpBean signupBean;
	
	
	public String getBundleIdentifier() {
		return OpenIDConstants.IW_BUNDLE_IDENTIFIER;
	}
	
	@Override
	public void initializeComponent(FacesContext context) {
		
		IWContext iwc = IWContext.getIWContext(context);
		
		checkCopyOfFaceletToWebapp(context, "server/signup/request.xhtml");
		checkCopyOfFaceletToWebapp(context, "server/signup/requested.xhtml");
		checkCopyOfFaceletToWebapp(context, "server/signup/confirm.xhtml");
		checkCopyOfFaceletToWebapp(context, "server/signup/signup.xhtml");
		checkCopyOfFaceletToWebapp(context, "server/signup/done.xhtml");
		checkCopyOfFaceletToWebapp(context, "server/signup/error.xhtml");
		
		FaceletComponent facelet = (FaceletComponent) iwc.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(getBundle(context, OpenIDConstants.IW_BUNDLE_IDENTIFIER).getFaceletURI("#{openIDSignUpBean.faceletPath}"));
		add(facelet);
	}
	
	public void encodeBegin(FacesContext context) throws IOException {
		super.encodeBegin(context);
		
		IWContext iwc = IWContext.getIWContext(context);
		
		PresentationUtil.addJavaScriptSourceLineToHeader(iwc, getJQuery().getBundleURIToJQueryLib());
		
		PresentationUtil.addJavaScriptSourceLineToHeader(iwc, getBundle(context, getBundleIdentifier()).getVirtualPathWithFileNameString("javascript/signup.js"));
		PresentationUtil.addStyleSheetToHeader(iwc, getBundle(context, getBundleIdentifier()).getVirtualPathWithFileNameString("style/signup.css"));
	}
	
	protected void checkCopyOfFaceletToWebapp(FacesContext context, String src){
		IWBundleResourceFilter.checkCopyOfResourceToWebapp(context, getBundle(context, getBundleIdentifier()).getFaceletURI(src));
	}
	
	private JQuery getJQuery() {
		if (jQuery == null) {
			ELUtil.getInstance().autowire(this);
		}
		return jQuery;
	}
	
	private OpenIDSignUpBean getOpenIDSignUpBean() {
		if (signupBean == null) {
			ELUtil.getInstance().autowire(this);
		}
		return signupBean;
	}
}