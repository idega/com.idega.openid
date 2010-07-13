package com.idega.openid.server.dao.impl;

import java.util.List;

import javax.persistence.Query;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.persistence.Param;
import com.idega.core.persistence.impl.GenericDaoImpl;
import com.idega.openid.OpenIDConstants;
import com.idega.openid.server.dao.OpenIDServerDAO;
import com.idega.openid.server.data.AuthenticatedRealm;
import com.idega.openid.server.data.AuthenticationLog;
import com.idega.util.IWTimestamp;

@Repository("openIDServerDAO")
@Transactional(readOnly = true)
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class OpenIDServerDAOImpl extends GenericDaoImpl implements OpenIDServerDAO {

	@Transactional(readOnly = false)
	public void createAuthenticatedRealm(String userUUID, String realm) {
		AuthenticatedRealm authRealm = new AuthenticatedRealm();
		authRealm.setUserUUID(userUUID);
		authRealm.setRealm(realm);
		authRealm.setAddedWhen(IWTimestamp.getTimestampRightNow());
		
		getEntityManager().persist(authRealm);
	}
	
	public List<AuthenticatedRealm> getAuthenticatedRealmsByUser(String userUUID) {
		Query q = createNamedQuery("authRealm.findAllByUser");
		q.setParameter("userUUID", userUUID);
		List<AuthenticatedRealm> names = q.getResultList();
		return names;
	}
	
	public AuthenticatedRealm getAuthenticatedRealm(String userUUID, String realm) {
		Param p1 = new Param("userUUID", userUUID);
		Param p2 = new Param("realm", realm);
		
		return getSingleResult("authRealm.findByUserAndRealm", AuthenticatedRealm.class, p1, p2);
	}

	@Transactional(readOnly = false)
	public void createLogEntry(String userUUID, String realm) {
		AuthenticationLog log = new AuthenticationLog();
		log.setUserUUID(userUUID);
		log.setRealm(realm);
		log.setTimestamp(IWTimestamp.getTimestampRightNow());
		log.setStatus(OpenIDConstants.STATUS_SUCCESS);
		
		getEntityManager().persist(log);
	}
}