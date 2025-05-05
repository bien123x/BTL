package com.example.btl.Activity;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.btl.Helper.CloudinaryConfig;
import com.example.btl.Domain.Model.Artifact;
import com.example.btl.Domain.Model.ArtifactSample;
import com.example.btl.Domain.Model.User;
import com.example.btl.Domain.Repository.ArtifactRepository;
import com.example.btl.Domain.Repository.AuthRepository;
import com.example.btl.Domain.Repository.UserRepository;
import com.example.btl.R;
import com.example.btl.databinding.DialogArtifactInfoBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener, GoogleMap.OnMarkerClickListener {
    private GoogleMap mMap;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float azimuth = 0f;
    private LatLng currentUserLocation;

    private AuthRepository authRepository;
    private UserRepository userRepository;
    private ArtifactRepository artifactRepository;
    private Map<String, Artifact> artifactMap;
    private Map<String, User> userMap;
    private static final String TAG = "MainActivity";
    private static final float COLLECT_DISTANCE_THRESHOLD = 50f;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private Map<String, Marker> markerMap;
    private ImageView compassArrow;
    private Button collectButton;
    private ImageView userAvatar;
    private TextView userName;
    private TextView userScore;
    private ImageButton messageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CloudinaryConfig.init(getApplicationContext());

        compassArrow = findViewById(R.id.compassArrow);
        collectButton = findViewById(R.id.collectButton);
        userAvatar = findViewById(R.id.userAvatar);
        userName = findViewById(R.id.userName);
        userScore = findViewById(R.id.userScore);
        messageButton = findViewById(R.id.messageButton);

        authRepository = new AuthRepository();
        userRepository = new UserRepository();
        artifactRepository = new ArtifactRepository();
        artifactMap = new HashMap<>();
        userMap = new HashMap<>();
        markerMap = new HashMap<>();

        if (authRepository.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!checkGooglePlayServices()) {
            Toast.makeText(this, "Google Play Services không khả dụng", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
//            Log.d(TAG, "SupportMapFragment initialized successfully");
        } else {
            Toast.makeText(this, "Không thể khởi tạo bản đồ", Toast.LENGTH_LONG).show();
//            Log.e(TAG, "SupportMapFragment is null");
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (android.location.Location location : locationResult.getLocations()) {
                    currentUserLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    if (mMap != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 15f));
                    }
                    updateCollectButtonState();
                    updateUserLocationInFirestore(location.getLatitude(), location.getLongitude());
                    if (artifactMap.isEmpty()) {
                        generateRandomArtifacts();
                    }
                }
            }
        };

        loadUserInfo();

        collectButton.setOnClickListener(v -> collectNearestArtifact());

        messageButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MessageListActivity.class));
        });

        userAvatar.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MyProfileActivity.class));
        });

        checkLocationPermission();

        updateOnlineStatus(true);
    }

    private boolean checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.e(TAG, "This device does not support Google Play Services");
            }
            return false;
        }
        return true;
    }

    private void loadUserInfo() {
        String userId = authRepository.getCurrentUser().getUid();
        userRepository.getUser(userId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        User user = task.getResult();
                        if (user != null) {
                            userName.setText(user.getName());
                            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                                Glide.with(this)
                                        .load(user.getAvatar())
                                        .placeholder(R.drawable.default_avatar)
                                        .into(userAvatar);
                            }
                        }
                    } else {
                        Toast.makeText(this, "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                    }
                });

        // Tải danh sách cổ vật để tính điểm
        artifactRepository.getCollectedArtifacts(userId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Artifact> collectedArtifacts = task.getResult();
                        int totalPoints = 0;
                        for (Artifact artifact : collectedArtifacts) {
                            totalPoints += artifact.getPoints();
                        }
                        userScore.setText("Điểm: " + totalPoints);
                    } else {
                        Toast.makeText(this, "Không thể tải danh sách cổ vật", Toast.LENGTH_SHORT).show();
                        userScore.setText("Điểm: 0"); // Hiển thị mặc định nếu có lỗi
                    }
                });
    }

    private void updateUserLocationInFirestore(double latitude, double longitude) {
        String userId = authRepository.getCurrentUser().getUid();
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        userRepository.updateUserLocation(userId, locationData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated user location in Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update user location: " + e.getMessage()))
        ;
    }

    private void updateOnlineStatus(boolean online) {
        String userId = authRepository.getCurrentUser().getUid();
        userRepository.updateOnlineStatus(userId, online)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated online status to " + online))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update online status: " + e.getMessage()))
        ;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap == null) {
            Toast.makeText(this, "Không thể tải bản đồ", Toast.LENGTH_LONG).show();
//            Log.e(TAG, "GoogleMap is null");
            return;
        }

//        Log.d(TAG, "GoogleMap initialized successfully");
        mMap.setOnMarkerClickListener(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Quyền định vị chưa được cấp", Toast.LENGTH_SHORT).show();
//            Log.w(TAG, "Location permission not granted");
        }

        loadOtherUsers();
    }

    private void generateRandomArtifacts() {
        if (currentUserLocation == null) return;

        List<ArtifactSample> samples = ArtifactSample.getSampleArtifacts();
        Random random = new Random();

        for (int i = 0; i < 4; i++) {
            ArtifactSample sample = samples.get(random.nextInt(samples.size()));

            double radiusInDegrees = 500.0 / 111320.0;
            double latOffset = (random.nextDouble() * 2 - 1) * radiusInDegrees;
            double lngOffset = (random.nextDouble() * 2 - 1) * radiusInDegrees * Math.cos(Math.toRadians(currentUserLocation.latitude));
            double randomLat = currentUserLocation.latitude + latOffset;
            double randomLng = currentUserLocation.longitude + lngOffset;

            Artifact artifact = new Artifact();
            artifact.setId(UUID.randomUUID().toString());
            artifact.setName(sample.getName());
            artifact.setDescription(sample.getDescription());
            artifact.setImageUrl(sample.getImageUrl());
            artifact.setLatitude(randomLat);
            artifact.setLongitude(randomLng);
            artifact.setRarity(sample.getRarity());
            artifact.setPoints(sample.getPoints());

            LatLng location = new LatLng(artifact.getLatitude(), artifact.getLongitude());
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title(artifact.getName())
                    .icon(getBitmapDescriptorFromVector(R.drawable.star_icon)));

            artifactMap.put(marker.getId(), artifact);
            markerMap.put(marker.getId(), marker);

            if (artifact.getImageUrl() != null && !artifact.getImageUrl().isEmpty()) {
                Glide.with(this)
                        .asBitmap()
                        .load(artifact.getImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(resource, 100, 100, false);
                                marker.setIcon(BitmapDescriptorFactory.fromBitmap(scaledBitmap));
                            }

                            @Override
                            public void onLoadCleared(Drawable placeholder) {
                            }

                            @Override
                            public void onLoadFailed(Drawable errorDrawable) {
//                                Log.e(TAG, "Failed to load artifact image: " + artifact.getImageUrl());
                                marker.setIcon(getBitmapDescriptorFromVector(R.drawable.error_image));
                            }
                        });
            }
        }
    }

    private BitmapDescriptor getBitmapDescriptorFromVector(int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, vectorResId);
        if (vectorDrawable == null) {
            return null;
        }
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void loadOtherUsers() {
        userRepository.getOnlineUsers()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<User> users = task.getResult();
                        for (User user : users) {
                            if (user.getId().equals(authRepository.getCurrentUser().getUid())) {
                                continue;
                            }
                            if (user.getLatitude() == 0.0 && user.getLongitude() == 0.0) {
                                continue;
                            }
                            LatLng location = new LatLng(user.getLatitude(), user.getLongitude());
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(location)
                                    .title(user.getName())
                                    .icon(getBitmapDescriptorFromVector(R.drawable.user_icon)));
                            userMap.put(marker.getId(), user);
                        }
                    } else {
//                        Log.e(TAG, "Failed to load other users: " + task.getException().getMessage());
                    }
                });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Artifact artifact = artifactMap.get(marker.getId());
        if (artifact != null) {
            showArtifactDialog(artifact, marker);
            return true;
        }
        User user = userMap.get(marker.getId());
        if (user != null) {
            Intent intent = new Intent(MainActivity.this, UserProfileActivity.class);
            intent.putExtra("user", user);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private void showArtifactDialog(Artifact artifact, Marker marker) {
        Dialog dialog = new Dialog(this);
        DialogArtifactInfoBinding binding = DialogArtifactInfoBinding.inflate(getLayoutInflater());
        dialog.setContentView(binding.getRoot());

        binding.artifactName.setText(artifact.getName());
        binding.artifactDescription.setText(artifact.getDescription());
        binding.artifactRarity.setText("Độ hiếm: " + artifact.getRarity());
        binding.artifactPoints.setText("Điểm: " + artifact.getPoints());

        if (artifact.getImageUrl() != null && !artifact.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(artifact.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.artifactImage);
        } else {
            binding.artifactImage.setImageResource(R.drawable.placeholder_image);
        }

        String userId = authRepository.getCurrentUser().getUid();
        artifactRepository.hasUserCollectedArtifact(userId, artifact.getId())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean hasCollected = task.getResult();
                        if (hasCollected) {
                            binding.collectButton.setText("Đã thu thập");
                            binding.collectButton.setEnabled(false);
                            binding.collectButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
                        } else {
                            binding.collectButton.setOnClickListener(v -> {
                                artifact.setCollectedBy(userId);
                                artifactRepository.addArtifact(artifact,userId)
                                        .addOnCompleteListener(collectTask -> {
                                            if (collectTask.isSuccessful()) {
                                                Toast.makeText(this, "Thu thập thành công! Bạn nhận được " + artifact.getPoints() + " điểm", Toast.LENGTH_SHORT).show();
                                                binding.collectButton.setText("Đã thu thập");
                                                binding.collectButton.setEnabled(false);
                                                binding.collectButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
                                                loadUserInfo();
                                                marker.remove();
                                                artifactMap.remove(marker.getId());
                                                markerMap.remove(marker.getId());
                                                generateRandomArtifacts();
                                            } else {
                                                Toast.makeText(this, "Không thể thu thập cổ vật: " + collectTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                            });
                        }
                    } else {
                        Toast.makeText(this, "Không thể kiểm tra trạng thái thu thập", Toast.LENGTH_SHORT).show();
                    }
                });

        binding.closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateCollectButtonState() {
        if (currentUserLocation == null) {
            collectButton.setEnabled(false);
            return;
        }

        boolean canCollect = false;
        for (Artifact artifact : artifactMap.values()) {
            LatLng artifactLocation = new LatLng(artifact.getLatitude(), artifact.getLongitude());
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    currentUserLocation.latitude, currentUserLocation.longitude,
                    artifactLocation.latitude, artifactLocation.longitude,
                    results);
            float distance = results[0];
            if (distance <= COLLECT_DISTANCE_THRESHOLD) {
                canCollect = true;
                break;
            }
        }
        collectButton.setEnabled(canCollect);
    }

    private void collectNearestArtifact() {
        if (currentUserLocation == null) {
            Toast.makeText(this, "Không thể xác định vị trí của bạn", Toast.LENGTH_SHORT).show();
            return;
        }

        Artifact nearestArtifact = null;
        float minDistance = Float.MAX_VALUE;
        String nearestMarkerId = null;

        for (Map.Entry<String, Artifact> entry : artifactMap.entrySet()) {
            Artifact artifact = entry.getValue();
            LatLng artifactLocation = new LatLng(artifact.getLatitude(), artifact.getLongitude());
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    currentUserLocation.latitude, currentUserLocation.longitude,
                    artifactLocation.latitude, artifactLocation.longitude,
                    results);
            float distance = results[0];
            if (distance <= COLLECT_DISTANCE_THRESHOLD && distance < minDistance) {
                minDistance = distance;
                nearestArtifact = artifact;
                nearestMarkerId = entry.getKey();
            }
        }

        if (nearestArtifact != null && nearestMarkerId != null) {
            Marker nearestMarker = markerMap.get(nearestMarkerId);
            if (nearestMarker != null) {
                handleArtifactCollection(nearestArtifact, nearestMarker);
            }
        }
    }

    private void handleArtifactCollection(Artifact artifact, Marker marker) {
        String userId = authRepository.getCurrentUser().getUid();
        artifactRepository.hasUserCollectedArtifact(userId, artifact.getId())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult()) {
                        artifact.setCollectedBy(userId);
                        artifactRepository.addArtifact(artifact,userId)
                                .addOnCompleteListener(collectTask -> {
                                    if (collectTask.isSuccessful()) {
                                        Toast.makeText(this, "Thu thập thành công! Bạn nhận được " + artifact.getPoints() + " điểm", Toast.LENGTH_SHORT).show();
                                        loadUserInfo();
                                        updateCollectButtonState();
                                        marker.remove();
                                        artifactMap.remove(marker.getId());
                                        markerMap.remove(marker.getId());
                                        generateRandomArtifacts();
                                    } else {
                                        Toast.makeText(this, "Không thể thu thập cổ vật: " + collectTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Bạn đã thu thập cổ vật này rồi", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    mMap.setMyLocationEnabled(true);
                    startLocationUpdates();
                }
            }
        } else {
            Toast.makeText(this, "Ứng dụng cần quyền định vị để hoạt động", Toast.LENGTH_LONG).show();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone();
            float x = event.values[0];
            if (x > 15) {
                Toast.makeText(this, "Bạn đang chạy!", Toast.LENGTH_SHORT).show();
            }
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values.clone();
        }

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;
                compassArrow.setRotation(azimuth);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
        updateOnlineStatus(true);
        loadUserInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        fusedLocationClient.removeLocationUpdates(locationCallback);
        updateOnlineStatus(false);
    }
}