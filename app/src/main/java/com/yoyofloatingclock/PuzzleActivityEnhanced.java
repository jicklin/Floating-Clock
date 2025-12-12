package com.yoyofloatingclock;

import android.app.Activity;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 完全增强的照片拼图Activity - 支持先选布局，点击格子添加图片
 */
public class PuzzleActivityEnhanced extends AppCompatActivity {

    private MaterialButtonToggleGroup modeToggleGroup;
    private LinearLayout variantsContainer;
    private ScrollView variantsScrollContainer;
    private FrameLayout canvasContainer;
    private PuzzleViewEnhanced puzzleView;
    private StitchingView stitchingView;
    private TextView tvHint;
    
    private List<Bitmap> loadedBitmaps = new ArrayList<>();
    
    private PuzzleLayoutEnhanced.PuzzleMode currentMode = PuzzleLayoutEnhanced.PuzzleMode.GRID;
    private PuzzleLayoutEnhanced.LayoutVariant currentVariant = null;
    private StitchMode currentStitchMode = StitchMode.HORIZONTAL;
    
    private int spacing = 10;
    private int borderWidth = 0;
    private int currentCellIndex = -1;  // 当前点击的格子索引
    
    // 画布比例设置
    private float canvasAspectRatio = 0f;  // 0表示自由比例
    private String aspectRatioName = "自由";  // 用于显示
    
    // 单张图片选择器（为单个格子选图）
    private ActivityResultLauncher<PickVisualMediaRequest> pickSingleMedia;
    // 多张图片选择器（批量选图）
    private ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia;
    // 自定义布局编辑器
    private ActivityResultLauncher<Intent> customLayoutLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle_enhanced);
        
        initViews();
        initImagePickers();
        setupModeToggle();
        setupButtons();
        setupPuzzleView();
        
        // 默认显示所有布局变体
        showAllLayoutVariants();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        modeToggleGroup = findViewById(R.id.mode_toggle_group);
        variantsContainer = findViewById(R.id.variants_container);
        variantsScrollContainer = findViewById(R.id.variants_scroll_container);
        canvasContainer = findViewById(R.id.canvas_container);
        tvHint = findViewById(R.id.tv_hint);
        
        puzzleView = new PuzzleViewEnhanced(this);
        stitchingView = new StitchingView(this);
        
        canvasContainer.addView(puzzleView);
    }
    
    private void initImagePickers() {
        // 单张图片选择（点击格子时用）
        pickSingleMedia = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    loadSingleImageForCell(uri, currentCellIndex);
                }
            }
        );
        
        // 多张图片选择（批量导入）
        pickMultipleMedia = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(50),
            uris -> {
                if (!uris.isEmpty()) {
                    loadMultipleImages(uris);
                }
            }
        );
        
        // 自定义布局编辑器
        customLayoutLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    ArrayList<GridCell> cells = result.getData().getParcelableArrayListExtra("grid_cells");
                    if (cells != null && !cells.isEmpty()) {
                        // 直接应用自定义布局的GridCells
                        puzzleView.setLayoutFromCells(cells);
                        Toast.makeText(this, String.format("已应用自定义布局（%d格）", cells.size()), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }
    
    private void setupPuzzleView() {
        // 设置格子点击监听
        puzzleView.setOnCellClickListener(cellIndex -> {
            // 点击空格子，选择图片填充
            currentCellIndex = cellIndex;
            pickSingleMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
        });
    }
    
    private void setupModeToggle() {
        modeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            
            if (checkedId == R.id.btn_mode_grid) {
                switchToMode(PuzzleLayoutEnhanced.PuzzleMode.GRID);
            } else if (checkedId == R.id.btn_mode_stitch_h) {
                switchToMode(PuzzleLayoutEnhanced.PuzzleMode.STITCH_H);
            } else if (checkedId == R.id.btn_mode_stitch_v) {
                switchToMode(PuzzleLayoutEnhanced.PuzzleMode.STITCH_V);
            }
        });
    }
    
    private void switchToMode(PuzzleLayoutEnhanced.PuzzleMode mode) {
        currentMode = mode;
        
        canvasContainer.removeAllViews();
        
        if (mode == PuzzleLayoutEnhanced.PuzzleMode.GRID) {
            canvasContainer.addView(puzzleView);
            variantsScrollContainer.setVisibility(View.VISIBLE);
            showAllLayoutVariants();
        } else {
            // 根据拼接方向使用不同的ScrollView
            currentStitchMode = (mode == PuzzleLayoutEnhanced.PuzzleMode.STITCH_H) ? 
                StitchMode.HORIZONTAL : StitchMode.VERTICAL;
            stitchingView.setStitchMode(currentStitchMode);
            
            // 设置插入/删除监听器
            stitchingView.setOnImageActionListener(new StitchingView.OnImageActionListener() {
                @Override
                public void onInsertBefore(int position) {
                    // 选择图片并插入到指定位置
                    currentCellIndex = position;
                    pickSingleMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
                }
                
                @Override
                public void onDelete(int position) {
                    if (position >= 0 && position < loadedBitmaps.size()) {
                        loadedBitmaps.remove(position);
                        stitchingView.setImages(loadedBitmaps);
                        Toast.makeText(PuzzleActivityEnhanced.this, "已删除图片", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            
            // 包裹在ScrollView中
            if (currentStitchMode == StitchMode.HORIZONTAL) {
                // 横拼 - 横向滚动
                android.widget.HorizontalScrollView scrollView = new android.widget.HorizontalScrollView(this);
                scrollView.addView(stitchingView);
                canvasContainer.addView(scrollView);
            } else {
                // 竖拼 - 纵向滚动
                ScrollView scrollView = new ScrollView(this);
                scrollView.addView(stitchingView);
                canvasContainer.addView(scrollView);
            }
            
            variantsScrollContainer.setVisibility(View.GONE);
            
            if (!loadedBitmaps.isEmpty()) {
                stitchingView.setImages(loadedBitmaps);
            }
        }
        
        updateHint();
    }
    
    /**
     * 显示所有可用的布局变体（按图片数量分组）
     */
    private void showAllLayoutVariants() {
        variantsContainer.removeAllViews();
        
        // 显示2-9张图片的所有布局，按数量分组
        for (int count = 2; count <= 9; count++) {
            List<PuzzleLayoutEnhanced.LayoutVariant> variants = 
                PuzzleLayoutEnhanced.getVariantsForCount(count);
            
            if (!variants.isEmpty()) {
                // 添加分组标题
                addGroupHeader(count, variants.size());
                
                // 添加该组的所有布局
                for (final PuzzleLayoutEnhanced.LayoutVariant variant : variants) {
                    addLayoutVariantCard(variant);
                }
                
                // 添加分组间隔
                if (count < 9) {
                    addGroupSpacer();
                }
            }
        }
    }
    
    /**
     * 添加分组标题
     */
    private void addGroupHeader(int photoCount, int variantCount) {
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        headerLayout.setPadding(0, 0, 0, 8);
        
        TextView headerText = new TextView(this);
        headerText.setText(String.format("%d张图片", photoCount));
        headerText.setTextSize(16);
        headerText.setTypeface(null, android.graphics.Typeface.BOLD);
        headerText.setTextColor(getColor(R.color.color_text_primary));
        
        TextView countText = new TextView(this);
        countText.setText(String.format("%d种布局", variantCount));
        countText.setTextSize(12);
        countText.setTextColor(getColor(R.color.color_text_secondary));
        
        headerLayout.addView(headerText);
        headerLayout.addView(countText);
        
        variantsContainer.addView(headerLayout);
    }
    
    /**
     * 添加布局变体卡片
     */
    private void addLayoutVariantCard(PuzzleLayoutEnhanced.LayoutVariant variant) {
        View itemView = LayoutInflater.from(this)
            .inflate(R.layout.layout_template_item, variantsContainer, false);
        
        MaterialCardView card = (MaterialCardView) itemView;
        TextView nameText = itemView.findViewById(R.id.template_name);
        LayoutPreviewView previewView = itemView.findViewById(R.id.layout_preview);
        
        nameText.setText(variant.getDisplayName());
        previewView.setGridCells(variant.getGridCells());
        
        if (variant == currentVariant) {
            card.setStrokeColor(getColor(R.color.color_accent));
            card.setStrokeWidth(4);
        } else {
            card.setStrokeColor(Color.TRANSPARENT);
            card.setStrokeWidth(2);
        }
        
        card.setOnClickListener(v -> {
            currentVariant = variant;
            applyLayout(variant);
            refreshVariantSelection();
        });
        
        variantsContainer.addView(itemView);
    }
    
    /**
     * 添加分组间隔
     */
    private void addGroupSpacer() {
        View spacer = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            8, LinearLayout.LayoutParams.MATCH_PARENT);
        spacer.setLayoutParams(params);
        spacer.setBackgroundColor(0x00000000);  // 透明
        variantsContainer.addView(spacer);
    }
    
    private void applyLayout(PuzzleLayoutEnhanced.LayoutVariant variant) {
        puzzleView.setLayoutVariant(variant);
        refreshVariantSelection();
        updateHint();
    }
    
    private void refreshVariantSelection() {
        for (int i = 0; i < variantsContainer.getChildCount(); i++) {
            View child = variantsContainer.getChildAt(i);
            if (child instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) child;
                LayoutPreviewView previewView = child.findViewById(R.id.layout_preview);
                TextView nameText = child.findViewById(R.id.template_name);
                
                if (nameText != null && currentVariant != null && 
                    nameText.getText().toString().equals(currentVariant.getDisplayName())) {
                    card.setStrokeColor(getColor(R.color.color_accent));
                    card.setStrokeWidth(4);
                } else {
                    card.setStrokeColor(Color.TRANSPARENT);
                    card.setStrokeWidth(2);
                }
            }
        }
    }
    
    private void setupButtons() {
        Button btnSelectPhotos = findViewById(R.id.btn_select_photos);
        btnSelectPhotos.setText("批量添加");
        btnSelectPhotos.setOnClickListener(v -> selectMultiplePhotos());
        
        Button btnCustomLayout = findViewById(R.id.btn_custom_layout);
        btnCustomLayout.setOnClickListener(v -> openCustomLayoutEditor());
        
        Button btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        
        Button btnExport = findViewById(R.id.btn_export);
        btnExport.setOnClickListener(v -> exportPuzzle());
    }
    
    private void openCustomLayoutEditor() {
        Intent intent = new Intent(this, LayoutEditorActivity.class);
        customLayoutLauncher.launch(intent);
    }
    
    private void selectMultiplePhotos() {
        pickMultipleMedia.launch(new PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
            .build());
    }
    
    /**
     * 为单个格子加载图片
     */
    private void loadSingleImageForCell(Uri uri, int cellIndex) {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .override(1500, 1500)
            .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap bitmap, com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                    if (currentMode == PuzzleLayoutEnhanced.PuzzleMode.GRID) {
                        puzzleView.setImageForCell(cellIndex, bitmap);
                    } else {
                        // 拼接模式 - 在指定位置插入图片
                        loadedBitmaps.add(cellIndex, bitmap);
                        stitchingView.setImages(loadedBitmaps);
                    }
                    updateHint();
                }
                
                @Override
                public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                }
            });
    }
    
    /**
     * 批量加载图片
     */
    private void loadMultipleImages(List<Uri> uris) {
        loadedBitmaps.clear();
        
        int totalImages = uris.size();
        final int[] loadedCount = {0};
        
        for (Uri uri : uris) {
            Glide.with(this)
                .asBitmap()
                .load(uri)
                .override(1500, 1500)
                .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                        loadedBitmaps.add(bitmap);
                        loadedCount[0]++;
                        
                        if (loadedCount[0] == totalImages) {
                            onAllImagesLoaded();
                        }
                    }
                    
                    @Override
                    public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                    }
                });
        }
    }
    
    private void onAllImagesLoaded() {
        if (currentMode == PuzzleLayoutEnhanced.PuzzleMode.GRID) {
            puzzleView.setImages(loadedBitmaps);
        } else {
            stitchingView.setImages(loadedBitmaps);
        }
        updateHint();
    }
    
    private void updateHint() {
        if (currentMode == PuzzleLayoutEnhanced.PuzzleMode.GRID) {
            if (currentVariant == null) {
                tvHint.setVisibility(View.VISIBLE);
                tvHint.setText("请选择一个布局开始");
            } else if (puzzleView.getFilledCellCount() == 0) {
                tvHint.setVisibility(View.VISIBLE);
                tvHint.setText("点击格子选择图片");
            } else {
                tvHint.setVisibility(View.GONE);
            }
        }else {
            if (loadedBitmaps.isEmpty()) {
                tvHint.setVisibility(View.VISIBLE);
                tvHint.setText(R.string.hint_stitch_mode);
            } else {
                tvHint.setVisibility(View.GONE);
            }
        }
    }
    
    private void showSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_puzzle_settings, null);
        
        SeekBar seekSpacing = dialogView.findViewById(R.id.seek_spacing);
        SeekBar seekBorder = dialogView.findViewById(R.id.seek_border);
        TextView tvSpacingValue = dialogView.findViewById(R.id.tv_spacing_value);
        TextView tvBorderValue = dialogView.findViewById(R.id.tv_border_value);
        
        MaterialButtonToggleGroup aspectGroup1 = dialogView.findViewById(R.id.aspect_ratio_group);
        MaterialButtonToggleGroup aspectGroup2 = dialogView.findViewById(R.id.aspect_ratio_group2);
        android.widget.RadioGroup formatGroup = dialogView.findViewById(R.id.format_group);
        
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
            .setTitle("设置")
            .setView(dialogView)
            .setPositiveButton("应用", (dialog, which) -> {
                spacing = seekSpacing.getProgress();
                borderWidth = seekBorder.getProgress();
                
                // 应用画布比例
                int checkedAspectId = aspectGroup1.getCheckedButtonId();
                if (checkedAspectId == View.NO_ID) {
                    checkedAspectId = aspectGroup2.getCheckedButtonId();
                }
                
                if (checkedAspectId == R.id.btn_ratio_free) {
                    canvasAspectRatio = 0f;
                    aspectRatioName = "自由";
                } else if (checkedAspectId == R.id.btn_ratio_16_9) {
                    canvasAspectRatio = 16f / 9f;
                    aspectRatioName = "16:9";
                } else if (checkedAspectId == R.id.btn_ratio_9_16) {
                    canvasAspectRatio = 9f / 16f;
                    aspectRatioName = "9:16";
                } else if (checkedAspectId == R.id.btn_ratio_4_3) {
                    canvasAspectRatio = 4f / 3f;
                    aspectRatioName = "4:3";
                } else if (checkedAspectId == R.id.btn_ratio_3_4) {
                    canvasAspectRatio = 3f / 4f;
                    aspectRatioName = "3:4";
                } else if (checkedAspectId == R.id.btn_ratio_1_1) {
                    canvasAspectRatio = 1f;
                    aspectRatioName = "1:1";
                } else if (checkedAspectId == R.id.btn_ratio_2_3) {
                    canvasAspectRatio = 2f / 3f;
                    aspectRatioName = "2:3";
                } else if (checkedAspectId == R.id.btn_ratio_3_2) {
                    canvasAspectRatio = 3f / 2f;
                    aspectRatioName = "3:2";
                }
                
                // 应用设置到PuzzleView
                puzzleView.setSpacing(spacing);
                puzzleView.setBorderWidth(borderWidth);
                puzzleView.setCanvasAspectRatio(canvasAspectRatio);
                
                stitchingView.setSpacing(spacing);
                
                Toast.makeText(this, "设置已应用 - 画布比例: " + aspectRatioName, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void exportPuzzle() {
        if (currentMode == PuzzleLayoutEnhanced.PuzzleMode.GRID && puzzleView.getFilledCellCount() == 0) {
            Toast.makeText(this, "请先添加图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Bitmap bitmap;
            if (currentMode == PuzzleLayoutEnhanced.PuzzleMode.GRID) {
                bitmap = puzzleView.exportBitmap();
            } else {
                bitmap = stitchingView.exportBitmap();
            }
            
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
            MediaStore.Images.Media.insertImage(
                getContentResolver(),
                bitmap,
                displayName,
                "Photo puzzle created with YoYo"
            );
        }
    }
}
