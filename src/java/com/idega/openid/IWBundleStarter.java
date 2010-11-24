package com.idega.openid;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;
import com.idega.openid.server.dao.OpenIDServerDAO;
import com.idega.openid.server.data.ExchangeAttribute;
import com.idega.servlet.filter.IWBundleResourceFilter;
import com.idega.util.expression.ELUtil;

public class IWBundleStarter implements IWBundleStartable {
	
	@Autowired
	private OpenIDServerDAO dao;
	
	public void start(IWBundle starterBundle) {
		IWBundleResourceFilter.copyAllFilesFromJarDirectory(starterBundle.getApplication(), starterBundle, "/facelets/");
		IWBundleResourceFilter.copyAllFilesFromJarDirectory(starterBundle.getApplication(), starterBundle, "/resources/");
		
		List<ExchangeAttribute> allAttr = getDAO().getAllExchangeAttributes();
		if(allAttr.isEmpty()){
			getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_EMAIL, OpenIDConstants.ATTRIBUTE_TYPE_EMAIL);
			getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_PERSONAL_ID, OpenIDConstants.ATTRIBUTE_TYPE_PERSONAL_ID);
			getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_FULL_NAME, OpenIDConstants.ATTRIBUTE_TYPE_FULL_NAME);
			getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_DATE_OF_BIRTH, OpenIDConstants.ATTRIBUTE_TYPE_DATE_OF_BIRTH);
			getDAO().createExchangeAttribute(OpenIDConstants.ATTRIBUTE_ALIAS_GENDER, OpenIDConstants.ATTRIBUTE_TYPE_GENDER);
		}
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