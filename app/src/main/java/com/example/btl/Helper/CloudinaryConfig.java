package com.example.btl.Helper;

import android.content.Context;

import com.cloudinary.Cloudinary;
import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryConfig {
    private static Cloudinary cloudinary;

    public static void init(Context context) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dnlgpuwyr"); // Thay bằng Cloud Name của bạn
        config.put("api_key", "388245198361581"); // Thay bằng API Key của bạn
        config.put("api_secret", "B6FacmB02VNKiF97YVl2ppDanxM"); // Thay bằng API Secret của bạn

        MediaManager.init(context, config); // ✅ đúng kiểu
        cloudinary = new Cloudinary(config);
    }

    public static Cloudinary getCloudinary() {
        return cloudinary;
    }
}
