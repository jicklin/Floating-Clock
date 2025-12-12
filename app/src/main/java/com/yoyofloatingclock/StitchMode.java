package com.yoyofloatingclock;

/**
 * 拼接模式枚举
 */
public enum StitchMode {
    HORIZONTAL("横向拼接"),
    VERTICAL("纵向拼接");
    
    private final String displayName;
    
    StitchMode(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
