package com.yoyofloatingclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 网格编辑器View - 用于自定义布局编辑
 */
public class GridEditorView extends View {
    
    private int rows = 6;
    private int columns = 6;
    
    private Paint gridPaint;
    private Paint selectedPaint;
    private Paint mergePaint;
    private Paint numberPaint;
    
    private List<GridCell> cells = new ArrayList<>();
    private Set<Integer> selectedCellIndices = new HashSet<>();
    private boolean[][] cellOccupied;  // 标记哪些网格被占用
    
    private int cellCounter = 1;  // 用于给格子编号
    
    public GridEditorView(Context context) {
        this(context, null);
    }
    
    public GridEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0xFFCCCCCC);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2);
        
        selectedPaint = new Paint();
        selectedPaint.setColor(0x66FF9800);  // 半透明橙色
        
        mergePaint = new Paint();
        mergePaint.setColor(0xFF2196F3);  // 蓝色
        
        numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numberPaint.setColor(Color.WHITE);
        numberPaint.setTextSize(32);
        numberPaint.setTextAlign(Paint.Align.CENTER);
        
        initCellOccupied();
    }
    
    private void initCellOccupied() {
        cellOccupied = new boolean[rows][columns];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                cellOccupied[r][c] = false;
            }
        }
    }
    
    public void setGridSize(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        initCellOccupied();
        cells.clear();
        selectedCellIndices.clear();
        cellCounter = 1;
        invalidate();
    }
    
    public int getRows() { return rows; }
    public int getColumns() { return columns; }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        
        float cellWidth = width / (float) columns;
        float cellHeight = height / (float) rows;
        
        // 绘制网格线
        for (int i = 0; i <= rows; i++) {
            float y = i * cellHeight;
            canvas.drawLine(0, y, width, y, gridPaint);
        }
        for (int i = 0; i <= columns; i++) {
            float x = i * cellWidth;
            canvas.drawLine(x, 0, x, height, gridPaint);
        }
        
        // 绘制已合并的格子
        for (int i = 0; i < cells.size(); i++) {
            GridCell cell = cells.get(i);
            RectF rect = new RectF(
                cell.getColStart() * cellWidth,
                cell.getRowStart() * cellHeight,
                cell.getColEnd() * cellWidth,
                cell.getRowEnd() * cellHeight
            );
            canvas.drawRect(rect, mergePaint);
            
            // 绘制格子编号
            String number = String.valueOf(i + 1);
            float textX = rect.centerX();
            float textY = rect.centerY() - (numberPaint.descent() + numberPaint.ascent()) / 2;
            canvas.drawText(number, textX, textY, numberPaint);
        }
        
        // 绘制选中的单元格
        for (int index : selectedCellIndices) {
            int row = index / columns;
            int col = index % columns;
            
            if (!cellOccupied[row][col]) {
                RectF rect = new RectF(
                    col * cellWidth,
                    row * cellHeight,
                    (col + 1) * cellWidth,
                    (row + 1) * cellHeight
                );
                canvas.drawRect(rect, selectedPaint);
            }
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || 
            event.getAction() == MotionEvent.ACTION_MOVE) {
            
            float x = event.getX();
            float y = event.getY();
            
            int col = (int) (x / getWidth() * columns);
            int row = (int) (y / getHeight() * rows);
            
            if (row >= 0 && row < rows && col >= 0 && col < columns) {
                int index = row * columns + col;
                
                if (!cellOccupied[row][col]) {
                    if (selectedCellIndices.contains(index)) {
                        selectedCellIndices.remove(index);
                    } else {
                        selectedCellIndices.add(index);
                    }
                    invalidate();
                }
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
    
    /**
     * 合并选中的格子
     */
    public boolean mergeSelectedCells() {
        if (selectedCellIndices.size() < 2) {
            return false;
        }
        
        // 找出选中格子的边界
        int minRow = rows, maxRow = -1;
        int minCol = columns, maxCol = -1;
        
        for (int index : selectedCellIndices) {
            int row = index / columns;
            int col = index % columns;
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
        }
        
        // 检查是否形成矩形
        int expectedCells = (maxRow - minRow + 1) * (maxCol - minCol + 1);
        if (selectedCellIndices.size() != expectedCells) {
            return false;  // 不是矩形区域
        }
        
        // 创建合并后的格子
        GridCell mergedCell = new GridCell(minRow, maxRow + 1, minCol, maxCol + 1);
        cells.add(mergedCell);
        
        // 标记这些格子为已占用
        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                cellOccupied[r][c] = true;
            }
        }
        
        selectedCellIndices.clear();
        invalidate();
        return true;
    }
    
    /**
     * 删除最后一个合并的格子
     */
    public void deleteLastCell() {
        if (cells.isEmpty()) return;
        
        GridCell lastCell = cells.remove(cells.size() - 1);
        
        // 取消占用标记
        for (int r = lastCell.getRowStart(); r < lastCell.getRowEnd(); r++) {
            for (int c = lastCell.getColStart(); c < lastCell.getColEnd(); c++) {
                cellOccupied[r][c] = false;
            }
        }
        
        invalidate();
    }
    
    /**
     * 重置所有格子
     */
    public void reset() {
        cells.clear();
        selectedCellIndices.clear();
        initCellOccupied();
        cellCounter = 1;
        invalidate();
    }
    
    /**
     * 自动填充剩余空格
     */
    public void autoFill() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (!cellOccupied[r][c]) {
                    GridCell cell = new GridCell(r, r + 1, c, c + 1);
                    cells.add(cell);
                    cellOccupied[r][c] = true;
                }
            }
        }
        invalidate();
    }
    
    /**
     * 获取所有格子
     */
    public List<GridCell> getCells() {
        return new ArrayList<>(cells);
    }
    
    /**
     * 获取格子数量
     */
    public int getCellCount() {
        return cells.size();
    }
}
