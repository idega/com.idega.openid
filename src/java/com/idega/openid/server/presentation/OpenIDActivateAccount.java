package com.idega.openid.server.presentation;

/**
 * This bean is a temporary fix for the signup procedure since the facelet component 
 * does not preserve state when validation fails.
 * 
 * @deprecated
 * @author Gummi
 *
 */
public class OpenIDActivateAccount extends OpenIDRegistration {

	public OpenIDActivateAccount() {
		faceletUriBinding = "#{openIDActivateAccountBean.faceletPath}";
	}
	
}
