package com.amily.tycoon.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class Comment implements Parcelable{

    private String c;
    private String uid;
    private long tm;
    private String cid;

    public Comment() {

    }

    public Comment(String c, String uid, long tm, String cid) {
        this.c = c;
        this.uid = uid;
        this.tm = tm;
        this.cid = cid;
    }

    protected Comment(Parcel in) {
        c = in.readString();
        uid = in.readString();
        tm = in.readLong();
        cid = in.readString();
    }

    public static final Creator<Comment> CREATOR = new Creator<Comment>() {
        @Override
        public Comment createFromParcel(Parcel in) {
            return new Comment(in);
        }

        @Override
        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };

    public String getC() {
        return c;
    }

    public void setC(String c) {
        this.c = c;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getTm() {
        return tm;
    }

    public void setTm(long tm) {
        this.tm = tm;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "c='" + c + '\'' +
                ", uid='" + uid + '\'' +
                ", tm=" + tm +
                ", cid='" + cid + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(c);
        dest.writeString(uid);
        dest.writeLong(tm);
        dest.writeString(cid);
    }
}