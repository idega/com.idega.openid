package com.idega.openid.server.dao;

import com.idega.openid.server.data.OpenIdSignupInfo;

public interface OpenIDSignupDAO {

	public void createOpenIDSignupInfo(String confirmAttribute, String confirmCode, String personalID, String email, String loginName);

	public OpenIdSignupInfo getOpenIdSignupInfo(String confirmAttribute, String confirmCode);
	
}
