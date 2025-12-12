package com.yoyofloatingclock;

import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;

/**
 * 拼图布局模板类
 */
public class PuzzleLayout {
    
    public enum LayoutType {
        SINGLE(1, "1张"),
        GRID_2(2, "2张"),
        GRID_3(3, "3张"),
        GRID_4(4, "4张"),
        GRID_6(6, "6张"),
        GRID_9(9, "9张");
        
        private final int imageCount;
        private final String displayName;
        
        LayoutType(int imageCount, String displayName) {
            this.imageCount = imageCount;
            this.displayName = displayName;
        }
        
        public int getImageCount() {
            return imageCount;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private LayoutType layoutType;
    private int canvasWidth;
    private int canvasHeight;
    private int spacing;  // 图片间距
    
    public PuzzleLayout(LayoutType layoutType, int canvasWidth, int canvasHeight, int spacing) {
        this.layoutType = layoutType;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.spacing = spacing;
    }
    
    /**
     * 计算所有图片格子的位置
     */
    public List<RectF> calculateCellBounds() {
        List<RectF> cells = new ArrayList<>();
        
        switch (layoutType) {
            case SINGLE:
                cells.add(new RectF(0, 0, canvasWidth, canvasHeight));
                break;
                
            case GRID_2:
                // 2张：左右布局
                calculateGrid(cells, 2, 1);
                break;
                
            case GRID_3:
                // 3张：上1下2布局
                float halfHeight = canvasHeight / 2f;
                cells.add(new RectF(spacing, spacing, 
                    canvasWidth - spacing, halfHeight - spacing / 2f));
                
                float halfWidth = canvasWidth / 2f;
                cells.add(new RectF(spacing, halfHeight + spacing / 2f, 
                    halfWidth - spacing / 2f, canvasHeight - spacing));
                cells.add(new RectF(halfWidth + spacing / 2f, halfHeight + spacing / 2f, 
                    canvasWidth - spacing, canvasHeight - spacing));
                break;
                
            case GRID_4:
                // 4张：2x2网格
                calculateGrid(cells, 2, 2);
                break;
                
            case GRID_6:
                // 6张：2x3网格
                calculateGrid(cells, 2, 3);
                break;
                
            case GRID_9:
                // 9张：3x3网格
                calculateGrid(cells, 3, 3);
                break;
        }
        
        return cells;
    }
    
    /**
     * 计算规则网格布局
     */
    private void calculateGrid(List<RectF> cells, int cols, int rows) {
        float cellWidth = (canvasWidth - spacing * (cols + 1)) / (float) cols;
        float cellHeight = (canvasHeight - spacing * (rows + 1)) / (float) rows;
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                float left = spacing + col * (cellWidth + spacing);
                float top = spacing + row * (cellHeight + spacing);
                float right = left + cellWidth;
                float bottom = top + cellHeight;
                
                cells.add(new RectF(left, top, right, bottom));
            }
        }
    }
    
    public LayoutType getLayoutType() {
        return layoutType;
    }
    
    public void setLayoutType(LayoutType layoutType) {
        this.layoutType = layoutType;
    }
    
    public void setCanvasSize(int width, int height) {
        this.canvasWidth = width;
        this.canvasHeight = height;
    }
    
    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }
    
    public int getSpacing() {
        return spacing;
    }
    
    public int getImageCount() {
        return layoutType.getImageCount();
    }
    
    /**
     * 根据图片数量获取推荐的布局类型
     */
    public static LayoutType getRecommendedLayout(int imageCount) {
        if (imageCount <= 1) return LayoutType.SINGLE;
        else if (imageCount <= 2) return LayoutType.GRID_2;
        else if (imageCount <= 3) return LayoutType.GRID_3;
        else if (imageCount <= 4) return LayoutType.GRID_4;
        else if (imageCount <= 6) return LayoutType.GRID_6;
        else return LayoutType.GRID_9;
    }
}
