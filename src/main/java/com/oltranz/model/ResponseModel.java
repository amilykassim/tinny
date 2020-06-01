package com.oltranz.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "response")
public class ResponseModel {

	@JacksonXmlProperty(localName = "MSISDN")
	private String msisdn;
	@JacksonXmlProperty(localName = "APPLICATIONRESPONSE")
	private String applicationResponse;
	@JacksonXmlProperty(localName = "APPDRIVENMENUCODE")
	private String appDrivenMenuCode;

	public ResponseModel() {
		super();
		// TODO Auto-generated constructor stub
	}

	public ResponseModel(String msisdn, String applicationResponse, String appDrivenMenuCode) {
		super();
		this.msisdn = msisdn;
		this.applicationResponse = applicationResponse;
		this.appDrivenMenuCode = appDrivenMenuCode;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public String getApplicationResponse() {
		return applicationResponse;
	}

	public void setApplicationResponse(String applicationResponse) {
		this.applicationResponse = applicationResponse;
	}

	public String getAppDrivenMenuCode() {
		return appDrivenMenuCode;
	}

	public void setAppDrivenMenuCode(String appDrivenMenuCode) {
		this.appDrivenMenuCode = appDrivenMenuCode;
	}

	@Override
    public String toString() {
        return "RequestModel{" + "msisdn=" + msisdn + ", applicationResponse=" + applicationResponse+ ", appDrivenMenuCode=" + appDrivenMenuCode + '}';
    }

}