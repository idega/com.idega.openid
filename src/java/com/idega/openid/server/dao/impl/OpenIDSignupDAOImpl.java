package com.idega.openid.server.dao.impl;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.openid.server.dao.OpenIDSignupDAO;
import com.idega.openid.server.data.OpenIdSignupInfo;

@Repository("openIDSignupDAO")
@Transactional(readOnly = true)
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class OpenIDSignupDAOImpl extends GenericDaoImpl implements OpenIDSignupDAO {
	
	@Transactional(readOnly = false)
	public void createOpenIDSignupInfo(String confirmAttribute, String confirmCode, String personalID, String email, String loginName) {
		OpenIdSignupInfo info = new OpenIdSignupInfo();
		info.setConfirmAttribute(confirmAttribute);
		info.setConfirmCode(confirmCode);
		info.setPersonalID(personalID);
		info.setEmail(email);
		info.setLoginName(loginName);
		info.setAddedWhen(new Date());
		info.setValid(true);
		
		//Invalidate all already existing valid entries for this personal ID
		Param p1 = new Param("personalID", personalID);
		List<OpenIdSignupInfo> l = getResultList(OpenIdSignupInfo.QUERY_FIND_VALID_BY_PERSONAL_ID, OpenIdSignupInfo.class, p1);
		// Should normally be max 1 iteration
		if(l!=null && l.size()>0){
			for(OpenIdSignupInfo inf : l){ 
				inf.setValid(false);
				merge(inf);
			}	
		}
		    
		getEntityManager().persist(info);
		
	}
	
	@Override
	public OpenIdSignupInfo getOpenIdSignupInfo(String confirmAttribute, String confirmCode) {
		Param p1 = new Param("confirmAttribute", confirmAttribute);
		Param p2 = new Param("confirmCode", confirmCode);
		
		return getSingleResult(OpenIdSignupInfo.QUERY_FIND_VALID_BY_CONFIRM_ATTRIBUTE_AND_CODE, OpenIdSignupInfo.class, p1, p2);
	}

}
