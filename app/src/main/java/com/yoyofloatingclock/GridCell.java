package com.yoyofloatingclock;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * 网格单元格 - 支持行列跨度
 */
public class GridCell implements Parcelable {
    private final int rowStart;
    private final int rowEnd;
    private final int colStart;
    private final int colEnd;
    
    private RectF bounds;  // 实际绘制的边界
    
    public GridCell(int rowStart, int rowEnd, int colStart, int colEnd) {
        this.rowStart = rowStart;
        this.rowEnd = rowEnd;
        this.colStart = colStart;
        this.colEnd = colEnd;
    }
    
    // Parcelable implementation
    protected GridCell(Parcel in) {
        rowStart = in.readInt();
        rowEnd = in.readInt();
        colStart = in.readInt();
        colEnd = in.readInt();
        bounds = in.readParcelable(RectF.class.getClassLoader());
    }
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(rowStart);
        dest.writeInt(rowEnd);
        dest.writeInt(colStart);
        dest.writeInt(colEnd);
        dest.writeParcelable(bounds, flags);
    }
    
    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Creator<GridCell> CREATOR = new Creator<GridCell>() {
        @Override
        public GridCell createFromParcel(Parcel in) {
            return new GridCell(in);
        }
        
        @Override
        public GridCell[] newArray(int size) {
            return new GridCell[size];
        }
    };
    
    // Getters
    public int getRowStart() { return rowStart; }
    public int getRowEnd() { return rowEnd; }
    public int getColStart() { return colStart; }
    public int getColEnd() { return colEnd; }
    public int getRowSpan() { return rowEnd - rowStart; }
    public int getColSpan() { return colEnd - colStart; }
    
    public RectF getBounds() { return bounds; }
    public void setBounds(RectF bounds) { this.bounds = bounds; }
}
