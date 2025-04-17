package com.example.btl.Domain.Model;

import java.util.ArrayList;
import java.util.List;

public class ArtifactSample {
    private String name;
    private String description;
    private String imageUrl;
    private String rarity; // Đổi từ int sang String
    private int points;

    public ArtifactSample(String name, String description, String imageUrl, String rarity, int points) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.rarity = rarity;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getRarity() {
        return rarity;
    }

    public int getPoints() {
        return points;
    }

    // Danh sách cổ vật mẫu
    public static List<ArtifactSample> getSampleArtifacts() {
        List<ArtifactSample> samples = new ArrayList<>();
        samples.add(new ArtifactSample(
                "Cổ vật 1",
                "Mô tả cổ vật 1",
                "https://res.cloudinary.com/dnlgpuwyr/image/upload/v1744471187/artifact1_luj37m.png",
                "Common", // Đổi từ 1 sang "Common"
                50
        ));
        samples.add(new ArtifactSample(
                "Cổ vật 2",
                "Mô tả cổ vật 2",
                "https://res.cloudinary.com/dnlgpuwyr/image/upload/v1744471189/artifact2_rid2rb.png ",
                "Uncommon", // Đổi từ 2 sang "Uncommon"
                60
        ));
        samples.add(new ArtifactSample(
                "Cổ vật 3",
                "Mô tả cổ vật 3",
                "https://res.cloudinary.com/dnlgpuwyr/image/upload/v1744471187/artifact3_tytxuc.png",
                "Rare", // Đổi từ 3 sang "Rare"
                70
        ));
        samples.add(new ArtifactSample(
                "Cổ vật 4", // Sửa tên từ "Cổ vật 3" thành "Cổ vật 4" để tránh trùng lặp
                "Mô tả cổ vật 4",
                "https://res.cloudinary.com/dnlgpuwyr/image/upload/v1744471188/artifact4_hpjott.png",
                "Epic", // Đổi từ 4 sang "Epic"
                80
        ));
        return samples;
    }
}