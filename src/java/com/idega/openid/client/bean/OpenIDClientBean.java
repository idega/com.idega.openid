package com.idega.openid.client.bean;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("openIDClientBean")
@Scope("request")
public class OpenIDClientBean {

	private String action;
	private String output;
	private Collection<Parameter> parameters = new ArrayList<Parameter>();

	private String styleClass = null;
	private String localeStyle;

	public void addParameter(String parameter, String value) {
		this.parameters.add(new Parameter(parameter, value));
	}

	public Collection<Parameter> getParameters() {
		return this.parameters;
	}
	
	public class Parameter {

		String parameter;
		String value;

		public Parameter(String param, String val) {
			this.parameter = param;
			this.value = val;
		}

		public String getParameter() {
			return this.parameter;
		}

		public String getValue() {
			return this.value;
		}
	}
	
	/**
	 * @return the action
	 */
	public String getAction() {
		return this.action;
	}

	/**
	 * @param action
	 *          the action to set
	 */
	public void setAction(String theAction) {
		this.action = theAction;
	}

	/**
	 * @return the output
	 */
	public String getOutput() {
		return this.output;
	}

	/**
	 * @param output
	 *          the output to set
	 */
	public void setOutput(String textOutput) {
		this.output = textOutput;
	}

	/**
	 * @return the styleClass
	 */
	public String getStyleClass() {
		return this.styleClass;
	}

	/**
	 * @param styleClass
	 *          the styleClass to set
	 */
	public void setStyleClass(String theStyleClass) {
		this.styleClass = theStyleClass;
	}

	/**
	 * @return the localeStyle
	 */
	public String getLocaleStyle() {
		return this.localeStyle;
	}

	/**
	 * @param localeStyle
	 *          the localeStyle to set
	 */
	public void setLocaleStyle(String localeStyleClass) {
		this.localeStyle = localeStyleClass;
	}
}