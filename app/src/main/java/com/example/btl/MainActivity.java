package com.example.btl;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng userLocation; // Vị trí hiện tại của người chơi
    private TextView scoreTextView; // Hiển thị điểm số
    private Button collectButton; // Nút để thu thập cổ vật
    private int playerScore = 0; // Điểm số của người chơi

    // Danh sách cổ vật
    private List<Artifact> artifacts = new ArrayList<>();
    private Map<Marker, Artifact> markerToArtifactMap = new HashMap<>(); // Liên kết marker với cổ vật

    // Lớp Artifact để lưu thông tin cổ vật
    private static class Artifact {
        LatLng location;
        String name;
        String rarity; // Độ hiếm: Thường, Hiếm, Huyền Thoại
        int points; // Điểm số
        boolean isCollected; // Trạng thái đã thu thập hay chưa

        Artifact(LatLng location, String name, String rarity, int points) {
            this.location = location;
            this.name = name;
            this.rarity = rarity;
            this.points = points;
            this.isCollected = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo giao diện
        scoreTextView = findViewById(R.id.scoreTextView);
        collectButton = findViewById(R.id.collectButton);
        scoreTextView.setText("Điểm: " + playerScore);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Khởi tạo LocationCallback để theo dõi vị trí người chơi
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (android.location.Location location : locationResult.getLocations()) {
                    userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    // Kiểm tra khoảng cách đến các cổ vật
                    checkProximityToArtifacts();
                }
            }
        };

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Không thể khởi tạo bản đồ", Toast.LENGTH_LONG).show();
        }

        checkLocationPermission();

        // Xử lý sự kiện khi nhấn nút thu thập
        collectButton.setOnClickListener(v -> collectNearbyArtifact());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Đặt vị trí mặc định cho camera
        LatLng defaultLocation = new LatLng(10.7769, 106.7009);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));

        // Thêm các cổ vật vào bản đồ
        initializeArtifacts();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            startLocationUpdates();
        } else {
            Toast.makeText(this, "Quyền định vị chưa được cấp", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeArtifacts() {
        // Thêm các cổ vật (tọa độ, tên, độ hiếm, điểm số)
        artifacts.add(new Artifact(new LatLng(10.7769, 106.7009), "Bình gốm cổ", "Thường", 10));
        artifacts.add(new Artifact(new LatLng(10.7800, 106.7100), "Kiếm đồng", "Hiếm", 50));
        artifacts.add(new Artifact(new LatLng(10.7700, 106.6900), "Vương miện vàng", "Huyền Thoại", 100));

        // Hiển thị các cổ vật trên bản đồ
        for (Artifact artifact : artifacts) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(artifact.location)
                    .title(artifact.name)
                    .snippet("Độ hiếm: " + artifact.rarity + " | Điểm: " + artifact.points));
            markerToArtifactMap.put(marker, artifact);
        }
    }

    private void checkProximityToArtifacts() {
        if (userLocation == null) return;

        boolean nearArtifact = false;
        for (Artifact artifact : artifacts) {
            if (artifact.isCollected) continue; // Bỏ qua cổ vật đã thu thập

            float distance = calculateDistance(userLocation, artifact.location);
            if (distance < 10) { // Khoảng cách < 10 mét
                nearArtifact = true;
                break;
            }
        }

        // Hiển thị hoặc ẩn nút thu thập dựa trên khoảng cách
        collectButton.setVisibility(nearArtifact ? View.VISIBLE : View.GONE);
    }

    private void collectNearbyArtifact() {
        if (userLocation == null) return;

        for (Artifact artifact : artifacts) {
            if (artifact.isCollected) continue;

            float distance = calculateDistance(userLocation, artifact.location);
            if (distance < 10) { // Khoảng cách < 10 mét
                artifact.isCollected = true;
                playerScore += artifact.points;
                scoreTextView.setText("Điểm: " + playerScore);
                Toast.makeText(this, "Đã thu thập: " + artifact.name + " (" + artifact.rarity + ")! +" + artifact.points + " điểm", Toast.LENGTH_LONG).show();

                // Xóa marker của cổ vật đã thu thập
                for (Map.Entry<Marker, Artifact> entry : markerToArtifactMap.entrySet()) {
                    if (entry.getValue() == artifact) {
                        entry.getKey().remove();
                        markerToArtifactMap.remove(entry.getKey());
                        break;
                    }
                }
                break; // Chỉ thu thập một cổ vật gần nhất
            }
        }

        // Kiểm tra lại khoảng cách sau khi thu thập
        checkProximityToArtifacts();
    }

    private float calculateDistance(LatLng point1, LatLng point2) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(
                point1.latitude, point1.longitude,
                point2.latitude, point2.longitude,
                results);
        return results[0];
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "Ứng dụng cần quyền định vị để hiển thị vị trí của bạn trên bản đồ", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000); // Cập nhật mỗi 5 giây
        locationRequest.setFastestInterval(2000); // Nhanh nhất là 2 giây
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
}