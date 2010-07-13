package com.idega.openid.server.bean;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("openIDServerBean")
@Scope("request")
public class OpenIDServerBean {

	private String username;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}