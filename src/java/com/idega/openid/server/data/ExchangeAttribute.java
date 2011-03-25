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
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = ExchangeAttribute.ENTITY_NAME)
//@UniqueConstraint(columnNames={ExchangeAttribute.COLUMN_ATTRIBUTE_NAME,ExchangeAttribute.COLUMN_ATTRIBUTE_TYPE})
@NamedQueries({
        @NamedQuery(name = "exchangeAttr.findAll", query = "select a from ExchangeAttribute a"),
        @NamedQuery(name = "exchangeAttr.findByNameOrType", query = "select a from ExchangeAttribute a where a.name = :name or a.type = :type"),
        @NamedQuery(name = "exchangeAttr.findByName", query = "select a from ExchangeAttribute a where a.name = :name")
})
public class ExchangeAttribute implements Serializable {

	private static final long serialVersionUID = 2194400499476632504L;

	public static final String ENTITY_NAME = "openid_exchange_attr";
	
	static final String COLUMN_ID = "openid_exchange_attr_id";
	static final String COLUMN_ATTRIBUTE_NAME = "attr_name";
	static final String COLUMN_ATTRIBUTE_TYPE = "attr_type";
	static final String COLUMN_TIMESTAMP = "added_when";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = ExchangeAttribute.COLUMN_ID)
	private Long id;
	
	@Column(name = ExchangeAttribute.COLUMN_ATTRIBUTE_NAME, nullable = false)
	private String name;
	
	@Column(name = ExchangeAttribute.COLUMN_ATTRIBUTE_TYPE, length = 255)
	private String type;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = ExchangeAttribute.COLUMN_TIMESTAMP)
	private Date addedWhen;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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
				+ ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((type == null) ? 0 : type.hashCode());
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
		ExchangeAttribute other = (ExchangeAttribute) obj;
		if (addedWhen == null) {
			if (other.addedWhen != null)
				return false;
		} else if (!addedWhen.equals(other.addedWhen))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ExchangeAttribute [addedWhen=" + addedWhen + ", attrName="
				+ name + ", attrType=" + type + ", id=" + id + "]";
	}
}