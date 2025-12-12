package com.yoyofloatingclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * 增强的图片拼接View - 支持横向和纵向拼接，每张图片带插入/删除按钮
 */
public class StitchingView extends LinearLayout {
    
    private StitchMode stitchMode = StitchMode.HORIZONTAL;
    private List<Bitmap> images = new ArrayList<>();
    private List<ImageItem> imageItems = new ArrayList<>();
    
    private int spacing = 0;
    private int backgroundColor = Color.WHITE;
    
    private OnImageActionListener actionListener;
    
    public interface OnImageActionListener {
        void onInsertBefore(int position);
        void onDelete(int position);
    }
    
    public StitchingView(Context context) {
        this(context, null);
    }
    
    public StitchingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        setOrientation(VERTICAL);  // 默认纵向，会根据mode改变
        setBackgroundColor(backgroundColor);
    }
    
    public void setOnImageActionListener(OnImageActionListener listener) {
        this.actionListener = listener;
    }
    
    /**
     * 设置拼接模式
     */
    public void setStitchMode(StitchMode mode) {
        this.stitchMode = mode;
        setOrientation(mode == StitchMode.HORIZONTAL ? HORIZONTAL : VERTICAL);
        updateImages();
    }
    
    /**
     * 添加图片
     */
    public void addImage(Bitmap bitmap) {
        images.add(bitmap);
        updateImages();
    }
    
    /**
     * 在指定位置插入图片
     */
    public void insertImage(int position, Bitmap bitmap) {
        if (position >= 0 && position <= images.size()) {
            images.add(position, bitmap);
            updateImages();
        }
    }
    
    /**
     * 删除指定位置的图片（不回收bitmap，由外部管理）
     */
    public void removeImage(int position) {
        if (position >= 0 && position < images.size()) {
            images.remove(position);  // 不recycle，由Activity管理
            updateImages();
        }
    }
    
    /**
     * 设置所有图片
     */
    public void setImages(List<Bitmap> images) {
        this.images.clear();
        this.images.addAll(images);
        updateImages();
    }
    
    /**
     * 清空图片（不回收bitmap）
     */
    public void clearImages() {
        images.clear();
        imageItems.clear();
        removeAllViews();
    }
    
    /**
     * 设置间距
     */
    public void setSpacing(int spacing) {
        this.spacing = spacing;
        updateImages();
    }
    
    /**
     * 更新图片显示
     */
    private void updateImages() {
        removeAllViews();
        imageItems.clear();
        
        for (int i = 0; i < images.size(); i++) {
            final int position = i;
            Bitmap bitmap = images.get(i);
            
            // 跳过无效的bitmap
            if (bitmap == null || bitmap.isRecycled()) {
                continue;
            }
            
            ImageItem item = new ImageItem(getContext());
            item.setImage(bitmap, stitchMode);
            item.setOnInsertClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onInsertBefore(position);
                }
            });
            item.setOnDeleteClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDelete(position);
                }
            });
            
            imageItems.add(item);
            addView(item);
            
            // 添加间距
            if (i < images.size() - 1 && spacing > 0) {
                View spacer = new View(getContext());
                LayoutParams params = stitchMode == StitchMode.HORIZONTAL ?
                    new LayoutParams(spacing, LayoutParams.MATCH_PARENT) :
                    new LayoutParams(LayoutParams.MATCH_PARENT, spacing);
                spacer.setLayoutParams(params);
                addView(spacer);
            }
        }
    }
    
    /**
     * 导出为Bitmap
     */
    public Bitmap exportBitmap() {
        // 隐藏所有按钮，只导出图片
        for (ImageItem item : imageItems) {
            item.hideButtons();
        }
        
        measure(
            MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        );
        
        layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
        
        Bitmap bitmap = Bitmap.createBitmap(
            getMeasuredWidth(), 
            getMeasuredHeight(), 
            Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        
        // 恢复按钮显示
        for (ImageItem item : imageItems) {
            item.showButtons();
        }
        
        return bitmap;
    }
    
    public int getImageCount() {
        return images.size();
    }
    
    public StitchMode getStitchMode() {
        return stitchMode;
    }
    
    /**
     * 单个图片项 - 包含图片和按钮
     */
    private static class ImageItem extends FrameLayout {
        private ImageView imageView;
        private View btnInsert, btnDelete;
        
        public ImageItem(Context context) {
            super(context);
            init();
        }
        
        private void init() {
            imageView = new ImageView(getContext());
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            addView(imageView, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ));
            
            // 插入按钮 - 使用圆形背景的View
            btnInsert = createRoundButton("+", 0xFF4CAF50);  // 绿色
            FrameLayout.LayoutParams insertParams = new FrameLayout.LayoutParams(
                dpToPx(48),
                dpToPx(48)
            );
            insertParams.leftMargin = dpToPx(8);
            insertParams.topMargin = dpToPx(8);
            addView(btnInsert, insertParams);
            
            // 删除按钮 - 使用圆形背景的View
            btnDelete = createRoundButton("×", 0xFF757575);  // 深灰色
            FrameLayout.LayoutParams deleteParams = new FrameLayout.LayoutParams(
                dpToPx(48),
                dpToPx(48)
            );
            deleteParams.rightMargin = dpToPx(8);
            deleteParams.topMargin = dpToPx(8);
            deleteParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            addView(btnDelete, deleteParams);
        }
        
        private View createRoundButton(String text, int backgroundColor) {
            FrameLayout button = new FrameLayout(getContext());
            
            // 设置圆形背景
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(backgroundColor);
            button.setBackground(drawable);
            
            // 添加文字
            TextView textView = new TextView(getContext());
            textView.setText(text);
            textView.setTextColor(Color.WHITE);
            textView.setTextSize(24);
            textView.setGravity(android.view.Gravity.CENTER);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
            
            button.addView(textView, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ));
            
            button.setClickable(true);
            button.setFocusable(true);
            
            return button;
        }
        
        private int dpToPx(int dp) {
            return (int) (dp * getContext().getResources().getDisplayMetrics().density);
        }
        
        public void setImage(Bitmap bitmap, StitchMode mode) {
            if (bitmap == null) return;
            
            imageView.setImageBitmap(bitmap);
            
            // 根据模式调整布局参数
            LinearLayout.LayoutParams params;
            if (mode == StitchMode.HORIZONTAL) {
                // 横拼：宽度wrap，高度match
                int width = (int) (bitmap.getWidth() * 300.0f / bitmap.getHeight());
                params = new LinearLayout.LayoutParams(width, 300);
            } else {
                // 竖拼：宽度match，高度wrap
                int height = (int) (bitmap.getHeight() * 300.0f / bitmap.getWidth());
                params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
            }
            setLayoutParams(params);
        }
        
        public void setOnInsertClickListener(OnClickListener listener) {
            btnInsert.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                if (listener != null) {
                    listener.onClick(v);
                }
            });
        }
        
        public void setOnDeleteClickListener(OnClickListener listener) {
            btnDelete.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                if (listener != null) {
                    listener.onClick(v);
                }
            });
        }
        
        public void hideButtons() {
            btnInsert.setVisibility(GONE);
            btnDelete.setVisibility(GONE);
        }
        
        public void showButtons() {
            btnInsert.setVisibility(VISIBLE);
            btnDelete.setVisibility(VISIBLE);
        }
    }
}
