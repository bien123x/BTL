package com.example.btl.Domain.Model;

import com.google.firebase.Timestamp;

import java.util.UUID;

public class Artifact {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private double latitude; // Giữ lại để dùng trong MainActivity
    private double longitude; // Giữ lại để dùng trong MainActivity
    private String rarity; // Chuyển từ int sang String (ví dụ: "Common", "Rare", "Epic")
    private int points;
    private String collectedBy; // ID của user đã thu thập
    private Timestamp collectedAt; // Thời gian thu thập

    public Artifact() {
        this.id = UUID.randomUUID().toString();
    }

    public Artifact(String id, String name, String description, String imageUrl, double latitude, double longitude, String rarity, int points) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rarity = rarity;
        this.points = points;
    }

    // Getters và Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    // Phương thức để chuyển đổi từ int sang String (tương thích với code cũ)
    public void setRarity(int rarityValue) {
        switch (rarityValue) {
            case 1:
                this.rarity = "Common";
                break;
            case 2:
                this.rarity = "Uncommon";
                break;
            case 3:
                this.rarity = "Rare";
                break;
            case 4:
                this.rarity = "Epic";
                break;
            case 5:
                this.rarity = "Legendary";
                break;
            default:
                this.rarity = "Unknown";
        }
    }

    // Phương thức để lấy rarity dưới dạng int (nếu cần cho logic cũ)
    public int getRarityAsInt() {
        switch (rarity != null ? rarity : "") {
            case "Common":
                return 1;
            case "Uncommon":
                return 2;
            case "Rare":
                return 3;
            case "Epic":
                return 4;
            case "Legendary":
                return 5;
            default:
                return 0;
        }
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getCollectedBy() {
        return collectedBy;
    }

    public void setCollectedBy(String collectedBy) {
        this.collectedBy = collectedBy;
    }

    public Timestamp getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(Timestamp collectedAt) {
        this.collectedAt = collectedAt;
    }
}