package com.clew.android.trace;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class Trace implements Parcelable {

    private String id, name;
    private int population, posts;
    private long time;
    private Date createdAt;

    public Trace() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(id);
        parcel.writeString(name);
        parcel.writeInt(population);
        parcel.writeInt(posts);
        parcel.writeLong(time);
    }

    public static final Creator<Trace> CREATOR = new Creator<Trace>() {
        @Override
        public Trace createFromParcel(Parcel source) {
            return new Trace(source);
        }

        @Override
        public Trace[] newArray(int size) {
            return new Trace[size];
        }
    };

    private Trace(Parcel in){
        id = in.readString();
        name = in.readString();
        population = in.readInt();
        posts = in.readInt();
        time = in.readLong();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    public int getPosts() {
        return posts;
    }

    public void setPosts(int posts) {
        this.posts = posts;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
