package com.idega.openid.server.presentation;

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.faces.context.FacesContext;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.openid.OpenIDConstants;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.user.business.UserBusiness;
import com.idega.util.IWTimestamp;
import com.idega.util.PersonalIDFormatter;
import com.idega.util.text.Name;
import com.idega.util.text.SocialSecurityNumber;

public class OpenIDRegistration extends IWBaseComponent {

	private static final String PARAMETER_NAME = "prm_name";
	private static final String PARAMETER_PERSONAL_ID = "prm_personal_id";
	
	public String getBundleIdentifier() {
		return OpenIDConstants.IW_BUNDLE_IDENTIFIER;
	}
	
	@Override
	public void initializeComponent(FacesContext context) {
		IWContext iwc = IWContext.getIWContext(context);
		
		showForm(iwc);
		save(iwc);
	}
	
	private void showForm(IWContext iwc) {
		
	}
	
	private void save(IWContext iwc) {
		String fullName = iwc.getParameter(PARAMETER_NAME);
		String personalID = PersonalIDFormatter.stripForDatabaseSearch(iwc.getParameter(PARAMETER_PERSONAL_ID));
		IWTimestamp dateOfBirth = personalID != null ? new IWTimestamp(SocialSecurityNumber.getDateFromSocialSecurityNumber(personalID)) : null;
		
		Name name = new Name(fullName);
		
		try {
			getUserBusiness(iwc).createUser(name.getFirstName(), name.getMiddleName(), name.getLastName(), fullName, personalID, null, null, dateOfBirth, null, fullName);
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
		catch (CreateException e) {
			e.printStackTrace();
		}
	}
	
	private UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return (UserBusiness) IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
}