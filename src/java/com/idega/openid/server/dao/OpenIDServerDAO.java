package com.idega.openid.server.dao;

import java.util.List;

import com.idega.core.persistence.GenericDao;
import com.idega.openid.server.data.AuthenticatedRealm;

public interface OpenIDServerDAO extends GenericDao {

	public void createAuthenticatedRealm(String userUUID, String realm);
	
	public List<AuthenticatedRealm> getAuthenticatedRealmsByUser(String userUUID);
	
	public AuthenticatedRealm getAuthenticatedRealm(String userUUID, String realm);

	public void createLogEntry(String userUUID, String realm);
}