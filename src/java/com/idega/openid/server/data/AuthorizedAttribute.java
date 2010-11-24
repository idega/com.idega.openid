package com.idega.openid.server.data;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = AuthorizedAttribute.ENTITY_NAME)
@UniqueConstraint(columnNames={AuthorizedAttribute.COLUMN_USER,AuthorizedAttribute.COLUMN_REALM,AuthorizedAttribute.COLUMN_EXCHANGE_ATTRIBUTE})
@NamedQueries({
        @NamedQuery(name = "authAttr.findAll", query = "select a from AuthorizedAttribute a"),
        @NamedQuery(name = "authAttr.findAllByUser", query = "select a from AuthorizedAttribute a where a.userUUID = :userUUID"),
        @NamedQuery(name = "authAttr.findAllByUserAndRealm", query = "select a from AuthorizedAttribute a where a.userUUID = :userUUID and a.realm = :realm"),
        @NamedQuery(name = "authAttr.findByUserAndRealmAndExchangeAttribute", query = "select a from AuthorizedAttribute a where a.userUUID = :userUUID and a.realm = :realm and a.exchangeAttribute = :exchangeAttribute")
})
public class AuthorizedAttribute implements Serializable {

	private static final long serialVersionUID = 2194400499476632504L;

	public static final String ENTITY_NAME = "openid_auth_realm";
	
	static final String COLUMN_ID = "openid_auth_realm_id";
	static final String COLUMN_USER = "user_uuid";
	static final String COLUMN_REALM = "realm";
	static final String COLUMN_EXCHANGE_ATTRIBUTE = "exchange_attr_id";
	static final String COLUMN_IS_ALLOWED = "is_allowed";
	static final String COLUMN_TIMESTAMP = "added_when";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = AuthorizedAttribute.COLUMN_ID)
	private Long id;
	
	@Column(name = AuthorizedAttribute.COLUMN_USER, nullable = false)
	private String userUUID;
	
	@ManyToOne(optional=true)
	private ExchangeAttribute exchangeAttribute;
	
	@Column(name = AuthorizedAttribute.COLUMN_IS_ALLOWED, nullable = false) 
	private boolean isAllowed;
	
	@Column(name = AuthorizedAttribute.COLUMN_REALM, nullable = false)
	private String realm;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = AuthorizedAttribute.COLUMN_TIMESTAMP)
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
	
	public ExchangeAttribute getExchangeAttribute(){
		return exchangeAttribute;
	}
	
	public void setExchangeAttribute(ExchangeAttribute attr){
		this.exchangeAttribute = attr;
	}
	
	public boolean getIsAllowed(){
		return isAllowed;
	}
	
	public void setIsAllowed(boolean allowed){
		this.isAllowed = allowed;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((addedWhen == null) ? 0 : addedWhen.hashCode());
		result = prime * result
				+ ((exchangeAttribute == null) ? 0 : exchangeAttribute.hashCode());
		result = prime * result + (isAllowed ? 1231 : 1237);
		result = prime * result + ((realm == null) ? 0 : realm.hashCode());
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
		AuthorizedAttribute other = (AuthorizedAttribute) obj;
		if (addedWhen == null) {
			if (other.addedWhen != null)
				return false;
		} else if (!addedWhen.equals(other.addedWhen))
			return false;
		if (exchangeAttribute == null) {
			if (other.exchangeAttribute != null)
				return false;
		} else if (!exchangeAttribute.equals(other.exchangeAttribute))
			return false;
		if (isAllowed != other.isAllowed)
			return false;
		if (realm == null) {
			if (other.realm != null)
				return false;
		} else if (!realm.equals(other.realm))
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
		return "AuthorizedAttribute [addedWhen=" + addedWhen + ", attribute="
				+ exchangeAttribute + ", id=" + id + ", isAllowed=" + isAllowed
				+ ", realm=" + realm + ", userUUID=" + userUUID + "]";
	}
	
	@Transient
	public boolean isNotYetStored(){
		return id == null;
	}
	
}