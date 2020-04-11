package com.amily.tycoon.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Story implements Parcelable{

    private String c; // caption
    private String unm; // username
    private String pp; // profile photo
    private long tm; // time created
    private String url; // image url
    private String th; // image thumbnail
    private String pid; // photo id
    private String uid; // user id
    private String vUrl; // video path
    private String vTh; // video thumbnail
    private String vs;

    public Story() {

    }

    public Story(String c, String unm, String pp, long tm, String url, String th, String pid, String uid, String vUrl, String vTh, String vs) {
        this.c = c;
        this.unm = unm;
        this.pp = pp;
        this.tm = tm;
        this.url = url;
        this.th = th;
        this.pid = pid;
        this.uid = uid;
        this.vUrl = vUrl;
        this.vTh = vTh;
        this.vs = vs;
    }

    protected Story(Parcel in) {
        c = in.readString();
        unm = in.readString();
        pp = in.readString();
        tm = in.readLong();
        url = in.readString();
        th = in.readString();
        pid = in.readString();
        uid = in.readString();
        vUrl = in.readString();
        vTh = in.readString();
        vs = in.readString();
    }

    public static final Creator<Story> CREATOR = new Creator<Story>() {
        @Override
        public Story createFromParcel(Parcel in) {
            return new Story(in);
        }

        @Override
        public Story[] newArray(int size) {
            return new Story[size];
        }
    };

    public String getC() {
        return c;
    }

    public void setC(String c) {
        this.c = c;
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

    public long getTm() {
        return tm;
    }

    public void setTm(long tm) {
        this.tm = tm;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTh() {
        return th;
    }

    public void setTh(String th) {
        this.th = th;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getvUrl() {
        return vUrl;
    }

    public void setvUrl(String vUrl) {
        this.vUrl = vUrl;
    }

    public String getvTh() {
        return vTh;
    }

    public void setvTh(String vTh) {
        this.vTh = vTh;
    }

    public String getVs() {
        return vs;
    }

    public void setVs(String vs) {
        this.vs = vs;
    }

    @Override
    public String toString() {
        return "Story{" +
                "c='" + c + '\'' +
                ", unm='" + unm + '\'' +
                ", pp='" + pp + '\'' +
                ", tm=" + tm +
                ", url='" + url + '\'' +
                ", th='" + th + '\'' +
                ", pid='" + pid + '\'' +
                ", uid='" + uid + '\'' +
                ", vUrl='" + vUrl + '\'' +
                ", vTh='" + vTh + '\'' +
                ", vs='" + vs + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(c);
        dest.writeString(unm);
        dest.writeString(pp);
        dest.writeLong(tm);
        dest.writeString(url);
        dest.writeString(th);
        dest.writeString(pid);
        dest.writeString(uid);
        dest.writeString(vUrl);
        dest.writeString(vTh);
        dest.writeString(vs);
    }
}
