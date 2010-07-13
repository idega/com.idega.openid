package com.idega.openid.server.presentation;

import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.web2.business.JQuery;
import com.idega.facelets.ui.FaceletComponent;
import com.idega.openid.OpenIDConstants;
import com.idega.openid.server.bean.OpenIDServerBean;
import com.idega.openid.server.dao.OpenIDServerDAO;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.PresentationUtil;
import com.idega.util.expression.ELUtil;

public class OpenIDAuthenticate extends IWBaseComponent {

	private static final String PARAMETER_ACTION = "prm_action";
	private static final String PARAMETER_ALWAYS_ALLOW = "prm_always_allow";
	
	private static final int ACTION_ALLOW = 1;
	private static final int ACTION_DENY = 2;
	
	@Autowired
	private OpenIDServerDAO dao;
	
	@Autowired
	private JQuery jQuery;

	public String getBundleIdentifier() {
		return OpenIDConstants.IW_BUNDLE_IDENTIFIER;
	}
	
	@Override
	public void initializeComponent(FacesContext context) {
		IWContext iwc = IWContext.getIWContext(context);
		
		User user = iwc.getCurrentUser();
		String realm = (String) iwc.getSessionAttribute(OpenIDConstants.PARAMETER_REALM);

		if (iwc.isParameterSet(PARAMETER_ACTION)) {
			int action = Integer.parseInt(iwc.getParameter(PARAMETER_ACTION));
			if (action == ACTION_ALLOW) {
				if (iwc.isParameterSet(PARAMETER_ALWAYS_ALLOW)) {
					getDAO().createAuthenticatedRealm(user.getUniqueId(), realm);
				}
				else {
					iwc.setSessionAttribute(OpenIDConstants.ATTRIBUTE_ALLOWED_REALM, realm);
				}
				iwc.sendRedirect((String) iwc.getSessionAttribute(OpenIDConstants.ATTRIBUTE_SERVER_URL));
				return;
			}
			else if (action == ACTION_DENY) {
				iwc.sendRedirect((String) iwc.getSessionAttribute(OpenIDConstants.ATTRIBUTE_RETURN_URL));
				return;
			}
		}

		OpenIDServerBean bean = getBeanInstance("openIDServerBean");
		bean.setRealm(realm);
		
		PresentationUtil.addJavaScriptSourceLineToHeader(iwc, getJQuery().getBundleURIToJQueryLib());
		PresentationUtil.addJavaScriptSourceLineToHeader(iwc, getBundle(context, getBundleIdentifier()).getVirtualPathWithFileNameString("javascript/authenticate.js"));

		FaceletComponent facelet = (FaceletComponent) iwc.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(getBundle(context, OpenIDConstants.IW_BUNDLE_IDENTIFIER).getFaceletURI("server/authenticate/view.xhtml"));
		add(facelet);
	}
	
	private OpenIDServerDAO getDAO() {
		if (dao == null) {
			ELUtil.getInstance().autowire(this);
		}
		
		return dao;
	}

	private JQuery getJQuery() {
		if (jQuery == null) {
			ELUtil.getInstance().autowire(this);
		}
		return jQuery;
	}
}