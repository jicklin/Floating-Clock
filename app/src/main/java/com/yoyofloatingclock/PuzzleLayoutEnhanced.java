package com.yoyofloatingclock;

import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;

/**
 * 增强的拼图布局系统，支持多种布局变体和网格合并
 */
public class PuzzleLayoutEnhanced {
    
    /**
     * 拼图模式
     */
    public enum PuzzleMode {
        GRID,          // 网格拼图
        STITCH_H,      // 横向拼接
        STITCH_V       // 纵向拼接
    }
    
    /**
     * 布局变体定义
     */
    public enum LayoutVariant {
        // === 2张图片布局 ===
        GRID_2_H("2张-横向", 2, createGrid2H()),
        GRID_2_V("2张-纵向", 2, createGrid2V()),
        GRID_2_LEFT_LARGE("2张-左大", 2, createGrid2LeftLarge()),
        GRID_2_RIGHT_LARGE("2张-右大", 2, createGrid2RightLarge()),
        GRID_2_TOP_LARGE("2张-上大", 2, createGrid2TopLarge()),
        GRID_2_BOTTOM_LARGE("2张-下大", 2, createGrid2BottomLarge()),
        
        // === 3张图片布局 ===
        GRID_3_VERTICAL("3张-三列", 3, createGrid3Vertical()),
        GRID_3_HORIZONTAL("3张-三行", 3, createGrid3Horizontal()),
        GRID_3_TOP_ONE("3张-上1下2", 3, createGrid3TopOne()),
        GRID_3_BOTTOM_ONE("3张-上2下1", 3, createGrid3BottomOne()),
        GRID_3_LEFT_ONE("3张-左1右2", 3, createGrid3LeftOne()),
        GRID_3_RIGHT_ONE("3张-左2右1", 3, createGrid3RightOne()),
        GRID_3_LEFT_LARGE("3张-左大右2", 3, createGrid3LeftLarge()),
        GRID_3_RIGHT_LARGE("3张-右2左大", 3, createGrid3RightLarge()),
        
        // === 4张图片布局 ===
        GRID_4_SQUARE("4张-方形", 4, createGrid4Square()),
        GRID_4_VERTICAL("4张-四列", 4, createGrid4Vertical()),
        GRID_4_HORIZONTAL("4张-四行", 4, createGrid4Horizontal()),
        GRID_4_TOP_LARGE("4张-上大", 4, createGrid4TopLarge()),
        GRID_4_BOTTOM_LARGE("4张-下大", 4, createGrid4BottomLarge()),
        GRID_4_LEFT_LARGE("4张-左大", 4, createGrid4LeftLarge()),
        GRID_4_RIGHT_LARGE("4张-右大", 4, createGrid4RightLarge()),
        GRID_4_CENTER_LARGE("4张-中大", 4, createGrid4CenterLarge()),
        GRID_4_LEFT_COL("4张-左列3", 4, createGrid4LeftCol()),
        GRID_4_RIGHT_COL("4张-右列3", 4, createGrid4RightCol()),
        
        // === 5张图片布局 ===
        GRID_5_TOP_ONE("5张-上1下4", 5, createGrid5TopOne()),
        GRID_5_BOTTOM_ONE("5张-上4下1", 5, createGrid5BottomOne()),
        GRID_5_LEFT_ONE("5张-左1右4", 5, createGrid5LeftOne()),
        GRID_5_RIGHT_ONE("5张-左4右1", 5, createGrid5RightOne()),
        GRID_5_CENTER("5张-中心1", 5, createGrid5Center()),
        GRID_5_CROSS("5张-十字", 5, createGrid5Cross()),
        GRID_5_T_SHAPE("5张-T型", 5, createGrid5TShape()),
        GRID_5_L_SHAPE("5张-L型", 5, createGrid5LShape()),
        
        // === 6张图片布局 ===
        GRID_6_2X3("6张-2x3", 6, createGrid6_2x3()),
        GRID_6_3X2("6张-3x2", 6, createGrid6_3x2()),
        GRID_6_LEFT_LARGE("6张-左大", 6, createGrid6LeftLarge()),
        GRID_6_RIGHT_LARGE("6张-右大", 6, createGrid6RightLarge()),
        GRID_6_TOP_LARGE("6张-上大下5", 6, createGrid6TopLarge()),
        GRID_6_BOTTOM_LARGE("6张-上5下大", 6, createGrid6BottomLarge()),
        
        // === 7张图片布局 ===
        GRID_7_TOP_ONE("7张-上1下6", 7, createGrid7TopOne()),
        GRID_7_CENTER_ONE("7张-中心1", 7, createGrid7CenterOne()),
        GRID_7_LEFT_COL("7张-左3右4", 7, createGrid7LeftCol()),
        GRID_7_RIGHT_COL("7张-左4右3", 7, createGrid7RightCol()),
        GRID_7_TOP_ROW("7张-上3下4", 7, createGrid7TopRow()),
        GRID_7_BOTTOM_ROW("7张-上4下3", 7, createGrid7BottomRow()),
        
        // === 8张图片布局 ===
        GRID_8_2X4("8张-2x4", 8, createGrid8_2x4()),
        GRID_8_4X2("8张-4x2", 8, createGrid8_4x2()),
        GRID_8_BORDER("8张-边框", 8, createGrid8Border()),
        GRID_8_CENTER_LARGE("8张-中大", 8, createGrid8CenterLarge()),
        GRID_8_LEFT_LARGE("8张-左大", 8, createGrid8LeftLarge()),
        GRID_8_RIGHT_LARGE("8张-右大", 8, createGrid8RightLarge()),
        
        // === 9张图片布局 ===
        GRID_9_SQUARE("9张-方形", 9, createGrid9Square());
        
        private final String displayName;
        private final int imageCount;
        private final List<GridCell> gridCells;
        
        LayoutVariant(String displayName, int imageCount, List<GridCell> gridCells) {
            this.displayName = displayName;
            this.imageCount = imageCount;
            this.gridCells = gridCells;
        }
        
        public String getDisplayName() { return displayName; }
        public int getImageCount() { return imageCount; }
        public List<GridCell> getGridCells() { return gridCells; }
    }
    
    // ==================== 2张图片布局定义 ====================
    
    private static List<GridCell> createGrid2H() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 左
        cells.add(new GridCell(0, 1, 1, 2));  // 右
        return cells;
    }
    
    private static List<GridCell> createGrid2V() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 2));  // 上
        cells.add(new GridCell(1, 2, 0, 2));  // 下
        return cells;
    }
    
    private static List<GridCell> createGrid2LeftLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 2, 0, 2));  // 左大（占2列）
        cells.add(new GridCell(0, 2, 2, 3));  // 右小
        return cells;
    }
    
    private static List<GridCell> createGrid2RightLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 2, 0, 1));  // 左小
        cells.add(new GridCell(0, 2, 1, 3));  // 右大
        return cells;
    }
    
    private static List<GridCell> createGrid2TopLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 2, 0, 2));  // 上大
        cells.add(new GridCell(2, 3, 0, 2));  // 下小
        return cells;
    }
    
    private static List<GridCell> createGrid2BottomLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 2));  // 上小
        cells.add(new GridCell(1, 3, 0, 2));  // 下大
        return cells;
    }
    
    // ==================== 3张图片布局定义 ====================
    
    private static List<GridCell> createGrid3Vertical() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));
        cells.add(new GridCell(0, 1, 1, 2));
        cells.add(new GridCell(0, 1, 2, 3));
        return cells;
    }
    
    private static List<GridCell> createGrid3Horizontal() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));
        cells.add(new GridCell(1, 2, 0, 1));
        cells.add(new GridCell(2, 3, 0, 1));
        return cells;
    }
    
    private static List<GridCell> createGrid3TopOne() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 2));  // 上1
        cells.add(new GridCell(1, 2, 0, 1));  // 下左
        cells.add(new GridCell(1, 2, 1, 2));  // 下右
        return cells;
    }
    
    private static List<GridCell> createGrid3BottomOne() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 上左
        cells.add(new GridCell(0, 1, 1, 2));  // 上右
        cells.add(new GridCell(1, 2, 0, 2));  // 下1
        return cells;
    }
    
    private static List<GridCell> createGrid3LeftOne() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 2, 0, 1));  // 左1
        cells.add(new GridCell(0, 1, 1, 2));  // 右上
        cells.add(new GridCell(1, 2, 1, 2));  // 右下
        return cells;
    }
    
    private static List<GridCell> createGrid3RightOne() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 左上
        cells.add(new GridCell(1, 2, 0, 1));  // 左下
        cells.add(new GridCell(0, 2, 1, 2));  // 右1
        return cells;
    }
    
    private static List<GridCell> createGrid3LeftLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 2, 0, 2));  // 左大
        cells.add(new GridCell(0, 1, 2, 3));  // 右上小
        cells.add(new GridCell(1, 2, 2, 3));  // 右下小
        return cells;
    }
    
    private static List<GridCell> createGrid3RightLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 左上小
        cells.add(new GridCell(1, 2, 0, 1));  // 左下小
        cells.add(new GridCell(0, 2, 1, 3));  // 右大
        return cells;
    }
    
    // ==================== 4张图片布局定义 ====================
    
    private static List<GridCell> createGrid4Square() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));
        cells.add(new GridCell(0, 1, 1, 2));
        cells.add(new GridCell(1, 2, 0, 1));
        cells.add(new GridCell(1, 2, 1, 2));
        return cells;
    }
    
    private static List<GridCell> createGrid4Vertical() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));
        cells.add(new GridCell(0, 1, 1, 2));
        cells.add(new GridCell(0, 1, 2, 3));
        cells.add(new GridCell(0, 1, 3, 4));
        return cells;
    }
    
    private static List<GridCell> createGrid4Horizontal() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));
        cells.add(new GridCell(1, 2, 0, 1));
        cells.add(new GridCell(2, 3, 0, 1));
        cells.add(new GridCell(3, 4, 0, 1));
        return cells;
    }
    
    private static List<GridCell> createGrid4TopLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 2, 0, 3));  // 上大
        cells.add(new GridCell(2, 3, 0, 1));  // 下左
        cells.add(new GridCell(2, 3, 1, 2));  // 下中
        cells.add(new GridCell(2, 3, 2, 3));  // 下右
        return cells;
    }
    
    private static List<GridCell> createGrid4BottomLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 上左
        cells.add(new GridCell(0, 1, 1, 2));  // 上中
        cells.add(new GridCell(0, 1, 2, 3));  // 上右
        cells.add(new GridCell(1, 3, 0, 3));  // 下大
        return cells;
    }
    
    private static List<GridCell> createGrid4LeftLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 3, 0, 2));  // 左大
        cells.add(new GridCell(0, 1, 2, 3));  // 右上
        cells.add(new GridCell(1, 2, 2, 3));  // 右中
        cells.add(new GridCell(2, 3, 2, 3));  // 右下
        return cells;
    }
    
    private static List<GridCell> createGrid4RightLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 左上
        cells.add(new GridCell(1, 2, 0, 1));  // 左中
        cells.add(new GridCell(2, 3, 0, 1));  // 左下
        cells.add(new GridCell(0, 3, 1, 3));  // 右大
        return cells;
    }
    
    private static List<GridCell> createGrid4CenterLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 2));  // 上
        cells.add(new GridCell(1, 2, 0, 1));  // 左
        cells.add(new GridCell(1, 2, 1, 2));  // 右
        cells.add(new GridCell(2, 3, 0, 2));  // 下
        return cells;
    }
    
    private static List<GridCell> createGrid4LeftCol() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 左上
        cells.add(new GridCell(1, 2, 0, 1));  // 左中
        cells.add(new GridCell(2, 3, 0, 1));  // 左下
        cells.add(new GridCell(0, 3, 1, 2));  // 右大
        return cells;
    }
    
    private static List<GridCell> createGrid4RightCol() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 3, 0, 1));  // 左大
        cells.add(new GridCell(0, 1, 1, 2));  // 右上
        cells.add(new GridCell(1, 2, 1, 2));  // 右中
        cells.add(new GridCell(2, 3, 1, 2));  // 右下
        return cells;
    }
    
    // ==================== 5张图片布局定义 ====================
    
    private static List<GridCell> createGrid5TopOne() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 4));  // 上1大
        cells.add(new GridCell(1, 2, 0, 1));  // 下左1
        cells.add(new GridCell(1, 2, 1, 2));  // 下左2
        cells.add(new GridCell(1, 2, 2, 3));  // 下右1
        cells.add(new GridCell(1, 2, 3, 4));  // 下右2
        return cells;
    }
    
    private static List<GridCell> createGrid5BottomOne() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 上左1
        cells.add(new GridCell(0, 1, 1, 2));  // 上左2
        cells.add(new GridCell(0, 1, 2, 3));  // 上右1
        cells.add(new GridCell(0, 1, 3, 4));  // 上右2
        cells.add(new GridCell(1, 2, 0, 4));  // 下1大
        return cells;
    }
    
    private static List<GridCell> createGrid5LeftOne() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 4, 0, 1));  // 左1大
        cells.add(new GridCell(0, 1, 1, 2));  // 右上1
        cells.add(new GridCell(1, 2, 1, 2));  // 右上2
        cells.add(new GridCell(2, 3, 1, 2));  // 右下1
        cells.add(new GridCell(3, 4, 1, 2));  // 右下2
        return cells;
    }
    
    private static List<GridCell> createGrid5RightOne() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 左上1
        cells.add(new GridCell(1, 2, 0, 1));  // 左上2
        cells.add(new GridCell(2, 3, 0, 1));  // 左下1
        cells.add(new GridCell(3, 4, 0, 1));  // 左下2
        cells.add(new GridCell(0, 4, 1, 2));  // 右1大
        return cells;
    }
    
    private static List<GridCell> createGrid5Center() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(1, 2, 1, 2));  // 中心
        cells.add(new GridCell(0, 1, 0, 3));  // 上
        cells.add(new GridCell(0, 3, 0, 1));  // 左
        cells.add(new GridCell(0, 3, 2, 3));  // 右
        cells.add(new GridCell(2, 3, 0, 3));  // 下
        return cells;
    }
    
    private static List<GridCell> createGrid5Cross() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 1, 2));  // 上
        cells.add(new GridCell(1, 2, 0, 1));  // 左
        cells.add(new GridCell(1, 2, 1, 2));  // 中
        cells.add(new GridCell(1, 2, 2, 3));  // 右
        cells.add(new GridCell(2, 3, 1, 2));  // 下
        return cells;
    }
    
    private static List<GridCell> createGrid5TShape() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 上左
        cells.add(new GridCell(0, 1, 1, 2));  // 上中
        cells.add(new GridCell(0, 1, 2, 3));  // 上右
        cells.add(new GridCell(1, 3, 0, 2));  // 下左
        cells.add(new GridCell(1, 3, 2, 3));  // 下右
        return cells;
    }
    
    private static List<GridCell> createGrid5LShape() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 2, 0, 2));  // 左上大
        cells.add(new GridCell(0, 1, 2, 3));  // 右上
        cells.add(new GridCell(1, 2, 2, 3));  // 右中
        cells.add(new GridCell(2, 3, 0, 1));  // 下左
        cells.add(new GridCell(2, 3, 1, 3));  // 下右大
        return cells;
    }
    
    // ==================== 6张图片布局定义 ====================
    
    private static List<GridCell> createGrid6_2x3() {
        List<GridCell> cells = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 2; col++) {
                cells.add(new GridCell(row, row + 1, col, col + 1));
            }
        }
        return cells;
    }
    
    private static List<GridCell> createGrid6_3x2() {
        List<GridCell> cells = new ArrayList<>();
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                cells.add(new GridCell(row, row + 1, col, col + 1));
            }
        }
        return cells;
    }
    
    private static List<GridCell> createGrid6LeftLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 2, 0, 2));  // 左大
        cells.add(new GridCell(0, 1, 2, 3));  // 右上1
        cells.add(new GridCell(0, 1, 3, 4));  // 右上2
        cells.add(new GridCell(1, 2, 2, 3));  // 右中1
        cells.add(new GridCell(1, 2, 3, 4));  // 右中2
        cells.add(new GridCell(2, 3, 0, 4));  // 下
        return cells;
    }
    
    private static List<GridCell> createGrid6RightLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 左上1
        cells.add(new GridCell(0, 1, 1, 2));  // 左上2
        cells.add(new GridCell(1, 2, 0, 1));  // 左中1
        cells.add(new GridCell(1, 2, 1, 2));  // 左中2
        cells.add(new GridCell(0, 2, 2, 4));  // 右大
        cells.add(new GridCell(2, 3, 0, 4));  // 下
        return cells;
    }
    
    private static List<GridCell> createGrid6TopLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 2, 0, 3));  // 上大
        cells.add(new GridCell(2, 3, 0, 1));  // 下1
        cells.add(new GridCell(2, 3, 1, 2));  // 下2
        cells.add(new GridCell(2, 3, 2, 3));  // 下3
        cells.add(new GridCell(2, 3, 3, 4));  // 下4
        cells.add(new GridCell(2, 3, 4, 5));  // 下5
        return cells;
    }
    
    private static List<GridCell> createGrid6BottomLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 上1
        cells.add(new GridCell(0, 1, 1, 2));  // 上2
        cells.add(new GridCell(0, 1, 2, 3));  // 上3
        cells.add(new GridCell(0, 1, 3, 4));  // 上4
        cells.add(new GridCell(0, 1, 4, 5));  // 上5
        cells.add(new GridCell(1, 3, 0, 5));  // 下大
        return cells;
    }
    
    // ==================== 7张图片布局定义 ====================
    
    private static List<GridCell> createGrid7TopOne() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 6));  // 上1大
        cells.add(new GridCell(1, 2, 0, 1));  // 下1
        cells.add(new GridCell(1, 2, 1, 2));  // 下2
        cells.add(new GridCell(1, 2, 2, 3));  // 下3
        cells.add(new GridCell(1, 2, 3, 4));  // 下4
        cells.add(new GridCell(1, 2, 4, 5));  // 下5
        cells.add(new GridCell(1, 2, 5, 6));  // 下6
        return cells;
    }
    
    private static List<GridCell> createGrid7CenterOne() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(1, 2, 1, 2));  // 中心大
        cells.add(new GridCell(0, 1, 0, 3));  // 上
        cells.add(new GridCell(0, 3, 0, 1));  // 左上
        cells.add(new GridCell(0, 3, 2, 3));  // 右上
        cells.add(new GridCell(2, 3, 0, 1));  // 左下
        cells.add(new GridCell(2, 3, 1, 2));  // 中下
        cells.add(new GridCell(2, 3, 2, 3));  // 右下
        return cells;
    }
    
    private static List<GridCell> createGrid7LeftCol() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 左1
        cells.add(new GridCell(1, 2, 0, 1));  // 左2
        cells.add(new GridCell(2, 3, 0, 1));  // 左3
        cells.add(new GridCell(0, 1, 1, 2));  // 右上1
        cells.add(new GridCell(0, 1, 2, 3));  // 右上2
        cells.add(new GridCell(1, 3, 1, 2));  // 右下左
        cells.add(new GridCell(1, 3, 2, 3));  // 右下右
        return cells;
    }
    
    private static List<GridCell> createGrid7RightCol() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 左上1
        cells.add(new GridCell(0, 1, 1, 2));  // 左上2
        cells.add(new GridCell(1, 3, 0, 1));  // 左下左
        cells.add(new GridCell(1, 3, 1, 2));  // 左下右
        cells.add(new GridCell(0, 1, 2, 3));  // 右1
        cells.add(new GridCell(1, 2, 2, 3));  // 右2
        cells.add(new GridCell(2, 3, 2, 3));  // 右3
        return cells;
    }
    
    private static List<GridCell> createGrid7TopRow() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 上1
        cells.add(new GridCell(0, 1, 1, 2));  // 上2
        cells.add(new GridCell(0, 1, 2, 3));  // 上3
        cells.add(new GridCell(1, 2, 0, 2));  // 中左
        cells.add(new GridCell(1, 2, 2, 3));  // 中右
        cells.add(new GridCell(2, 3, 0, 2));  // 下左
        cells.add(new GridCell(2, 3, 2, 3));  // 下右
        return cells;
    }
    
    private static List<GridCell> createGrid7BottomRow() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 2));  // 上左
        cells.add(new GridCell(0, 1, 2, 3));  // 上右
        cells.add(new GridCell(1, 2, 0, 2));  // 中左
        cells.add(new GridCell(1, 2, 2, 3));  // 中右
        cells.add(new GridCell(2, 3, 0, 1));  // 下1
        cells.add(new GridCell(2, 3, 1, 2));  // 下2
        cells.add(new GridCell(2, 3, 2, 3));  // 下3
        return cells;
    }
    
    // ==================== 8张图片布局定义 ====================
    
    private static List<GridCell> createGrid8_2x4() {
        List<GridCell> cells = new ArrayList<>();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 2; col++) {
                cells.add(new GridCell(row, row + 1, col, col + 1));
            }
        }
        return cells;
    }
    
    private static List<GridCell> createGrid8_4x2() {
        List<GridCell> cells = new ArrayList<>();
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                cells.add(new GridCell(row, row + 1, col, col + 1));
            }
        }
        return cells;
    }
    
    private static List<GridCell> createGrid8Border() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 左上
        cells.add(new GridCell(0, 1, 1, 2));  // 上中
        cells.add(new GridCell(0, 1, 2, 3));  // 右上
        cells.add(new GridCell(1, 2, 0, 1));  // 左
        cells.add(new GridCell(1, 2, 2, 3));  // 右
        cells.add(new GridCell(2, 3, 0, 1));  // 左下
        cells.add(new GridCell(2, 3, 1, 2));  // 下中
        cells.add(new GridCell(2, 3, 2, 3));  // 右下
        return cells;
    }
    
    private static List<GridCell> createGrid8CenterLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 1, 2));  // 上
        cells.add(new GridCell(0, 2, 0, 1));  // 左上
        cells.add(new GridCell(1, 3, 1, 3));  // 中大
        cells.add(new GridCell(0, 2, 3, 4));  // 右上
        cells.add(new GridCell(2, 3, 0, 1));  // 左下
       cells.add(new GridCell(3, 4, 0, 2));  // 下左
        cells.add(new GridCell(3, 4, 2, 4));  // 下右
        cells.add(new GridCell(2, 3, 3, 4));  // 右下
        return cells;
    }
    
    private static List<GridCell> createGrid8LeftLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 3, 0, 2));  // 左大
        cells.add(new GridCell(0, 1, 2, 3));  // 右上1
        cells.add(new GridCell(0, 1, 3, 4));  // 右上2
        cells.add(new GridCell(1, 2, 2, 3));  // 右中1
        cells.add(new GridCell(1, 2, 3, 4));  // 右中2
        cells.add(new GridCell(2, 3, 2, 3));  // 右下1
        cells.add(new GridCell(2, 3, 3, 4));  // 右下2
        cells.add(new GridCell(3, 4, 0, 4));  // 最下
        return cells;
    }
    
    private static List<GridCell> createGrid8RightLarge() {
        List<GridCell> cells = new ArrayList<>();
        cells.add(new GridCell(0, 1, 0, 1));  // 左上1
        cells.add(new GridCell(0, 1, 1, 2));  // 左上2
        cells.add(new GridCell(1, 2, 0, 1));  // 左中1
        cells.add(new GridCell(1, 2, 1, 2));  // 左中2
        cells.add(new GridCell(2, 3, 0, 1));  // 左下1
        cells.add(new GridCell(2, 3, 1, 2));  // 左下2
        cells.add(new GridCell(0, 3, 2, 4));  // 右大
        cells.add(new GridCell(3, 4, 0, 4));  // 最下
        return cells;
    }
    
    // ==================== 9张图片布局定义 ====================
    
    private static List<GridCell> createGrid9Square() {
        List<GridCell> cells = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                cells.add(new GridCell(row, row + 1, col, col + 1));
            }
        }
        return cells;
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 根据图片数量获取所有适用的布局变体
     */
    public static List<LayoutVariant> getVariantsForCount(int imageCount) {
        List<LayoutVariant> variants = new ArrayList<>();
        for (LayoutVariant variant : LayoutVariant.values()) {
            if (variant.getImageCount() == imageCount) {
                variants.add(variant);
            }
        }
        return variants;
    }
    
    /**
     * 计算所有格子的实际bounds
     */
    public static List<RectF> calculateBounds(List<GridCell> gridCells, int canvasWidth, int canvasHeight, int spacing) {
        if (gridCells.isEmpty()) return new ArrayList<>();
        
        // 找出最大行列数
        int maxRow = 0, maxCol = 0;
        for (GridCell cell : gridCells) {
            maxRow = Math.max(maxRow, cell.getRowEnd());
            maxCol = Math.max(maxCol, cell.getColEnd());
        }
        
        // 计算每个单元格的大小
        float cellWidth = (canvasWidth - spacing * (maxCol + 1)) / (float) maxCol;
        float cellHeight = (canvasHeight - spacing * (maxRow + 1)) / (float) maxRow;
        
        // 计算每个GridCell的bounds
        List<RectF> bounds = new ArrayList<>();
        for (GridCell cell : gridCells) {
            float left = spacing + cell.getColStart() * (cellWidth + spacing);
            float top = spacing + cell.getRowStart() * (cellHeight + spacing);
            float right = left + cell.getColSpan() * cellWidth + (cell.getColSpan() - 1) * spacing;
            float bottom = top + cell.getRowSpan() * cellHeight + (cell.getRowSpan() - 1) * spacing;
            
            bounds.add(new RectF(left, top, right, bottom));
        }
        
        return bounds;
    }
}
