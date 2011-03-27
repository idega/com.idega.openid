package com.idega.openid;

public class OpenIDConstants {

	public static final String IW_BUNDLE_IDENTIFIER = "com.idega.openid";

	public static final String PROPERTY_OPENID_PROVIDER = "openid.provider";
	public static final String PROPERTY_SERVER_URL = "openid.server.url";
	public static final String PROPERTY_END_POINT_URL = "openid.endpoint.url";
	public static final String PROPERTY_USER_SETUP_URL = "openid.user.setup.url";
	public static final String PROPERTY_AUTHENTICATION_URL = "openid.authentication.url";
	public static final String PROPERTY_CONSUMER_MANAGER_TIMEOUT = "openid.consumer_manager.timeout";
	public static final String PROPERTY_CONSUMER_MANAGER_CONNECTION_TIMEOUT = "openid.consumer_manager.connection_timeout";
	public static final String PROPERTY_CONSUMER_MANAGER_SOCKET_TIMEOUT = "openid.consumer_manager.socket_timeout";
	public static final String PROPERTY_CONSUMER_MANAGER_MAX_REDIRECTS = "openid.consumer_manager.max_redirects";
	public static final String PROPERTY_OPENID_IDENTITY_FORMAT = "openid.identity.format";
	public static final String PROPERTY_OPENID_AUTO_CREATE_USERS = "openid.auto.create.users";
	public static final String PROPERTY_OPENID_CLIENT_UPDATE_USER_INFO = "openid.client.update.user.info";
	public static final String PROPERTY_SIGN_EXTENSIONS = "openid.sign.extensions";
	
	public static final String PARAMETER_RETURN = "openid_return";
	public static final String PARAMETER_IDENTIFIER = "openid_identifier";
	public static final String PARAMETER_REALM = "openid.realm";
	public static final String PARAMETER_ALLOWED = "openid.allowed";

	public static final String PARAMETER_CHECK_AUTHENTICATION = "check_authentication";
	public static final String PARAMETER_CHECKID_IMMEDIATE = "checkid_immediate";
	public static final String PARAMETER_CHECKID_SETUP = "checkid_setup";
	public static final String PARAMETER_ASSOCIATE = "associate";
	public static final String PARAMETER_OPENID_MODE = "openid.mode";
	public static final String PARAMETER_ASSOCIATE_HANDLE = "openid.assoc_handle";
	
	public static final String ATTRIBUTE_CONSUMER_MANAGER = "openid.consumer_manager";
	public static final String ATTRIBUTE_SERVER_MANAGER = "openid.server_manager";
	
//	public static final String ATTRIBUTE_ALLOWED_REALM = "openid_allowed_realm";
	public static final String ATTRIBUTE_RETURN_URL = "openid_return_url";

	public static final String STATUS_SUCCESS = "SUCCESS";
	
	public static final String ATTRIBUTE_ALIAS_EMAIL = "email";
	public static final String ATTRIBUTE_ALIAS_PERSONAL_ID = "personalID";
	public static final String ATTRIBUTE_ALIAS_FULL_NAME = "fullname";
	public static final String ATTRIBUTE_ALIAS_DATE_OF_BIRTH = "dob";
	public static final String ATTRIBUTE_ALIAS_GENDER = "gender";
	public static final String ATTRIBUTE_ALIAS_NICKNAME = "nickname";
	public static final String ATTRIBUTE_ALIAS_POSTCODE = "postcode";
	public static final String ATTRIBUTE_ALIAS_COUNTRY = "country";
	public static final String ATTRIBUTE_ALIAS_LANGUAGE = "language";
	public static final String ATTRIBUTE_ALIAS_TIMEZONE = "timezone";
	
	public static final String ATTRIBUTE_TYPE_EMAIL = "http://axschema.org/contact/email";
	public static final String ATTRIBUTE_TYPE_PERSONAL_ID = "http://www.elykill.is/contact/personalID";
	public static final String ATTRIBUTE_TYPE_FULL_NAME = "http://axschema.org/namePerson";
	public static final String ATTRIBUTE_TYPE_FRIENDLY_NAME = "http://axschema.org/namePerson/friendly";
	public static final String ATTRIBUTE_TYPE_DATE_OF_BIRTH = "http://axschema.org/birthDate";
	public static final String ATTRIBUTE_TYPE_GENDER = "http://axschema.org/person/gender";
	public static final String ATTRIBUTE_TYPE_POSTAL_CODE = "http://axschema.org/contact/postalCode/home";
	public static final String ATTRIBUTE_TYPE_COUNTRY = "http://axschema.org/contact/country/home";
	public static final String ATTRIBUTE_TYPE_LANGUAGE = "http://axschema.org/pref/language";
	public static final String ATTRIBUTE_TYPE_TIMEZONE = "http://axschema.org/pref/timezone";

	public static final String LOGIN_TYPE = "openid";

}