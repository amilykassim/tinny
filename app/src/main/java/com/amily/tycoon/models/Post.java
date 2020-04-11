package com.amily.tycoon.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class Post implements Parcelable {

    private String t; // title of description
    private String d; // description (post)
    private long tm; // time created
    private String pid; // post id
    private String uid; // user id
    private String v; // post views
    private List<Comment> c; // comments


    public Post() {

    }

    public Post(String t, String d, long tm, String pid, String uid, String v, List<Comment> c) {
        this.t = t;
        this.d = d;
        this.tm = tm;
        this.pid = pid;
        this.uid = uid;
        this.v = v;
        this.c = c;
    }

    protected Post(Parcel in) {
        t = in.readString();
        d = in.readString();
        tm = in.readLong();
        pid = in.readString();
        uid = in.readString();
        v = in.readString();
        c = in.createTypedArrayList(Comment.CREATOR);
    }

    public static final Creator<Post> CREATOR = new Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    public String getT() {
        return t;
    }

    public void setT(String t) {
        this.t = t;
    }

    public String getD() {
        return d;
    }

    public void setD(String d) {
        this.d = d;
    }

    public long getTm() {
        return tm;
    }

    public void setTm(long tm) {
        this.tm = tm;
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

    public String getV() {
        return v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public List<Comment> getC() {
        return c;
    }

    public void setC(List<Comment> c) {
        this.c = c;
    }

    @Override
    public String toString() {
        return "Post{" +
                "t='" + t + '\'' +
                ", d='" + d + '\'' +
                ", tm=" + tm +
                ", pid='" + pid + '\'' +
                ", uid='" + uid + '\'' +
                ", v='" + v + '\'' +
                ", c=" + c +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(t);
        dest.writeString(d);
        dest.writeLong(tm);
        dest.writeString(pid);
        dest.writeString(uid);
        dest.writeString(v);
        dest.writeTypedList(c);
    }
}












