package com.idega.openid.server.bean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.expression.ELUtil;

/**
 * This bean is a temporary fix for the signup procedure since the facelet component 
 * does not preserve state when validation fails.
 * 
 * @deprecated Use OpenIDSignUpBean instead
 * @author Gummi
 *
 */

@Service("openIDActivateAccountBean")
@Scope("request")
public class OpenIDActivateAccountBean {

	@Autowired
	private OpenIDSignUpBean signup;
	
	public OpenIDActivateAccountBean(){
		int s = getOpenIDSignUpBean().getState();
		if(s < OpenIDSignUpBean.STATE_CONFIRM){
			getOpenIDSignUpBean().setState(OpenIDSignUpBean.STATE_CONFIRM); //Changes the default state
		}
	}
	
	private OpenIDSignUpBean getOpenIDSignUpBean() {
		if (signup == null) {
			ELUtil.getInstance().autowire(this);
		}
		return signup;
	}

	public int getState() {
		return getOpenIDSignUpBean().getState();
	}

	public void setState(int state) {
		getOpenIDSignUpBean().setState(state);
	}
	
	public String getFaceletPath() {
		return getOpenIDSignUpBean().getFaceletPath();
	}

}
