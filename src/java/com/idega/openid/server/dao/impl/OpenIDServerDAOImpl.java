package com.idega.openid.server.dao.impl;

import java.util.Date;
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
import com.idega.openid.server.data.AuthenticationLog;
import com.idega.openid.server.data.AuthorizedAttribute;
import com.idega.openid.server.data.ExchangeAttribute;
import com.idega.util.IWTimestamp;

@Repository("openIDServerDAO")
@Transactional(readOnly = true)
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class OpenIDServerDAOImpl extends GenericDaoImpl implements OpenIDServerDAO {

	@Transactional(readOnly = false)
	public void createAuthorizedAttribute(String userUUID, String realm, ExchangeAttribute attribute) {
		AuthorizedAttribute authAttr = new AuthorizedAttribute();
		authAttr.setUserUUID(userUUID);
		authAttr.setRealm(realm);
		authAttr.setExchangeAttribute(attribute);
		authAttr.setIsAllowed(true);
		authAttr.setAddedWhen(IWTimestamp.getTimestampRightNow());
		
		getEntityManager().persist(authAttr);
	}
	
	public List<AuthorizedAttribute> getAuthorizedAttributes(String userUUID, String realm) {
		Param p1 = new Param("userUUID", userUUID);
		Param p2 = new Param("realm", realm);
		
		return getResultList("authAttr.findByUserAndRealm", AuthorizedAttribute.class, p1, p2);
	}
	
	public AuthorizedAttribute getAuthorizedAttributes(String userUUID, String realm, ExchangeAttribute attr) {
		Param p1 = new Param("userUUID", userUUID);
		Param p2 = new Param("realm", realm);
		Param p3 = new Param("exchangeAttribute",attr);
		
		return getSingleResult("authAttr.findByUserAndRealmAndExchangeAttribute", AuthorizedAttribute.class, p1, p2, p3);
	}

	@Transactional(readOnly = false)
	public void createLogEntry(String userUUID, String realm, String exchangedAttributes) {
		AuthenticationLog log = new AuthenticationLog();
		log.setUserUUID(userUUID);
		log.setRealm(realm);
		log.setTimestamp(IWTimestamp.getTimestampRightNow());
		log.setStatus(OpenIDConstants.STATUS_SUCCESS);
		log.setExchangedAttributes(exchangedAttributes);
		
		getEntityManager().persist(log);
	}
	
	@SuppressWarnings("unchecked")
	public List<ExchangeAttribute> getAllExchangeAttributes() {
		Query q = createNamedQuery("exchangeAttr.findAll");
		List<ExchangeAttribute> attr = q.getResultList();
		return attr;
	}
	
	@Transactional(readOnly = false)
	public void createExchangeAttribute(String name, String type) {
		ExchangeAttribute attr = getExchangeAttribute(name, type);
		if (attr == null) {
			attr = new ExchangeAttribute();
			attr.setName(name);
			attr.setType(type);
			attr.setAddedWhen(new Date());
			getEntityManager().persist(attr);
		}
	}

	@Override
	public ExchangeAttribute getExchangeAttribute(String name) {
		Param p1 = new Param("name", name);
		
		return getSingleResult("exchangeAttr.findByName", ExchangeAttribute.class, p1);
	}
	
	@Override
	public ExchangeAttribute getExchangeAttribute(String name, String type) {
		Param p1 = new Param("name", name);
		Param p2 = new Param("type", type);
		
		return getSingleResult("exchangeAttr.findByNameOrType", ExchangeAttribute.class, p1, p2);
	}
	
	public void saveAuthorizedAttribute(AuthorizedAttribute attr){
		if (attr.isNotYetStored()) {
			attr.setAddedWhen(new Date());
		}
		getEntityManager().persist(attr);
	}
}