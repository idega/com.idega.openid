package com.idega.openid.server.dao;

import javax.ejb.CreateException;

import com.idega.openid.server.data.OpenIdSignupInfo;

public interface OpenIDSignupDAO {

	public void createOpenIDSignupInfo(String confirmAttribute, String confirmCode, String personalID, String email, String loginName);

	public OpenIdSignupInfo getOpenIdSignupInfo(String confirmAttribute, String confirmCode);
	
	public void activateLogin( String login, String newPassword) throws CreateException;
	
}
