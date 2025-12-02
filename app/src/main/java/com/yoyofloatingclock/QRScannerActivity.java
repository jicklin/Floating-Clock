package com.yoyofloatingclock;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRScannerActivity extends AppCompatActivity {

    private static final String TAG = "QRScannerActivity";

    private PreviewView previewView;
    private TextView tvScanHint;
    private LinearLayout resultContainer;
    private TextView tvResultContent;
    private Button btnCopyResult;
    private Button btnScanAgain;
    private ImageButton btnBack;

    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;

    private boolean isScanning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        initViews();
        initBarcodeScanner();
        startCamera();

        btnBack.setOnClickListener(v -> finish());
        btnCopyResult.setOnClickListener(v -> copyResultToClipboard());
        btnScanAgain.setOnClickListener(v -> resetScanner());
    }

    private void initViews() {
        previewView = findViewById(R.id.preview_view);
        tvScanHint = findViewById(R.id.tv_scan_hint);
        resultContainer = findViewById(R.id.result_container);
        tvResultContent = findViewById(R.id.tv_result_content);
        btnCopyResult = findViewById(R.id.btn_copy_result);
        btnScanAgain = findViewById(R.id.btn_scan_again);
        btnBack = findViewById(R.id.btn_back);
    }

    private void initBarcodeScanner() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera initialization failed", e);
                Toast.makeText(this, "相机初始化失败", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            return;
        }

        // 预览用例
        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // 图像分析用例
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new BarcodeAnalyzer());

        // 相机选择器（后置摄像头）
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // 解绑所有用例
            cameraProvider.unbindAll();

            // 绑定用例到生命周期
            camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void onBarcodeDetected(String result) {
        runOnUiThread(() -> {
            isScanning = false;
            tvScanHint.setVisibility(View.GONE);
            resultContainer.setVisibility(View.VISIBLE);
            tvResultContent.setText(result);
        });
    }

    private void copyResultToClipboard() {
        String result = tvResultContent.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("QR Result", result);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.toast_result_copied, Toast.LENGTH_SHORT).show();
    }

    private void resetScanner() {
        isScanning = true;
        tvScanHint.setVisibility(View.VISIBLE);
        resultContainer.setVisibility(View.GONE);
        tvResultContent.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }

    // 条形码分析器
    private class BarcodeAnalyzer implements ImageAnalysis.Analyzer {

        @androidx.camera.core.ExperimentalGetImage
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (!isScanning) {
                imageProxy.close();
                return;
            }

            @androidx.camera.core.ExperimentalGetImage
            android.media.Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.getImageInfo().getRotationDegrees()
                );

                Task<List<Barcode>> result = barcodeScanner.process(image);
                result.addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null && !rawValue.isEmpty()) {
                            onBarcodeDetected(rawValue);
                            break;
                        }
                    }
                }).addOnCompleteListener(task -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        }
    }
}
