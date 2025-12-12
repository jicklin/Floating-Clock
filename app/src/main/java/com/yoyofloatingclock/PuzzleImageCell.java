package com.yoyofloatingclock;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * 拼图中的单个图片格子
 */
public class PuzzleImageCell {
    private RectF bounds;  // 格子的边界
    private Bitmap bitmap;  // 图片
    private Matrix matrix;  // 变换矩阵（用于缩放、平移）
    private float scale = 1.0f;  // 缩放比例
    private float offsetX = 0;  // X偏移
    private float offsetY = 0;  // Y偏移

    public PuzzleImageCell(RectF bounds) {
        this.bounds = bounds;
        this.matrix = new Matrix();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        // 初始化时让图片填充满格子
        resetTransform();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public RectF getBounds() {
        return bounds;
    }

    public void setBounds(RectF bounds) {
        this.bounds = bounds;
        resetTransform();
    }

    /**
     * 重置变换，让图片填满格子
     */
    private void resetTransform() {
        if (bitmap == null) return;
        
        float cellWidth = bounds.width();
        float cellHeight = bounds.height();
        float imageWidth = bitmap.getWidth();
        float imageHeight = bitmap.getHeight();
        
        // 计算缩放比例，确保图片覆盖整个格子（类似 centerCrop）
        float scaleX = cellWidth / imageWidth;
        float scaleY = cellHeight / imageHeight;
        scale = Math.max(scaleX, scaleY);
        
        // 计算居中偏移
        offsetX = (cellWidth - imageWidth * scale) / 2;
        offsetY = (cellHeight - imageHeight * scale) / 2;
        
        updateMatrix();
    }

    /**
     * 更新变换矩阵
     */
    private void updateMatrix() {
        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(bounds.left + offsetX, bounds.top + offsetY);
    }

    /**
     * 平移图片
     */
    public void translate(float dx, float dy) {
        offsetX += dx;
        offsetY += dy;
        updateMatrix();
    }

    /**
     * 缩放图片
     */
    public void scaleBy(float scaleFactor, float focusX, float focusY) {
        // 相对于格子内的焦点进行缩放
        float localFocusX = focusX - bounds.left;
        float localFocusY = focusY - bounds.top;
        
        float oldScale = scale;
        scale *= scaleFactor;
        
        // 限制缩放范围
        float cellWidth = bounds.width();
        float cellHeight = bounds.height();
        if (bitmap != null) {
            float minScale = Math.max(cellWidth / bitmap.getWidth(), cellHeight / bitmap.getHeight());
            float maxScale = minScale * 3;
            scale = Math.max(minScale, Math.min(scale, maxScale));
        }
        
        // 调整偏移，使缩放围绕焦点进行
        float scaleChange = scale / oldScale;
        offsetX = localFocusX - (localFocusX - offsetX) * scaleChange;
        offsetY = localFocusY - (localFocusY - offsetY) * scaleChange;
        
        updateMatrix();
    }

    /**
     * 绘制图片
     */
    public void draw(Canvas canvas, Paint paint) {
        if (bitmap == null) return;
        
        canvas.save();
        canvas.clipRect(bounds);  // 裁剪到格子范围内
        canvas.drawBitmap(bitmap, matrix, paint);
        canvas.restore();
    }

    /**
     * 判断点是否在此格子内
     */
    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }

    /**
     * 检查是否有图片
     */
    public boolean hasImage() {
        return bitmap != null;
    }

    /**
     * 清除图片
     */
    public void clear() {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        bitmap = null;
    }
}
