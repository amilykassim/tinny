package com.amily.tycoon.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class TwoPartyMessageIDs implements Parcelable{

    private String sid; // sender id
    private String rid; // receiver id;
    private long tm; // time sent;

    public TwoPartyMessageIDs() {

    }

    public TwoPartyMessageIDs(String sid, String rid, long tm) {
        this.sid = sid;
        this.rid = rid;
        this.tm = tm;
    }

    protected TwoPartyMessageIDs(Parcel in) {
        sid = in.readString();
        rid = in.readString();
        tm = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sid);
        dest.writeString(rid);
        dest.writeLong(tm);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<TwoPartyMessageIDs> CREATOR = new Creator<TwoPartyMessageIDs>() {
        @Override
        public TwoPartyMessageIDs createFromParcel(Parcel in) {
            return new TwoPartyMessageIDs(in);
        }

        @Override
        public TwoPartyMessageIDs[] newArray(int size) {
            return new TwoPartyMessageIDs[size];
        }
    };

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public long getTm() {
        return tm;
    }

    public void setTm(long tm) {
        this.tm = tm;
    }

    @Override
    public String toString() {
        return "TwoPartyMessageIDs{" +
                "sid='" + sid + '\'' +
                ", rid='" + rid + '\'' +
                ", tm=" + tm +
                '}';
    }
}
