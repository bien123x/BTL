plugins {
    alias(libs.plugins.android.application)

    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.btl"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.btl"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    //  Thêm dòng này để bật Data Binding
    buildFeatures {
        viewBinding =true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation ("com.google.firebase:firebase-messaging")

    // Firebase bổ sung
    implementation("com.google.firebase:firebase-auth") // F1, F2, F3: Xác thực người dùng
    implementation("com.google.firebase:firebase-firestore") // F4, F14: Lưu trữ dữ liệu
    implementation("com.google.firebase:firebase-database") // F15: Đồng bộ thời gian thực
    implementation ("com.google.firebase:firebase-storage")
    // Google Maps và Location (F5, F6, F7, F8)
    implementation("com.google.android.gms:play-services-maps:18.2.0") // Hiển thị bản đồ
    implementation("com.google.android.gms:play-services-location:21.0.1") // Định vị người chơi
    implementation("com.google.android.gms:play-services-base:18.5.0")

    // Cảm biến (F9, F10)
    implementation("androidx.core:core:1.12.0") // Hỗ trợ cảm biến gia tốc kế và la bàn

    //  Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation ("com.cloudinary:cloudinary-android:2.2.0")
}