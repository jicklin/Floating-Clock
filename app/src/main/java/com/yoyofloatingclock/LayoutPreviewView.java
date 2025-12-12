package com.yoyofloatingclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

/**
 * 布局预览View - 显示网格划分
 */
public class LayoutPreviewView extends View {
    
    private List<GridCell> gridCells;
    private Paint linePaint;
    private Paint bgPaint;
    
    public LayoutPreviewView(Context context) {
        this(context, null);
    }
    
    public LayoutPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.WHITE);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2);
        
        bgPaint = new Paint();
        bgPaint.setColor(0xFF888888);  // 灰色背景
    }
    
    public void setGridCells(List<GridCell> cells) {
        this.gridCells = cells;
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (gridCells == null || gridCells.isEmpty()) {
            // 绘制单色背景
            canvas.drawColor(0xFF888888);
            return;
        }
        
        int width = getWidth();
        int height = getHeight();
        
        // 计算bounds
        List<RectF> bounds = PuzzleLayoutEnhanced.calculateBounds(
            gridCells, width, height, 2  // 使用小间距
        );
        
        // 绘制每个格子
        for (RectF bound : bounds) {
            canvas.drawRect(bound, bgPaint);
            canvas.drawRect(bound, linePaint);
        }
    }
}
