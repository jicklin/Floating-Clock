package com.yoyofloatingclock;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Size;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRScannerActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private ProcessCameraProvider cameraProvider;
    
    // UI Elements
    private CardView cardResult;
    private TextView tvScanResult;
    private Button btnCopy;
    private Button btnScanAgain;
    private ImageButton btnBack;
    private TabLayout tabLayout;
    private View layoutScanMode;
    private View layoutGenerateMode;
    
    // Generate Mode UI
    private EditText etQrContent;
    private Button btnGenerate;
    private CardView cardQrImage;
    private ImageView ivQrCode;
    private Button btnSaveQr;
    private Bitmap generatedQrBitmap;
    
    // URL Smart Edit UI
    private Button btnParseUrl;
    private View layoutUrlEdit;
    private EditText etBaseUrl;
    private LinearLayout containerParams;
    private Button btnAddParam;
    private Button btnUpdateUrl;
    
    // Scan from Image
    private Button btnSelectImage;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    private boolean isScanning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
        
        // Initialize Image Picker
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                scanImage(uri);
            }
        });

        initViews();
        initBarcodeScanner();
        setupListeners();
        
        // 默认开始相机
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.CAMERA}, 10);
        }
    }

    private void initViews() {
        viewFinder = findViewById(R.id.viewFinder);
        cardResult = findViewById(R.id.card_result);
        tvScanResult = findViewById(R.id.tv_scan_result);
        btnCopy = findViewById(R.id.btn_copy);
        btnScanAgain = findViewById(R.id.btn_scan_again);
        btnBack = findViewById(R.id.btn_back);
        
        tabLayout = findViewById(R.id.tab_layout);
        layoutScanMode = findViewById(R.id.layout_scan_mode);
        layoutGenerateMode = findViewById(R.id.layout_generate_mode);
        
        etQrContent = findViewById(R.id.et_qr_content);
        btnGenerate = findViewById(R.id.btn_generate);
        cardQrImage = findViewById(R.id.card_qr_image);
        ivQrCode = findViewById(R.id.iv_qr_code);
        btnSaveQr = findViewById(R.id.btn_save_qr);
        
        // URL Edit Views
        btnParseUrl = findViewById(R.id.btn_parse_url);
        layoutUrlEdit = findViewById(R.id.layout_url_edit);
        etBaseUrl = findViewById(R.id.et_base_url);
        containerParams = findViewById(R.id.container_params);
        btnAddParam = findViewById(R.id.btn_add_param);
        btnUpdateUrl = findViewById(R.id.btn_update_url);
        
        // Scan from Image
        btnSelectImage = findViewById(R.id.btn_select_image);
    }

    private void initBarcodeScanner() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnCopy.setOnClickListener(v -> {
            String result = tvScanResult.getText().toString().replace(getString(R.string.qr_scan_result_prefix), "");
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Scan Result", result);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.copy_success, Toast.LENGTH_SHORT).show();
        });
        
        btnScanAgain.setOnClickListener(v -> {
            cardResult.setVisibility(View.GONE);
            isScanning = true;
        });
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutScanMode.setVisibility(View.VISIBLE);
                    layoutGenerateMode.setVisibility(View.GONE);
                    isScanning = true;
                    hideKeyboard();
                } else {
                    layoutScanMode.setVisibility(View.GONE);
                    layoutGenerateMode.setVisibility(View.VISIBLE);
                    isScanning = false;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        btnGenerate.setOnClickListener(v -> generateQRCode());
        btnSaveQr.setOnClickListener(v -> saveQRCodeToGallery());
        
        // URL Smart Edit Listeners
        etQrContent.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                if (text.startsWith("http://") || text.startsWith("https://")) {
                    btnParseUrl.setVisibility(View.VISIBLE);
                } else {
                    btnParseUrl.setVisibility(View.GONE);
                    layoutUrlEdit.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        btnParseUrl.setOnClickListener(v -> parseUrlParams());
        btnAddParam.setOnClickListener(v -> addParamRow("", ""));
        btnUpdateUrl.setOnClickListener(v -> rebuildUrl());
        
        // Scan from Image Listener
        btnSelectImage.setOnClickListener(v -> 
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build())
        );
    }
    
    private void scanImage(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(this, uri);
            barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (barcodes.isEmpty()) {
                        Toast.makeText(this, R.string.error_no_qr_found, Toast.LENGTH_SHORT).show();
                    } else {
                        Barcode barcode = barcodes.get(0);
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null) {
                            onBarcodeDetected(rawValue);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.error_load_image, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_load_image, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void parseUrlParams() {
        String url = etQrContent.getText().toString().trim();
        try {
            Uri uri = Uri.parse(url);
            String baseUrl = url.split("\\?")[0];
            etBaseUrl.setText(baseUrl);
            
            containerParams.removeAllViews();
            if (uri.getQuery() != null) {
                for (String key : uri.getQueryParameterNames()) {
                    String value = uri.getQueryParameter(key);
                    addParamRow(key, value);
                }
            }
            
            layoutUrlEdit.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void addParamRow(String key, String value) {
        View row = getLayoutInflater().inflate(R.layout.item_url_param, containerParams, false);
        EditText etKey = row.findViewById(R.id.et_key);
        EditText etValue = row.findViewById(R.id.et_value);
        ImageButton btnRemove = row.findViewById(R.id.btn_remove);
        
        etKey.setText(key);
        etValue.setText(value);
        
        btnRemove.setOnClickListener(v -> containerParams.removeView(row));
        
        containerParams.addView(row);
    }
    
    private void rebuildUrl() {
        String baseUrl = etBaseUrl.getText().toString().trim();
        if (baseUrl.isEmpty()) return;
        
        Uri.Builder builder = Uri.parse(baseUrl).buildUpon();
        builder.clearQuery();
        
        for (int i = 0; i < containerParams.getChildCount(); i++) {
            View row = containerParams.getChildAt(i);
            EditText etKey = row.findViewById(R.id.et_key);
            EditText etValue = row.findViewById(R.id.et_value);
            
            String key = etKey.getText().toString().trim();
            String value = etValue.getText().toString().trim();
            
            if (!key.isEmpty()) {
                builder.appendQueryParameter(key, value);
            }
        }
        
        String newUrl = builder.build().toString();
        etQrContent.setText(newUrl);
        layoutUrlEdit.setVisibility(View.GONE);
        generateQRCode();
    }
    
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    
    private void generateQRCode() {
        String content = etQrContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.input_empty_error, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 隐藏键盘
        hideKeyboard();
        
        try {
            generatedQrBitmap = createQRCodeBitmap(content, 800, 800);
            ivQrCode.setImageBitmap(generatedQrBitmap);
            cardQrImage.setVisibility(View.VISIBLE);
            btnSaveQr.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.generate_success, Toast.LENGTH_SHORT).show();
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "生成失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private Bitmap createQRCodeBitmap(String content, int width, int height) throws WriterException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(
                content, BarcodeFormat.QR_CODE, width, height);
        
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitMatrix.get(x, y)) {
                    pixels[y * width + x] = Color.BLACK;
                } else {
                    pixels[y * width + x] = Color.WHITE;
                }
            }
        }
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
    
    private void saveQRCodeToGallery() {
        if (generatedQrBitmap == null) return;
        
        String filename = "QR_" + System.currentTimeMillis() + ".png";
        OutputStream fos;
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues resolver = new ContentValues();
                resolver.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                resolver.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                resolver.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FloatingClock");
                
                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, resolver);
                fos = getContentResolver().openOutputStream(imageUri);
            } else {
                Toast.makeText(this, "系统版本过低，暂不支持保存", Toast.LENGTH_SHORT).show();
                return;
            }
            
            generatedQrBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            if (fos != null) {
                fos.flush();
                fos.close();
                Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                // Handle any errors (including cancellation) here.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;
        
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new BarcodeAnalyzer());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                getBaseContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void onBarcodeDetected(String result) {
        runOnUiThread(() -> {
            isScanning = false;
            tvScanResult.setText(getString(R.string.qr_scan_result_prefix) + result);
            cardResult.setVisibility(View.VISIBLE);
        });
    }

    private class BarcodeAnalyzer implements ImageAnalysis.Analyzer {

        @androidx.camera.core.ExperimentalGetImage
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (!isScanning) {
                imageProxy.close();
                return;
            }
            
            android.media.Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                Task<List<Barcode>> result = barcodeScanner.process(image);
                result.addOnSuccessListener(barcodes -> {
                            if (!barcodes.isEmpty() && isScanning) {
                                Barcode barcode = barcodes.get(0);
                                String rawValue = barcode.getRawValue();
                                if (rawValue != null) {
                                    onBarcodeDetected(rawValue);
                                }
                            }
                        })
                        .addOnCompleteListener(task -> imageProxy.close());
            } else {
                imageProxy.close();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
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
}
