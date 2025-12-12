package com.yoyofloatingclock;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 照片拼图Activity
 */
public class PuzzleActivity extends AppCompatActivity {

    private PuzzleView puzzleView;
    private TextView tvHint;
    private LinearLayout templatesContainer;
    
    private List<Uri> selectedImageUris = new ArrayList<>();
    private PuzzleLayout.LayoutType currentLayout = PuzzleLayout.LayoutType.GRID_4;
    
    private int spacing = 10;
    private int borderWidth = 0;
    private int backgroundColor = Color.WHITE;
    private int borderColor = Color.WHITE;
    
    private ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle);
        
        initViews();
        initImagePicker();
        setupTemplates();
        setupButtons();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        puzzleView = findViewById(R.id.puzzle_view);
        tvHint = findViewById(R.id.tv_hint);
        templatesContainer = findViewById(R.id.layout_templates_container);
        
        // 设置默认布局
        puzzleView.post(() -> {
            puzzleView.setLayout(currentLayout);
            updateHint();
        });
    }
    
    private void initImagePicker() {
        // 使用新的照片选择器API (Android 13+)
        pickMultipleMedia = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(9),
            uris -> {
                if (!uris.isEmpty()) {
                    selectedImageUris.clear();
                    selectedImageUris.addAll(uris);
                    loadImagesToCanvas();
                }
            }
        );
    }
    
    private void setupTemplates() {
        PuzzleLayout.LayoutType[] layouts = PuzzleLayout.LayoutType.values();
        
        for (PuzzleLayout.LayoutType layout : layouts) {
            View itemView = LayoutInflater.from(this)
                .inflate(R.layout.layout_template_item, templatesContainer, false);
            
            MaterialCardView card = (MaterialCardView) itemView;
            TextView nameText = itemView.findViewById(R.id.template_name);
            nameText.setText(layout.getDisplayName());
            
            // 设置选中状态
            if (layout == currentLayout) {
                card.setStrokeColor(getColor(R.color.color_accent));
            }
            
            card.setOnClickListener(v -> {
                currentLayout = layout;
                puzzleView.setLayout(layout);
                updateTemplateSelection();
                loadImagesToCanvas();
            });
            
            templatesContainer.addView(itemView);
        }
    }
    
    private void updateTemplateSelection() {
        for (int i = 0; i < templatesContainer.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) templatesContainer.getChildAt(i);
            PuzzleLayout.LayoutType layout = PuzzleLayout.LayoutType.values()[i];
            
            if (layout == currentLayout) {
                card.setStrokeColor(getColor(R.color.color_accent));
            } else {
                card.setStrokeColor(Color.TRANSPARENT);
            }
        }
    }
    
    private void setupButtons() {
        Button btnSelectPhotos = findViewById(R.id.btn_select_photos);
        btnSelectPhotos.setOnClickListener(v -> selectPhotos());
        
        Button btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        
        Button btnExport = findViewById(R.id.btn_export);
        btnExport.setOnClickListener(v -> exportPuzzle());
    }
    
    private void selectPhotos() {
        pickMultipleMedia.launch(new PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
            .build());
    }
    
    private void loadImagesToCanvas() {
        puzzleView.clearImages();
        
        int cellCount = puzzleView.getCellCount();
        int imageCount = Math.min(selectedImageUris.size(), cellCount);
        
        for (int i = 0; i < imageCount; i++) {
            final int index = i;
            Glide.with(this)
                .asBitmap()
                .load(selectedImageUris.get(i))
                .override(1000, 1000)  // 限制图片大小
                .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                        puzzleView.setImage(index, bitmap);
                        updateHint();
                    }
                    
                    @Override
                    public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                    }
                });
        }
    }
    
    private void updateHint() {
        if (puzzleView.getFilledCellCount() == 0) {
            tvHint.setVisibility(View.VISIBLE);
            tvHint.setText(R.string.hint_select_photos);
        } else {
            tvHint.setVisibility(View.GONE);
        }
    }
    
    private void showSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_puzzle_settings, null);
        
        SeekBar seekSpacing = dialogView.findViewById(R.id.seek_spacing);
        SeekBar seekBorder = dialogView.findViewById(R.id.seek_border);
        TextView tvSpacingValue = dialogView.findViewById(R.id.tv_spacing_value);
        TextView tvBorderValue = dialogView.findViewById(R.id.tv_border_value);
        
        // 设置当前值
        seekSpacing.setProgress(spacing);
        seekBorder.setProgress(borderWidth);
        tvSpacingValue.setText(spacing + "px");
        tvBorderValue.setText(borderWidth + "px");
        
        seekSpacing.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSpacingValue.setText(progress + "px");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBorder.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvBorderValue.setText(progress + "px");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                spacing = seekSpacing.getProgress();
                borderWidth = seekBorder.getProgress();
                
                puzzleView.setSpacing(spacing);
                puzzleView.setBorderWidth(borderWidth);
                
                // 重新加载图片
                loadImagesToCanvas();
            })
            .setNegativeButton(R.string.btn_cancel, null)
            .show();
    }
    
    private void exportPuzzle() {
        if (puzzleView.getFilledCellCount() == 0) {
            Toast.makeText(this, R.string.toast_select_photos_first, Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Bitmap bitmap = puzzleView.exportBitmap();
            saveBitmapToGallery(bitmap);
            Toast.makeText(this, R.string.toast_puzzle_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.toast_puzzle_save_failed, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveBitmapToGallery(Bitmap bitmap) throws IOException {
        String displayName = "Puzzle_" + System.currentTimeMillis() + ".jpg";
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Puzzles");
            
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                }
            }
        } else {
            // For older Android versions
            MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                displayName,
                "Photo puzzle created with YoYo"
            );
        }
    }
}
