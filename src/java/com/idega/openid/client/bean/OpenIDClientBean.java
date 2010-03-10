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
		parameters.add(new Parameter(parameter, value));
	}

	public Collection<Parameter> getParameters() {
		return parameters;
	}
	
	public class Parameter {

		String parameter;
		String value;

		public Parameter(String parameter, String value) {
			this.parameter = parameter;
			this.value = value;
		}

		public String getParameter() {
			return parameter;
		}

		public String getValue() {
			return value;
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
	public void setAction(String action) {
		this.action = action;
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
	public void setOutput(String output) {
		this.output = output;
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
	public void setStyleClass(String styleClass) {
		this.styleClass = styleClass;
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
	public void setLocaleStyle(String localeStyle) {
		this.localeStyle = localeStyle;
	}
}