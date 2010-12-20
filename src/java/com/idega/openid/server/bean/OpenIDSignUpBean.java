package com.idega.openid.server.bean;

import is.idega.block.nationalregister.webservice.client.business.FerliClient;
import is.idega.block.nationalregister.webservice.client.business.UserHolder;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.mail.MessagingException;
import javax.transaction.UserTransaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.core.location.data.Address;
import com.idega.core.messaging.EmailMessage;
import com.idega.core.messaging.MessagingSettings;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.openid.OpenIDConstants;
import com.idega.openid.server.dao.OpenIDSignupDAO;
import com.idega.openid.server.data.OpenIdSignupInfo;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.IWTimestamp;
import com.idega.util.StringHandler;
import com.idega.util.expression.ELUtil;
import com.idega.util.text.Name;
import com.idega.util.text.SocialSecurityNumber;


@Service("openIDSignUpBean")
@Scope("request")
public class OpenIDSignUpBean {
	
	private static Logger log = Logger.getLogger(OpenIDSignUpBean.class.toString());
	
	public static String PARAM_CONFIRM_ID = "confirm_id";
	
	@Autowired
	private FerliClient natReg;
	
	@Autowired
	private OpenIDSignupDAO signupDAO;
	
	private String fullName;
	private String personalID;
	private String email;
	private String emailConfirmed;
	private String address;
	private String postalCode;
	private String city;
	private String gender;
	private String phoneNumber;
	private String mobileNumber;
	private String login;
	private String password;
	private String confirmedPassword;
	private boolean sendSnailMail;
	
	public static final int STATE_REQUEST = 0;
	public static final int STATE_REQUESTED = 1;
	public static final int STATE_CONFIRM = 2;
	public static final int STATE_SIGNUP = 3;
	public static final int STATE_FINISHED = 4;
	public static final int STATE_ERROR_OCCURRED = 5;
	
	private int state = STATE_REQUEST;
	private String confirmID;
	private String identificationCode;
	private String faceletPath;
	
	private String errorMessage;
	private String errorCode;
	private Object[] errorArguments = null;
	
	public static final String ERREOR_MESSAGE_ID_NATREG_NO_DATA = "com.idega.openid.server.bean.OpenIDSignUpBean.NATREG_NO_DATA";
	public static final String ERROR_CODE_NATREG_NO_DATA = "natreg_no_data";
	
	public static final String ERREOR_MESSAGE_ID_CREATE_USER_FAILED = "com.idega.openid.server.bean.OpenIDSignUpBean.CREATE_USER_FAILED";
	public static final String ERROR_CODE_CREATE_USER_FAILED = "create_user_failed";
	
	public static final String ERREOR_MESSAGE_ID_EMAIL_NOT_SENT = "com.idega.openid.server.bean.OpenIDSignUpBean.EMAIL_NOT_SENT";
	public static final String ERROR_CODE_EMAIL_NOT_SENT = "email_not_sent";

	public static final String ERREOR_MESSAGE_ID_MESSAGE_NOT_SENT_TO_BANK = "com.idega.openid.server.bean.OpenIDSignUpBean.MESSAGE_NOT_SENT_TO_BANK";
	public static final String ERROR_CODE_MESSAGE_NOT_SENT_TO_BANK = "message_not_sent_to_bank";

	
	private static final String DEFAULT_SIGNUP_SUBJECT = "Nýskráning á eLykill.is";
	private static final String LOCALIZED_SIGNUP_SUBJECT_KEY = "openid.signup.email.subject.format";
	private static final String DEFAULT_SIGNUP_BODY = 
		"Góðan dag {0}.\n\r<br/> " +
		"Okkur er sönn ánægja að svara beiðni þinni um að stofna eLykil fyrir þig.  " +
		"Til þess að virkja eLykilinn þinn þarft þú að sýna fram á að rétt kennitala hafi verið gefin upp " +
		"og að hún eigi við þig. Til þess að gera það hefur þú valið að nota heimbankann þinn.  Ferlið er í tveimur skrefum. " +
		"Fyrst opnar þú rafrænt skjal sem þér hefur borist í heimabankann frá elykill.is.  Í því skjali er staðfestingarkóði sem þú " +
		"skráir inn á slóðinni <a href=\"http://www.elykill.is/pages/signup?confirm_id={1}\" >http://www.elykill.is/pages/signup?confirm_id={1}</a>." +
		"\n\r<br/>" +
		" ";
	private static final String LOCALIZED_SIGNUP_BODY_KEY = "openid.signup.email.body.format";
	private static final String DEFAULT_SIGNUP_SENDER = "eLykill.is";
	private static final String LOCALIZED_SIGNUP_SENDER_KEY = "openid.signup.email.sender.name";

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public String getConfirmID() {
		return confirmID;
	}

	public void setConfirmID(String confirmID) {
		this.confirmID = confirmID;
	}

	public String getIdentificationCode() {
		return identificationCode;
	}

	public void setIdentificationCode(String identificationCode) {
		this.identificationCode = identificationCode;
	}

	public String getFaceletPath(){
		if(faceletPath != null){
			return faceletPath;
		}
		
		IWContext iwc = IWContext.getCurrentInstance();
		String confirmID = iwc.getParameter(PARAM_CONFIRM_ID);
		if(confirmID!=null){
			this.setState(OpenIDSignUpBean.STATE_CONFIRM);
			this.setConfirmID(confirmID);
		}
		
		faceletPath = "server/signup/";
		int state = this.getState();
		
		switch (state) {
		case OpenIDSignUpBean.STATE_REQUEST:
			faceletPath += "request.xhtml";
			break;
			
		case OpenIDSignUpBean.STATE_REQUESTED:
			faceletPath += "requested.xhtml";
			break;
			
		case OpenIDSignUpBean.STATE_CONFIRM:
			faceletPath += "confirm.xhtml";
			break;
			
		case OpenIDSignUpBean.STATE_SIGNUP:
			faceletPath += "signup.xhtml";
			break;
			
		case OpenIDSignUpBean.STATE_FINISHED:
			faceletPath += "done.xhtml";
			break;
			
		case OpenIDSignUpBean.STATE_ERROR_OCCURRED:
			faceletPath += "error.xhtml";
			break;
			
		}
		return faceletPath;
	}
	
	
	
	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getPersonalID() {
		return personalID;
	}

	public void setPersonalID(String personalID) {
		this.personalID = personalID;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setEmailConfirmed(String emailConfirmed) {
		this.emailConfirmed = emailConfirmed;
	}

	public String getEmailConfirmed() {
		return emailConfirmed;
	}

	public void setSendSnailMail(boolean sendSnailMail) {
		this.sendSnailMail = sendSnailMail;
	}

	public boolean isSendSnailMail() {
		return sendSnailMail;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getMobileNumber() {
		return mobileNumber;
	}

	public void setMobileNumber(String mobileNumber) {
		this.mobileNumber = mobileNumber;
	}
	
	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmedPassword() {
		return confirmedPassword;
	}

	public void setConfirmedPassword(String confirmedPassword) {
		this.confirmedPassword = confirmedPassword;
	}


	public void setErrorMessage(String errorMessage, Object... arguments) {
		this.errorMessage = errorMessage;
		this.errorArguments = arguments;
	}

	public void setErrorMessage(String errorMessage) {
		Object[] o = null;
		this.setErrorMessage(errorMessage,o);
	}

	public String getErrorMessage() {
		IWContext iwc = IWContext.getCurrentInstance();
        IWMainApplication iwma = IWMainApplication.getIWMainApplication(iwc);
        IWResourceBundle iwrb = iwma.getBundle(getBundleIdentifier()).getResourceBundle(iwc);
        String summaryText = iwrb.getLocalizedAndFormattedString(errorMessage, errorMessage, errorArguments);
		return summaryText;
	}
	
	public String getBundleIdentifier() {
		return OpenIDConstants.IW_BUNDLE_IDENTIFIER;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}


	/* Actions begin*/
//
//	String TMP_ATTRIBUTE_CONFIRM_ID_MAP = "openid.signup.confirm_id.map";
//	String TMP_ATTRIBUTE_ID_CODE_MAP = "openid.signup.id_code.map";
//	String TMP_ATTRIBUTE_META_DATA = "openid.signup.meta.data";
	
	
	public void request(){
		IWContext iwc = IWContext.getCurrentInstance();

		String cID = StringHandler.getRandomString(22);
		setConfirmID(cID);
		String idCode = StringHandler.getRandomStringNonAmbiguous(8);
		setIdentificationCode(idCode);
		
//			
//		Map<String,String> confirmMap = (Map<String, String>) iwc.getSessionAttribute(TMP_ATTRIBUTE_CONFIRM_ID_MAP);
//		if(confirmMap == null){
//			confirmMap = new HashMap<String, String> ();
//			iwc.setSessionAttribute(TMP_ATTRIBUTE_CONFIRM_ID_MAP, confirmMap);
//		}
//		confirmMap.put(cID, getPersonalID());
//		
//		Map<String,String> idCodeMap = (Map<String, String>) iwc.getSessionAttribute(TMP_ATTRIBUTE_ID_CODE_MAP);
//		if(idCodeMap == null){
//			idCodeMap = new HashMap<String, String> ();
//			iwc.setSessionAttribute(TMP_ATTRIBUTE_ID_CODE_MAP, idCodeMap);
//		}
//		idCodeMap.put(cID, idCode);
//		
//		
//		HashMap<String, String[]> emailMap = (HashMap<String, String[]>) iwc.getSessionAttribute(TMP_ATTRIBUTE_META_DATA);
//		if(emailMap == null){
//			emailMap = new HashMap<String, String[]> ();
//			iwc.setSessionAttribute(TMP_ATTRIBUTE_META_DATA, emailMap);
//		}
//		String[] metaData = new String[2];
//		metaData[0]=getEmail();
//		metaData[1]=getLogin();
//		emailMap.put(cID, metaData);
		

		
		getOpenIDSignupDAO().createOpenIDSignupInfo(cID, idCode,getPersonalID(),getEmail(),getLogin());
		
		//TODO Check if user already exists and skip remote fetch if it does have fairly recent info in local db
		User user = null;
		try {
			user = getUserBusiness(iwc).getUser(getPersonalID());
		} catch (RemoteException e1) {
			e1.printStackTrace();
		} catch (FinderException e1) {
			//Do nothing
		}
		
		
		//TODO create user and tmp login entry
		//TODO remove old logins if user is making new request with a different login name
		
		if(user != null){
			setFullName(user.getName());
			Collection<Address> addresses = user.getAddresses();
			if(addresses != null && !addresses.isEmpty()){
				Address a = addresses.iterator().next();
				setAddress(a.getStreetAddress());
				setPostalCode(a.getPostalAddress());
				setCity(a.getCity());
			} else {
				//TODO send error or update from natreg
			}
			
			
		} else {
			boolean fetch = fetchByPersonalID();
			if(!fetch){
				setErrorMessage(ERREOR_MESSAGE_ID_NATREG_NO_DATA);
				setErrorCode(ERROR_CODE_NATREG_NO_DATA);
				setState(STATE_ERROR_OCCURRED);
				return;
			}
			
			try {
				user = createUser();
			} catch (RemoteException e) {
				e.printStackTrace();
				setErrorMessage(ERREOR_MESSAGE_ID_CREATE_USER_FAILED);
				setErrorCode(ERROR_CODE_CREATE_USER_FAILED);
				setState(STATE_ERROR_OCCURRED);
				return;
			} catch (CreateException e) {
				e.printStackTrace();
				setErrorMessage(ERREOR_MESSAGE_ID_CREATE_USER_FAILED);
				setErrorCode(ERROR_CODE_CREATE_USER_FAILED);
				setState(STATE_ERROR_OCCURRED);
				return;
			}
		}
		
		Name name = new Name(getFullName());
		String usrName = name.getFirstName();
		String middleName = name.getMiddleName();
		if(middleName != null && middleName.length()>0){
			usrName += " " + middleName;
		}
		
		Object[] arguments = new String[2];
		arguments[0] = usrName;
		arguments[1] = getConfirmID();
		
		boolean success = sendEmail(iwc, arguments);
		if(!success){
			//Error occurred
			return;
		}
		
		success = sendLetterToBank(iwc, arguments);
		if(!success){
			//Error occurred
			return;
		}
		
		setState(STATE_REQUESTED);
	}


	private boolean sendLetterToBank(IWContext iwc, Object[] arguments) {
		
//		try {
//			//TODO send to bank
//		} catch ( e) {
//			e.printStackTrace();
//			log.warning("Message could not be sent to bank:" + e.getMessage());
//			setErrorCode(ERROR_CODE_MESSAGE_NOT_SENT_TO_BANK);
//			setErrorMessage(ERREOR_MESSAGE_ID_MESSAGE_NOT_SENT_TO_BANK);
//			setState(STATE_ERROR_OCCURRED);
//			return false;
//		}
		
		return true;
	}
	
	private boolean sendEmail(IWContext iwc, Object[] arguments) {
		
		IWResourceBundle iwrb = iwc.getIWMainApplication().getBundle(getBundleIdentifier()).getResourceBundle(iwc);
		
		String subject = iwrb.getLocalizedAndFormattedString(LOCALIZED_SIGNUP_SUBJECT_KEY, DEFAULT_SIGNUP_SUBJECT, arguments);
		String body = iwrb.getLocalizedAndFormattedString(LOCALIZED_SIGNUP_BODY_KEY, DEFAULT_SIGNUP_BODY, arguments);
		String senderName = iwrb.getLocalizedString(LOCALIZED_SIGNUP_SENDER_KEY, DEFAULT_SIGNUP_SENDER);
		String fromAddress = iwc.getApplicationSettings().getProperty(MessagingSettings.PROP_MESSAGEBOX_FROM_ADDRESS,MessagingSettings.DEFAULT_MESSAGEBOX_FROM_ADDRESS);
		
		
		EmailMessage message = new EmailMessage(subject,body);
		message.setToAddress(getEmail());
		message.setSenderName(senderName);
		message.setFromAddress(fromAddress);
		message.setMailType(MimeTypeUtil.MIME_TYPE_HTML);
		try {
			message.send();
		} catch (MessagingException e) {
			e.printStackTrace();
			log.warning("Email could not be sent:" + e.getMessage());
			setErrorCode(ERROR_CODE_EMAIL_NOT_SENT);
			setErrorMessage(ERREOR_MESSAGE_ID_EMAIL_NOT_SENT);
			setState(STATE_ERROR_OCCURRED);
			return false;
		}
		return true;
	}
	
	public void toConfirm(){
		
		setState(STATE_CONFIRM);
	}
	
	public void confirm(){
		
		OpenIdSignupInfo info = getOpenIDSignupDAO().getOpenIdSignupInfo(getConfirmID(), getIdentificationCode());
		if(info != null){
			String ssn = info.getPersonalID();
			setPersonalID(ssn);
			setEmail(info.getEmail());
			setLogin(info.getLoginName());
			boolean fetch = fetchByPersonalID();
			if(!fetch){
				setErrorMessage(ERREOR_MESSAGE_ID_NATREG_NO_DATA);
				setErrorCode(ERROR_CODE_NATREG_NO_DATA);
				setState(STATE_ERROR_OCCURRED);
				return;
			}			
			setState(STATE_SIGNUP);
			return;
		}
		
		
//		IWContext iwc = IWContext.getCurrentInstance();
//		
//		Map<String,String> confirmMap = (Map<String, String>) iwc.getSessionAttribute(TMP_ATTRIBUTE_CONFIRM_ID_MAP);
//		Map<String,String> idCodeMap = (Map<String, String>) iwc.getSessionAttribute(TMP_ATTRIBUTE_ID_CODE_MAP);
//		Map<String,String[]> emailMap = (Map<String, String[]>) iwc.getSessionAttribute(TMP_ATTRIBUTE_META_DATA);
//		if(confirmMap != null && idCodeMap != null){
//			String idCode = idCodeMap.get(getConfirmID());
//			if(idCode != null && idCode.equals(getIdentificationCode())){
//				String ssn = confirmMap.get(getConfirmID());
//				setPersonalID(ssn);
//				boolean fetch = fetchByPersonalID();
//				if(fetch){
//					setErrorMessage(MESSAGE_ID_NATREG_NO_DATA);
//					setErrorCode(ERROR_CODE_NATREG_NO_DATA);
//					setState(STATE_ERROR_OCCURRED);
//				}
//				
//				String[] e = emailMap.get(getConfirmID());
//				setEmail(e[0]);
//				setLogin(e[1]);
//				
//				setState(STATE_SIGNUP);
//				return;
//			}
//		}
		setState(STATE_REQUEST);
	}
	
	private boolean fetchByPersonalID(){
		if(getPersonalID() != null){
			FerliClient c = getNationalPopulationRegistryClient();
			UserHolder user = c.getUser(personalID);
			if(user != null){
				setFullName(user.getName());
				setAddress(user.getAddress());
				setPostalCode(user.getPostalCode());
				setCity(user.getPostalAddress());
				return true;
			}
		}
		return false;
	}
	
	public void activate(){
//		createUser();
		//TODO Add phone number and other user info
		//TODO Enable login and set password
		setState(STATE_FINISHED);
	}	
	
	private User createUser() throws RemoteException, CreateException {
		IWContext iwc = IWContext.getCurrentInstance();
		String ssn = getPersonalID();
		IWTimestamp dateOfBirth = ssn != null ? new IWTimestamp(SocialSecurityNumber.getDateFromSocialSecurityNumber(ssn)) : null;
		
		String usrFullName = getFullName();
		Name name = new Name(usrFullName);
		String displayName = usrFullName;
		String description = null;
		Integer genderInt = null;
		String login = getLogin();
		String password = "disabledAccount";  //Password cannot be null or empty string
		Boolean accountEnabled = Boolean.FALSE;
		int daysOfValidity = 30;
		Boolean passwordExpires = Boolean.FALSE;
		Boolean userAllowedToChangePassw = Boolean.TRUE;
		Boolean changeNextTime = Boolean.FALSE;
		String encryptionType = null;
		
		User usr = getUserBusiness(iwc).createUserWithLogin(
				name.getFirstName(), 
				name.getMiddleName(), 
				name.getLastName(), 
				ssn, 
				displayName, 
				description, 
				genderInt, 
				dateOfBirth, 
				null, 
				login, 
				password, 
				accountEnabled, 
				IWTimestamp.RightNow(), 
				daysOfValidity, 
				passwordExpires, 
				userAllowedToChangePassw, 
				changeNextTime, 
				encryptionType, 
				usrFullName);
		
		//TODO add address to user
		
		return usr;
	}
	
	public void cancel(){
		setState(STATE_REQUEST);
	}

	
	/* Actions end */

	private FerliClient getNationalPopulationRegistryClient() {
		if (natReg == null) {
			ELUtil.getInstance().autowire(this);
		}
		return natReg;
	}
	
	private UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
	
	private OpenIDSignupDAO getOpenIDSignupDAO() {
		if (signupDAO == null) {
			ELUtil.getInstance().autowire(this);
		}
		return signupDAO;
	}
	
}
