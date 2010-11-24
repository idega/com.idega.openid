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
@Table(name = RegisteredRealm.ENTITY_NAME)
@NamedQueries({
        @NamedQuery(name = "regRealm.findAll", query = "select a from RegisteredRealm a")
})
public class RegisteredRealm implements Serializable {

	private static final long serialVersionUID = 2194400499476632504L;

	public static final String ENTITY_NAME = "openid_reg_realm";
	
	private static final String COLUMN_ID = "openid_realm_id";
	private static final String COLUMN_REALM = "realm";
	private static final String COLUMN_TIMESTAMP = "added_when";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = RegisteredRealm.COLUMN_ID)
	private Long id;
	
	@Column(name = RegisteredRealm.COLUMN_REALM, length = 255)
	private String realm;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = RegisteredRealm.COLUMN_TIMESTAMP)
	private Date addedWhen;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
		result = prime * result + ((realm == null) ? 0 : realm.hashCode());
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
		RegisteredRealm other = (RegisteredRealm) obj;
		if (addedWhen == null) {
			if (other.addedWhen != null)
				return false;
		} else if (!addedWhen.equals(other.addedWhen))
			return false;
		if (realm == null) {
			if (other.realm != null)
				return false;
		} else if (!realm.equals(other.realm))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RegisteredRealm [addedWhen=" + addedWhen + ", id=" + id
				+ ", realm=" + realm + "]";
	}
}