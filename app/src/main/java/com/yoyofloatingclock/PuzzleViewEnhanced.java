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
 * 增强的拼图画布视图 - 支持点击格子添加图片
 */
public class PuzzleViewEnhanced extends View {
    
    private List<PuzzleImageCell> imageCells;
    private List<GridCell> gridCells;
    private Paint paint;
    private Paint borderPaint;
    private Paint plusPaint;
    private Paint bgPaint;
    
    private int backgroundColor = Color.WHITE;
    private int borderColor = Color.WHITE;
    private int borderWidth = 0;
    private int spacing = 10;
    private float canvasAspectRatio = 0f;  // 0表示自由比例
    
    private PuzzleImageCell activeCell;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    
    // 点击格子的回调
    private OnCellClickListener onCellClickListener;
    
    public interface OnCellClickListener {
        void onCellClick(int cellIndex);
    }
    
    public PuzzleViewEnhanced(Context context) {
        this(context, null);
    }
    
    public PuzzleViewEnhanced(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        imageCells = new ArrayList<>();
        gridCells = new ArrayList<>();
        
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(borderColor);
        borderPaint.setStrokeWidth(borderWidth);
        
        // "+" 号绘制
        plusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        plusPaint.setColor(0xFFCCCCCC);  // 浅灰色
        plusPaint.setStyle(Paint.Style.STROKE);
        plusPaint.setStrokeWidth(4);
        plusPaint.setStrokeCap(Paint.Cap.ROUND);
        
        // 空格子背景
        bgPaint = new Paint();
        bgPaint.setColor(0xFFF5F5F5);  // 浅灰背景
        
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // 单击检测 - 点击空格子添加图片
                int cellIndex = findCellIndexAt(e.getX(), e.getY());
                if (cellIndex >= 0 && cellIndex < imageCells.size()) {
                    PuzzleImageCell cell = imageCells.get(cellIndex);
                    // 如果是空格子，触发回调
                    if (!cell.hasImage() && onCellClickListener != null) {
                        onCellClickListener.onCellClick(cellIndex);
                        return true;
                    }
                }
                return false;
            }
            
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
                activeCell = findCellAt(e.getX(), e.getY());
                return activeCell != null;
            }
        });
        
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
    
    public void setOnCellClickListener(OnCellClickListener listener) {
        this.onCellClickListener = listener;
    }
    
    /**
     * 使用GridCell列表设置布局
     */
    public void setLayoutFromCells(List<GridCell> cells) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            post(() -> setLayoutFromCells(cells));
            return;
        }
        
        this.gridCells = new ArrayList<>(cells);
        
        // 计算所有GridCell的实际bounds
        List<RectF> bounds = PuzzleLayoutEnhanced.calculateBounds(
            cells, getWidth(), getHeight(), spacing
        );
        
        // 为每个GridCell设置bounds
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).setBounds(bounds.get(i));
        }
        
        // 保存现有图片
        List<Bitmap> existingImages = new ArrayList<>();
        for (PuzzleImageCell cell : imageCells) {
            if (cell.hasImage()) {
                existingImages.add(cell.getBitmap());
            }
        }
        
        // 重新创建imageCells
        imageCells.clear();
        for (RectF bound : bounds) {
            imageCells.add(new PuzzleImageCell(bound));
        }
        
        // 恢复图片
        int imageIndex = 0;
        for (int i = 0; i < imageCells.size() && imageIndex < existingImages.size(); i++) {
            imageCells.get(i).setBitmap(existingImages.get(imageIndex++));
        }
        
        invalidate();
    }
    
    /**
     * 使用布局变体设置布局
     */
    public void setLayoutVariant(PuzzleLayoutEnhanced.LayoutVariant variant) {
        setLayoutFromCells(variant.getGridCells());
    }
    
    /**
     * 为指定格子设置图片
     */
    public void setImageForCell(int cellIndex, Bitmap bitmap) {
        if (cellIndex >= 0 && cellIndex < imageCells.size()) {
            imageCells.get(cellIndex).setBitmap(bitmap);
            invalidate();
        }
    }
    
    /**
     * 添加图片到第一个空格子
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
     * 批量设置图片
     */
    public void setImages(List<Bitmap> bitmaps) {
        for (int i = 0; i < Math.min(bitmaps.size(), imageCells.size()); i++) {
            imageCells.get(i).setBitmap(bitmaps.get(i));
        }
        invalidate();
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
     * 查找指定坐标下的格子索引
     */
    private int findCellIndexAt(float x, float y) {
        for (int i = 0; i < imageCells.size(); i++) {
            if (imageCells.get(i).contains(x, y)) {
                return i;
            }
        }
        return -1;
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
        
        canvas.drawColor(backgroundColor);
        
        for (PuzzleImageCell cell : imageCells) {
            if (cell.hasImage()) {
                // 绘制有图片的格子
                cell.draw(canvas, paint);
            } else {
                // 绘制空格子 + "+" 号
                RectF bounds = cell.getBounds();
                canvas.drawRect(bounds, bgPaint);
                
                // 绘制 "+" 号
                float centerX = bounds.centerX();
                float centerY = bounds.centerY();
                float size = Math.min(bounds.width(), bounds.height()) * 0.15f;
                
                // 水平线
                canvas.drawLine(centerX - size, centerY, centerX + size, centerY, plusPaint);
                // 垂直线
                canvas.drawLine(centerX, centerY - size, centerX, centerY + size, plusPaint);
            }
            
            // 绘制边框
            if (borderWidth > 0) {
                canvas.drawRect(cell.getBounds(), borderPaint);
            }
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        if (canvasAspectRatio > 0) {
            // 应用固定比例
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            
            float currentRatio = (float) width / height;
            
            if (currentRatio > canvasAspectRatio) {
                // 太宽，限制宽度
                width = (int) (height * canvasAspectRatio);
            } else {
                // 太高，限制高度
                height = (int) (width / canvasAspectRatio);
            }
            
            setMeasuredDimension(width, height);
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
        // 重新应用当前布局
        if (!gridCells.isEmpty()) {
            setLayoutFromCells(gridCells);
        }
    }
    
    public void setCanvasAspectRatio(float aspectRatio) {
        this.canvasAspectRatio = aspectRatio;
        requestLayout();  // 触发重新测量
        invalidate();
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
