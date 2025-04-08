package com.example.btl.Domain.Model;

import java.io.Serializable;

public class User implements Serializable {
    private String id;
    private String name;
    private String email;
    private String avatar;
    private long score;
    private boolean online;
    private double latitude;
    private double longitude;

    // Constructor mặc định
    public User() {}

    // Constructor mới với các tham số
    public User(String id, String name, String email, String avatar, long score, boolean online) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.avatar = avatar;
        this.score = score;
        this.online = online;
        this.latitude = 0.0;
        this.longitude = 0.0;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}