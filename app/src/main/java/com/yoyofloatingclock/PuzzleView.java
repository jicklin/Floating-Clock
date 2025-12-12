package com.yoyofloatingclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼图画布视图
 */
public class PuzzleView extends View {
    
    private PuzzleLayout puzzleLayout;
    private List<PuzzleImageCell> imageCells;
    private Paint paint;
    private Paint borderPaint;
    
    private int backgroundColor = Color.WHITE;
    private int borderColor = Color.WHITE;
    private int borderWidth = 0;
    private int spacing = 10;
    
    private PuzzleImageCell activeCell;  // 当前操作的格子
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    
    public PuzzleView(Context context) {
        this(context, null);
    }
    
    public PuzzleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        imageCells = new ArrayList<>();
        
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(borderColor);
        borderPaint.setStrokeWidth(borderWidth);
        
        // 手势检测器（用于拖动）
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (activeCell != null && activeCell.hasImage()) {
                    activeCell.translate(-distanceX, -distanceY);
                    invalidate();
                    return true;
                }
                return false;
            }
            
            @Override
            public boolean onDown(MotionEvent e) {
                // 查找触摸点所在的格子
                activeCell = findCellAt(e.getX(), e.getY());
                return activeCell != null;
            }
        });
        
        // 缩放手势检测器
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (activeCell != null && activeCell.hasImage()) {
                    float scaleFactor = detector.getScaleFactor();
                    activeCell.scaleBy(scaleFactor, detector.getFocusX(), detector.getFocusY());
                    invalidate();
                    return true;
                }
                return false;
            }
        });
    }
    
    /**
     * 设置拼图布局
     */
    public void setLayout(PuzzleLayout.LayoutType layoutType) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            // 视图未测量，延迟设置
            post(() -> setLayout(layoutType));
            return;
        }
        
        puzzleLayout = new PuzzleLayout(layoutType, getWidth(), getHeight(), spacing);
        updateCells();
    }
    
    /**
     * 更新所有格子
     */
    private void updateCells() {
        // 保存现有的图片
        List<Bitmap> existingImages = new ArrayList<>();
        for (PuzzleImageCell cell : imageCells) {
            if (cell.hasImage()) {
                existingImages.add(cell.getBitmap());
            }
        }
        
        // 清空旧格子
        imageCells.clear();
        
        // 创建新格子
        List<RectF> bounds = puzzleLayout.calculateCellBounds();
        for (RectF bound : bounds) {
            imageCells.add(new PuzzleImageCell(bound));
        }
        
        // 重新分配图片
        int imageIndex = 0;
        for (int i = 0; i < imageCells.size() && imageIndex < existingImages.size(); i++) {
            imageCells.get(i).setBitmap(existingImages.get(imageIndex++));
        }
        
        invalidate();
    }
    
    /**
     * 添加图片到拼图
     */
    public void addImage(Bitmap bitmap) {
        for (PuzzleImageCell cell : imageCells) {
            if (!cell.hasImage()) {
                cell.setBitmap(bitmap);
                invalidate();
                return;
            }
        }
    }
    
    /**
     * 设置指定位置的图片
     */
    public void setImage(int index, Bitmap bitmap) {
        if (index >= 0 && index < imageCells.size()) {
            imageCells.get(index).setBitmap(bitmap);
            invalidate();
        }
    }
    
    /**
     * 获取所有图片
     */
    public List<Bitmap> getImages() {
        List<Bitmap> images = new ArrayList<>();
        for (PuzzleImageCell cell : imageCells) {
            if (cell.hasImage()) {
                images.add(cell.getBitmap());
            }
        }
        return images;
    }
    
    /**
     * 清空所有图片
     */
    public void clearImages() {
        for (PuzzleImageCell cell : imageCells) {
            cell.clear();
        }
        invalidate();
    }
    
    /**
     * 查找指定坐标下的格子
     */
    private PuzzleImageCell findCellAt(float x, float y) {
        for (PuzzleImageCell cell : imageCells) {
            if (cell.contains(x, y)) {
                return cell;
            }
        }
        return null;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 绘制背景
        canvas.drawColor(backgroundColor);
        
        // 绘制所有格子
        for (PuzzleImageCell cell : imageCells) {
            cell.draw(canvas, paint);
            
            // 绘制边框
            if (borderWidth > 0) {
                canvas.drawRect(cell.getBounds(), borderPaint);
            }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = scaleGestureDetector.onTouchEvent(event);
        handled = gestureDetector.onTouchEvent(event) || handled;
        
        if (event.getAction() == MotionEvent.ACTION_UP || 
            event.getAction() == MotionEvent.ACTION_CANCEL) {
            activeCell = null;
        }
        
        return handled || super.onTouchEvent(event);
    }
    
    /**
     * 导出为Bitmap
     */
    public Bitmap exportBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);
        return bitmap;
    }
    
    // Setters
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        invalidate();
    }
    
    public void setBorderColor(int color) {
        this.borderColor = color;
        borderPaint.setColor(color);
        invalidate();
    }
    
    public void setBorderWidth(int width) {
        this.borderWidth = width;
        borderPaint.setStrokeWidth(width);
        invalidate();
    }
    
    public void setSpacing(int spacing) {
        this.spacing = spacing;
        if (puzzleLayout != null) {
            puzzleLayout.setSpacing(spacing);
            updateCells();
        }
    }
    
    public int getCellCount() {
        return imageCells.size();
    }
    
    public int getFilledCellCount() {
        int count = 0;
        for (PuzzleImageCell cell : imageCells) {
            if (cell.hasImage()) count++;
        }
        return count;
    }
}
