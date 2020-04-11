package com.amily.tycoon.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Notification implements Parcelable {

    private String uid; // the user id of the one whom we sent the notification
    private String nid; // notification id
    private String f; // from (who send the notification)
    private String t; // type


    public Notification() {

    }

    public Notification(String uid, String nid, String f, String t) {
        this.uid = uid;
        this.nid = nid;
        this.f = f;
        this.t = t;
    }

    protected Notification(Parcel in) {
        uid = in.readString();
        nid = in.readString();
        f = in.readString();
        t = in.readString();
    }

    public static final Creator<Notification> CREATOR = new Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel in) {
            return new Notification(in);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNid() {
        return nid;
    }

    public void setNid(String nid) {
        this.nid = nid;
    }

    public String getF() {
        return f;
    }

    public void setF(String f) {
        this.f = f;
    }

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }


    @Override
    public String toString() {
        return "Notification{" +
                "uid='" + uid + '\'' +
                ", nid='" + nid + '\'' +
                ", f='" + f + '\'' +
                ", t='" + t + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(nid);
        dest.writeString(f);
        dest.writeString(t);
    }
}
