package com.clew.android.climage;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class Climage implements Parcelable {

    private String id, userId, userName, fullName, trace, type;
    private int likes; // type 1 = image, 2 = drawing
    private Date createdAt;

    public Climage() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(id);
        parcel.writeString(userId);
        parcel.writeString(userName);
        parcel.writeString(fullName);
        parcel.writeString(trace);
        parcel.writeString(type);
        parcel.writeInt(likes);
    }

    public static final Parcelable.Creator<Climage> CREATOR = new Parcelable.Creator<Climage>() {
        @Override
        public Climage createFromParcel(Parcel source) {
            return new Climage(source);
        }

        @Override
        public Climage[] newArray(int size) {
            return new Climage[size];
        }
    };

    private Climage(Parcel in){
        id = in.readString();
        userId = in.readString();
        userName = in.readString();
        fullName = in.readString();
        trace = in.readString();
        type = in.readString();
        likes = in.readInt();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
