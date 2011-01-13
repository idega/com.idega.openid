package com.idega.openid.server.dao.impl;

import java.util.Date;
import java.util.List;

import javax.ejb.CreateException;
import javax.persistence.EntityTransaction;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.accesscontrol.data.LoginInfo;
import com.idega.core.accesscontrol.data.LoginInfoHome;
import com.idega.core.accesscontrol.data.LoginTable;
import com.idega.core.accesscontrol.data.LoginTableHome;
import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.data.IDOLookup;
import com.idega.openid.server.dao.OpenIDSignupDAO;
import com.idega.openid.server.data.OpenIdSignupInfo;
import com.idega.util.Encrypter;

@Repository("openIDSignupDAO")
@Transactional(readOnly = true)
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class OpenIDSignupDAOImpl extends GenericDaoImpl implements OpenIDSignupDAO {
	
	protected static final String USER_CREATION_TYPE = "OPENID_SIGNUP";
	
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

	
	
	@Override
	public void activateLogin(String login, String newPassword) throws CreateException {
//		EntityTransaction tx = getEntityManager().getTransaction();
		try {
//			tx.begin();

			LoginTable loginTable = ((LoginTableHome) IDOLookup
					.getHome(LoginTable.class)).findByLogin(login);
			
			int bankCount = loginTable.getBankCount();

			// encrypte new password
			String encryptedPassword = Encrypter.encryptOneWay(newPassword);
			// store new password
			loginTable.setUserPassword(encryptedPassword, newPassword);
			loginTable.setBankCount(bankCount + 1);
			loginTable.store();

			LoginInfo loginInfo = ((LoginInfoHome) IDOLookup
					.getHome(LoginInfo.class)).findByPrimaryKey(loginTable
					.getPrimaryKey());
			loginInfo.setFailedAttemptCount(0);
			loginInfo.setAccessClosed(false);
			loginInfo.setAccountEnabled(true);
			loginInfo.setCreationType(USER_CREATION_TYPE);
			loginInfo.store();

//			tx.commit();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			// e.printStackTrace();
//			if (tx != null) {
//				tx.rollback();
//			}
			throw new CreateException(
					"There was an error setting the password. Message was: "
							+ e.getMessage());
		}
	}

}
