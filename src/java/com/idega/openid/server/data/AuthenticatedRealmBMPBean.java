package com.idega.openid.server.data;

import java.sql.Timestamp;

import com.idega.data.GenericEntity;
import com.idega.user.data.User;

public class AuthenticatedRealmBMPBean extends GenericEntity {

	private static final String ENTITY_NAME = "openid_auth_realm";
	
	private static final String COLUMN_USER = "user_id";
	private static final String COLUMN_REALM = "realm";
	private static final String COLUMN_TIMESTAMP = "added_when";
	
	@Override
	public String getEntityName() {
		return ENTITY_NAME;
	}

	@Override
	public void initializeAttributes() {
		addAttribute(getIDColumnName());
		
		addManyToOneRelationship(COLUMN_USER, User.class);
		
		addAttribute(COLUMN_REALM, "Realm", String.class);
		addAttribute(COLUMN_TIMESTAMP, "Added", Timestamp.class);
	}
	
	


}