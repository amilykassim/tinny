package com.amily.tycoon.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class UserAccountSettings implements Parcelable {

    private String d; // description
    private String dnm; // display name
    private String unm; // username
    private String pp; // profile photo
    private String uid; // user id
    private String psw; // password
    private String ct; // courses taken

    public UserAccountSettings() {

    }

    public UserAccountSettings(String d, String dnm, String unm, String pp, String uid, String psw, String ct) {
        this.d = d;
        this.dnm = dnm;
        this.unm = unm;
        this.pp = pp;
        this.uid = uid;
        this.psw = psw;
        this.ct = ct;
    }

    protected UserAccountSettings(Parcel in) {
        d = in.readString();
        dnm = in.readString();
        unm = in.readString();
        pp = in.readString();
        uid = in.readString();
        psw = in.readString();
        ct = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(d);
        dest.writeString(dnm);
        dest.writeString(unm);
        dest.writeString(pp);
        dest.writeString(uid);
        dest.writeString(psw);
        dest.writeString(ct);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserAccountSettings> CREATOR = new Creator<UserAccountSettings>() {
        @Override
        public UserAccountSettings createFromParcel(Parcel in) {
            return new UserAccountSettings(in);
        }

        @Override
        public UserAccountSettings[] newArray(int size) {
            return new UserAccountSettings[size];
        }
    };

    public String getD() {
        return d;
    }

    public void setD(String d) {
        this.d = d;
    }

    public String getDnm() {
        return dnm;
    }

    public void setDnm(String dnm) {
        this.dnm = dnm;
    }

    public String getUnm() {
        return unm;
    }

    public void setUnm(String unm) {
        this.unm = unm;
    }

    public String getPp() {
        return pp;
    }

    public void setPp(String pp) {
        this.pp = pp;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPsw() {
        return psw;
    }

    public void setPsw(String psw) {
        this.psw = psw;
    }

    public String getCt() {
        return ct;
    }

    public void setCt(String ct) {
        this.ct = ct;
    }

    @Override
    public String toString() {
        return "UserAccountSettings{" +
                "d='" + d + '\'' +
                ", dnm='" + dnm + '\'' +
                ", unm='" + unm + '\'' +
                ", pp='" + pp + '\'' +
                ", uid='" + uid + '\'' +
                ", psw='" + psw + '\'' +
                ", ct='" + ct + '\'' +
                '}';
    }
}
