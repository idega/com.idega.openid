package com.idega.openid.server.data;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = AuthenticatedRealm.ENTITY_NAME)
@NamedQueries({
        @NamedQuery(name = "authRealm.findAll", query = "select a from AuthenticatedRealm a"),
        @NamedQuery(name = "authRealm.findAllByUser", query = "select a from AuthenticatedRealm a where a.userUUID = :userUUID"),
        @NamedQuery(name = "authRealm.findByUserAndRealm", query = "select a from AuthenticatedRealm a where a.userUUID = :userUUID and a.realm = :realm")
})
public class AuthenticatedRealm implements Serializable {

	private static final long serialVersionUID = 2194400499476632504L;

	public static final String ENTITY_NAME = "openid_auth_realm";
	
	private static final String COLUMN_ID = "openid_auth_realm_id";
	private static final String COLUMN_USER = "user_uuid";
	private static final String COLUMN_REALM = "realm";
	private static final String COLUMN_TIMESTAMP = "added_when";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = AuthenticatedRealm.COLUMN_ID)
	private Long id;
	
	@Column(name = AuthenticatedRealm.COLUMN_USER, nullable = false)
	private String userUUID;
	
	@Column(name = AuthenticatedRealm.COLUMN_REALM, length = 255)
	private String realm;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = AuthenticatedRealm.COLUMN_TIMESTAMP)
	private Date addedWhen;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUserUUID() {
		return userUUID;
	}

	public void setUserUUID(String userUUID) {
		this.userUUID = userUUID;
	}

	public String getRealm() {
		return realm;
	}

	public void setRealm(String realm) {
		this.realm = realm;
	}

	public Date getAddedWhen() {
		return addedWhen;
	}

	public void setAddedWhen(Date addedWhen) {
		this.addedWhen = addedWhen;
	}
}