package com.oltranz.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "request")
public class RequestModel {

    private String msisdn;

    private String cellId;

    private String imsi;

    private String lac;

    private int location;

    private String msc;

    // private FreeFlow freeflow;

    private int newRequest;

    private String sessionId;

    private String subscriberInput;

    private String transactionId;

    private String dateFormat;

    public RequestModel() {

    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getCellId() {
        return cellId;
    }

    public void setCellId(String cellId) {
        this.cellId = cellId;
    }

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public String getLac() {
        return lac;
    }

    public void setLac(String lac) {
        this.lac = lac;
    }

    public int getLocation() {
        return location;
    }

    public void setLocation(int location) {
        this.location = location;
    }

    public String getMsc() {
        return msc;
    }

    public void setMsc(String msc) {
        this.msc = msc;
    }

    public int getNewRequest() {
        return newRequest;
    }

    public void setNewRequest(int newRequest) {
        this.newRequest = newRequest;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSubscriberInput() {
        return subscriberInput;
    }

    public void setSubscriberInput(String subscriberInput) {
        this.subscriberInput = subscriberInput;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public GateWayModel convertToGateWayModel() {
        return new GateWayModel(sessionId, msisdn, newRequest, dateFormat, transactionId);
    }

    @Override
    public String toString() {
        return "RequestModel{" + "msisdn=" + msisdn + ", cellId=" + cellId + ", imsi=" + imsi + ", lac=" + lac
                + ", location=" + location + ", msc=" + msc + ", newRequest=" + newRequest
                + ", sessionId=" + sessionId + ", subscriberInput=" + subscriberInput + ", transactionId="
                + transactionId + ", dateFormat=" + dateFormat + '}';
    }
}
