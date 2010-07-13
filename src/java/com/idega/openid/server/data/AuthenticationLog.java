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
@Table(name = AuthenticationLog.ENTITY_NAME)
@NamedQueries({
        @NamedQuery(name = "authLog.findAll", query = "select a from AuthenticationLog a"),
        @NamedQuery(name = "authLog.findAllByUser", query = "select a from AuthenticationLog a where a.userUUID = :userUUID"),
        @NamedQuery(name = "authLog.findByUserAndRealm", query = "select a from AuthenticationLog a where a.userUUID = :userUUID and a.realm = :realm")
})
public class AuthenticationLog implements Serializable {

	private static final long serialVersionUID = 1978248620732226256L;

	public static final String ENTITY_NAME = "openid_auth_log";
	
	private static final String COLUMN_ID = "openid_auth_log_id";
	private static final String COLUMN_USER = "user_uuid";
	private static final String COLUMN_REALM = "realm";
	private static final String COLUMN_TIMESTAMP = "log_timestamp";
	private static final String COLUMN_STATUS = "status";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = AuthenticationLog.COLUMN_ID)
	private Long id;
	
	@Column(name = AuthenticationLog.COLUMN_USER, nullable = false)
	private String userUUID;
	
	@Column(name = AuthenticationLog.COLUMN_REALM, length = 255)
	private String realm;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = AuthenticationLog.COLUMN_TIMESTAMP)
	private Date timestamp;
	
	@Column(name = AuthenticationLog.COLUMN_STATUS)
	private String status;

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

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}