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
	private static final String COLUMN_EXCHANGED_ATTRIBUTES = "exchanged_attr";
	
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
	
	@Column(name = AuthenticationLog.COLUMN_EXCHANGED_ATTRIBUTES, length = 2000)
	private String exchangedAttributes;

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
	
	public String getExchangedAttributes() {
		return exchangedAttributes;
	}

	public void setExchangedAttributes(String exchangedAttributes) {
		this.exchangedAttributes = exchangedAttributes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((exchangedAttributes == null) ? 0 : exchangedAttributes
						.hashCode());
		result = prime * result + ((realm == null) ? 0 : realm.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result
				+ ((timestamp == null) ? 0 : timestamp.hashCode());
		result = prime * result
				+ ((userUUID == null) ? 0 : userUUID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuthenticationLog other = (AuthenticationLog) obj;
		if (exchangedAttributes == null) {
			if (other.exchangedAttributes != null)
				return false;
		} else if (!exchangedAttributes.equals(other.exchangedAttributes))
			return false;
		if (realm == null) {
			if (other.realm != null)
				return false;
		} else if (!realm.equals(other.realm))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		if (userUUID == null) {
			if (other.userUUID != null)
				return false;
		} else if (!userUUID.equals(other.userUUID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AuthenticationLog [exchangedAttributes=" + exchangedAttributes
				+ ", id=" + id + ", realm=" + realm + ", status=" + status
				+ ", timestamp=" + timestamp + ", userUUID=" + userUUID + "]";
	}
	
}