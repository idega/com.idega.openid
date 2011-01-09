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
@Table(name = OpenIdSignupInfo.ENTITY_NAME)
@NamedQueries({
        @NamedQuery(name = "openidSignup.findAll", query = "select a from OpenIdSignupInfo a"),
        @NamedQuery(name = OpenIdSignupInfo.QUERY_FIND_VALID_BY_CONFIRM_ATTRIBUTE_AND_CODE, query = "select a from OpenIdSignupInfo a where a.confirmAttribute = :confirmAttribute and a.confirmCode = :confirmCode and a.isValid != false"),
        @NamedQuery(name = OpenIdSignupInfo.QUERY_FIND_VALID_BY_CONFIRM_ATTRIBUTE_AND_CODE_AND_PERSONAL_ID, query = "select a from OpenIdSignupInfo a where a.confirmAttribute = :confirmAttribute and a.confirmCode = :confirmCode and a.personalID = :personalID and a.isValid != false"),
        @NamedQuery(name = OpenIdSignupInfo.QUERY_FIND_ALL_BY_PERSONAL_ID, query = "select a from OpenIdSignupInfo a where a.personalID = :personalID"),
        @NamedQuery(name = OpenIdSignupInfo.QUERY_FIND_VALID_BY_PERSONAL_ID, query = "select a from OpenIdSignupInfo a where a.personalID = :personalID and a.isValid != false")
})
public class OpenIdSignupInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5482743088144038538L;
	
	public static final String QUERY_FIND_VALID_BY_CONFIRM_ATTRIBUTE_AND_CODE = "openidSignup.findValidByConfirmAttributeAndCode";
	public static final String QUERY_FIND_VALID_BY_CONFIRM_ATTRIBUTE_AND_CODE_AND_PERSONAL_ID = "openidSignup.findValidByConfirmAttributeAndCodeAndPersonalID";
	public static final String QUERY_FIND_ALL_BY_PERSONAL_ID = "openidSignup.findAllByPersonalID";
	public static final String QUERY_FIND_VALID_BY_PERSONAL_ID = "openidSignup.findValidByPersonalID";
	
	public static final String ENTITY_NAME = "openid_signup_info";
	
	private static final String COLUMN_ID = "openid_signup_info_id";
	private static final String COLUMN_CONFIRM_ATTRIBUTE = "confirm_attribute";
	private static final String COLUMN_CONFIRM_CODE = "confirm_code";
	private static final String COLUMN_PERSONAL_ID = "personal_id";
	private static final String COLUMN_LOGIN_NAME = "login";
	private static final String COLUMN_EMAIL = "email";
	private static final String COLUMN_TIMESTAMP = "added_when";
	private static final String COLUMN_IS_VALID = "is_valid";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = OpenIdSignupInfo.COLUMN_ID)
	private Long id;
	
	@Column(name = OpenIdSignupInfo.COLUMN_CONFIRM_ATTRIBUTE, length = 40)
	private String confirmAttribute;
	@Column(name = OpenIdSignupInfo.COLUMN_CONFIRM_CODE, length = 30)
	private String confirmCode;
	@Column(name = OpenIdSignupInfo.COLUMN_PERSONAL_ID, length = 100)
	private String personalID;
	@Column(name = OpenIdSignupInfo.COLUMN_LOGIN_NAME, length = 100)
	private String loginName;
	@Column(name = OpenIdSignupInfo.COLUMN_EMAIL, length = 100)
	private String email;
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = OpenIdSignupInfo.COLUMN_TIMESTAMP)
	private Date addedWhen;
	@Column(name = OpenIdSignupInfo.COLUMN_IS_VALID)
	private boolean isValid;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public String getConfirmAttribute() {
		return confirmAttribute;
	}
	public void setConfirmAttribute(String confirmAttribute) {
		this.confirmAttribute = confirmAttribute;
	}
	public String getConfirmCode() {
		return confirmCode;
	}
	public void setConfirmCode(String confirmCode) {
		this.confirmCode = confirmCode;
	}
	public String getPersonalID() {
		return personalID;
	}
	public void setPersonalID(String personalID) {
		this.personalID = personalID;
	}
	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getLoginName() {
		return loginName;
	}

	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public Date getAddedWhen() {
		return addedWhen;
	}
	public void setAddedWhen(Date addedWhen) {
		this.addedWhen = addedWhen;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public boolean isValid() {
		return isValid;
	}
	

}
