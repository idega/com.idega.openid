package com.idega.openid.server.dao;

import java.util.List;

import com.idega.core.persistence.GenericDao;
import com.idega.openid.server.data.AuthorizedAttribute;
import com.idega.openid.server.data.ExchangeAttribute;

public interface OpenIDServerDAO extends GenericDao {

	public void createAuthorizedAttribute(String userUUID, String realm, ExchangeAttribute attribute);
	
	public List<AuthorizedAttribute> getAuthorizedAttributes(String userUUID, String realm);

	public AuthorizedAttribute getAuthorizedAttributes(String userUUID, String realm, ExchangeAttribute attr);

	public void createLogEntry(String userUUID, String realm, String exchangedAttributes);
	
	public List<ExchangeAttribute> getAllExchangeAttributes();

	public void createExchangeAttribute(String name, String type);
	
	public ExchangeAttribute getExchangeAttribute(String name, String type);
	
	public void saveAuthorizedAttribute(AuthorizedAttribute attr);
	
}