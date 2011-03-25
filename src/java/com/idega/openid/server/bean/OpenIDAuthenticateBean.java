package com.idega.openid.server.bean;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.openid.OpenIDConstants;
import com.idega.openid.server.dao.OpenIDServerDAO;
import com.idega.openid.server.data.AuthorizedAttribute;
import com.idega.presentation.IWContext;
import com.idega.util.StringHandler;
import com.idega.util.expression.ELUtil;


@Service("openIDAuthenticateBean")
@Scope("request")
public class OpenIDAuthenticateBean {
	
	private boolean alwaysAllow = false;

	public boolean getAlwaysAllow() {
		return alwaysAllow;
	}
	
	public void setAlwaysAllow(boolean alwaysAllow) {
		this.alwaysAllow = alwaysAllow;
	}
	
	/* Actions begin*/
	public void allow(){
		IWContext iwc = IWContext.getCurrentInstance();
		OpenIDServerBean serverBean = ELUtil.getInstance().getBean("openIDServerBean");
		
		String redirectUrl = serverBean.getServerUrl();
		
		if(redirectUrl != null){
			String handle = StringHandler.getRandomString(10);
			String parameter = OpenIDConstants.PARAMETER_ALLOWED+"="+handle;		
			
			iwc.setSessionAttribute(OpenIDConstants.PARAMETER_ALLOWED,handle);

			if(!redirectUrl.contains("?")){
				redirectUrl += "?"+parameter;
			} else {
				redirectUrl += "&"+parameter;
			}
		}
		
		if (getAlwaysAllow()) {
			OpenIDServerDAO theDao = ELUtil.getInstance().getBean("openIDServerDAO");
			List<AuthorizedAttribute> attributes = serverBean.getRequestedAttributes();
			for(AuthorizedAttribute attr : attributes){
				theDao.saveAuthorizedAttribute(attr);
			}
		}
		
		iwc.sendRedirect(redirectUrl);
	}
	
	public void deny(){
		IWContext iwc = IWContext.getCurrentInstance();
		OpenIDServerBean serverBean = ELUtil.getInstance().getBean("openIDServerBean");
		String returnUrl = serverBean.getReturnUrl();
		serverBean.invalidate();
		iwc.sendRedirect(returnUrl);
	}
	
	/* Actions end */
}