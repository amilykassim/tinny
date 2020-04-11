package com.amily.tycoon.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class User implements Parcelable{

    private String unm; // username
    private String e; // email
    private String uid; // user id
    private String psw; // password

    public User() {

    }

    public User(String unm, String e, String uid, String psw) {
        this.unm = unm;
        this.e = e;
        this.uid = uid;
        this.psw = psw;
    }

    protected User(Parcel in) {
        unm = in.readString();
        e = in.readString();
        uid = in.readString();
        psw = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(unm);
        dest.writeString(e);
        dest.writeString(uid);
        dest.writeString(psw);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getUnm() {
        return unm;
    }

    public void setUnm(String unm) {
        this.unm = unm;
    }

    public String getE() {
        return e;
    }

    public void setE(String e) {
        this.e = e;
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

    @Override
    public String toString() {
        return "User{" +
                "unm='" + unm + '\'' +
                ", e='" + e + '\'' +
                ", uid='" + uid + '\'' +
                ", psw='" + psw + '\'' +
                '}';
    }
}
