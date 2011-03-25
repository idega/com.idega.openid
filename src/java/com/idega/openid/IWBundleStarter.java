package com.idega.openid;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;
import com.idega.openid.server.dao.OpenIDServerDAO;
import com.idega.openid.server.filter.OpenIDPagesRedirectHandler;
import com.idega.servlet.filter.IWBundleResourceFilter;
import com.idega.servlet.filter.util.PagesRedirectHandler;
import com.idega.util.expression.ELUtil;

public class IWBundleStarter implements IWBundleStartable {
	
	@Autowired
	private OpenIDServerDAO dao;
	
	public void start(IWBundle starterBundle) {
		String handlerClass = starterBundle.getApplication().getSettings().getProperty(PagesRedirectHandler.ATTRIBUTE_PAGES_REDIRECT_HANDLER_CLASS);
		if (handlerClass == null) {
			starterBundle.getApplication().getSettings().setProperty(PagesRedirectHandler.ATTRIBUTE_PAGES_REDIRECT_HANDLER_CLASS, OpenIDPagesRedirectHandler.class.getName());
		}
		
		IWBundleResourceFilter.copyAllFilesFromJarDirectory(starterBundle.getApplication(), starterBundle, "/facelets/");
		IWBundleResourceFilter.copyAllFilesFromJarDirectory(starterBundle.getApplication(), starterBundle, "/resources/");
		
		getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_EMAIL, OpenIDConstants.ATTRIBUTE_TYPE_EMAIL);
		getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_PERSONAL_ID, OpenIDConstants.ATTRIBUTE_TYPE_PERSONAL_ID);
		getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_NICKNAME, OpenIDConstants.ATTRIBUTE_TYPE_FRIENDLY_NAME);
		getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_FULL_NAME, OpenIDConstants.ATTRIBUTE_TYPE_FULL_NAME);
		getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_DATE_OF_BIRTH, OpenIDConstants.ATTRIBUTE_TYPE_DATE_OF_BIRTH);
		getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_GENDER, OpenIDConstants.ATTRIBUTE_TYPE_GENDER);
		getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_POSTCODE, OpenIDConstants.ATTRIBUTE_TYPE_POSTAL_CODE);
		getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_COUNTRY, OpenIDConstants.ATTRIBUTE_TYPE_COUNTRY);
		getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_LANGUAGE, OpenIDConstants.ATTRIBUTE_TYPE_LANGUAGE);
		getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_TIMEZONE, OpenIDConstants.ATTRIBUTE_TYPE_TIMEZONE);
	}

	public void stop(IWBundle starterBundle) {
		//No action...
	}
	
	private OpenIDServerDAO getDAO() {
		if (dao == null) {
			ELUtil.getInstance().autowire(this);
		}
		
		return dao;
	}
}