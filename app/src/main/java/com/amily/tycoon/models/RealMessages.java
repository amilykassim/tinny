package com.amily.tycoon.models;

import android.os.Parcel;
import android.os.Parcelable;

public class RealMessages implements Parcelable {

    private String m; // messages
    private String t; // (to) the person to receieve the message
    private String f; // (from) the person who sent the message
    private String sn; // seen
    private String mid; // message id
    private long tm; // the time sent

    public RealMessages() {

    }

    public RealMessages(String m, String t, String f, String sn, String mid, long tm) {
        this.m = m;
        this.t = t;
        this.f = f;
        this.sn = sn;
        this.mid = mid;
        this.tm = tm;
    }

    protected RealMessages(Parcel in) {
        m = in.readString();
        t = in.readString();
        f = in.readString();
        sn = in.readString();
        mid = in.readString();
        tm = in.readLong();
    }

    public static final Creator<RealMessages> CREATOR = new Creator<RealMessages>() {
        @Override
        public RealMessages createFromParcel(Parcel in) {
            return new RealMessages(in);
        }

        @Override
        public RealMessages[] newArray(int size) {
            return new RealMessages[size];
        }
    };

    public String getM() {
        return m;
    }

    public void setM(String m) {
        this.m = m;
    }

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public String getF() {
        return f;
    }

    public void setF(String f) {
        this.f = f;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public long getTm() {
        return tm;
    }

    public void setTm(long tm) {
        this.tm = tm;
    }

    @Override
    public String toString() {
        return "RealMessages{" +
                "m='" + m + '\'' +
                ", t='" + t + '\'' +
                ", f='" + f + '\'' +
                ", sn='" + sn + '\'' +
                ", mid='" + mid + '\'' +
                ", tm=" + tm +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(m);
        dest.writeString(t);
        dest.writeString(f);
        dest.writeString(sn);
        dest.writeString(mid);
        dest.writeLong(tm);
    }
}
