package com.amily.tycoon.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Views implements Parcelable {

    private String uid; // user id;
    private long tm; // date created;

    public Views() {

    }

    public Views(String uid, long tm) {
        this.uid = uid;
        this.tm = tm;
    }

    protected Views(Parcel in) {
        uid = in.readString();
        tm = in.readLong();
    }

    public static final Creator<Views> CREATOR = new Creator<Views>() {
        @Override
        public Views createFromParcel(Parcel in) {
            return new Views(in);
        }

        @Override
        public Views[] newArray(int size) {
            return new Views[size];
        }
    };

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

    @Override
    public String toString() {
        return "Views{" +
                "uid='" + uid + '\'' +
                ", tm=" + tm +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeLong(tm);
    }
}
