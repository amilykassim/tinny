/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oltranz.model;

import java.io.Serializable;

/**
 *
 * @author aimable
 */
public class GateWayModel implements Serializable {

    private String sessionId;

    private String phoneNumber;

    private int newRequest;

    private String requestTime;

    public GateWayModel() {
    }

    public GateWayModel(String sessionId, String phoneNumber, int newRequest, String requestTime,
            String transactionId) {
        this.sessionId = sessionId;
        this.phoneNumber = phoneNumber;
        this.newRequest = newRequest;
        this.requestTime = requestTime;

    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getphoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getNewRequest() {
        return newRequest;
    }

    public void setNewRequest(int newRequest) {
        this.newRequest = newRequest;
    }

    public String getrequestTime() {
        return requestTime;
    }

    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }

    @Override
    public String toString() {
        return "GateWayModel{" + "sessionId=" + sessionId + ", phoneNumber=" + phoneNumber + ", newRequest="
                + newRequest + ", requestTime=" + requestTime + '}';
    }

}
