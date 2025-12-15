package com.yoyofloatingclock;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<Intent> overlayPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化权限请求启动器
        initPermissionLaunchers();

        // 悬浮时钟 - 开启按钮
        Button startBtn = findViewById(R.id.btn_start_float);
        startBtn.setOnClickListener(v -> {
            checkOverlayPermission();
        });

        // 悬浮时钟 - 关闭按钮
        Button stopBtn = findViewById(R.id.btn_stop_float);
        stopBtn.setOnClickListener(v -> {
            Toast.makeText(this, R.string.toast_float_stopped, Toast.LENGTH_SHORT).show();
            stopService(new Intent(MainActivity.this, FloatService.class));
        });

        // 二维码扫描按钮
        Button scanQrBtn = findViewById(R.id.btn_scan_qr);
        scanQrBtn.setOnClickListener(v -> {
            checkCameraPermissionAndScan();
        });

        // 照片拼图按钮
        Button btnPuzzle = findViewById(R.id.btn_puzzle);
        btnPuzzle.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PuzzleActivityEnhanced.class);
            startActivity(intent);
        });

        Button btnWatermark = findViewById(R.id.btn_watermark);
        btnWatermark.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WatermarkActivity.class);
            startActivity(intent);
        });
    }

    private void initPermissionLaunchers() {
        // 相机权限请求启动器
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openQRScanner();
                    } else {
                        Toast.makeText(this, R.string.toast_permission_camera, Toast.LENGTH_LONG).show();
                    }
                }
        );

        // 悬浮窗权限请求启动器
        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Settings.canDrawOverlays(this)) {
                        Toast.makeText(this, R.string.toast_permission_granted, Toast.LENGTH_SHORT).show();
                        startFloatService();
                    } else {
                        Toast.makeText(this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.toast_permission_overlay, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                overlayPermissionLauncher.launch(intent);
            } else {
                startFloatService();
            }
        } else {
            startFloatService();
        }
    }

    private void startFloatService() {
        Toast.makeText(this, R.string.toast_float_started, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, FloatService.class);
        startService(intent);
    }

    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openQRScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openQRScanner() {
        Intent intent = new Intent(this, QRScannerActivity.class);
        startActivity(intent);
    }

}