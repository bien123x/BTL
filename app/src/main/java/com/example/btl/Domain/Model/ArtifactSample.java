package com.example.btl.Domain.Model;

import java.util.ArrayList;
import java.util.List;

public class ArtifactSample {
    private String name;
    private String description;
    private String imageUrl;
    private int rarity;
    private int points;

    public ArtifactSample(String name, String description, String imageUrl, int rarity, int points) {
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

    public int getRarity() {
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
                1,
                50
        ));
        samples.add(new ArtifactSample(
                "Cổ vật 2",
                "Mô tả cổ vật 2",
                "https://res.cloudinary.com/dnlgpuwyr/image/upload/v1744471189/artifact2_rid2rb.png ",
                2,
                60
        ));
        samples.add(new ArtifactSample(
                "Cổ vật 3",
                "Mô tả cổ vật 3",
                "https://res.cloudinary.com/dnlgpuwyr/image/upload/v1744471187/artifact3_tytxuc.png",
                3,
                70
        ));
        samples.add(new ArtifactSample(
                "Cổ vật 3",
                "Mô tả cổ vật 4",
                "https://res.cloudinary.com/dnlgpuwyr/image/upload/v1744471188/artifact4_hpjott.png",
                4,
                80
        ));
        return samples;
    }
}
