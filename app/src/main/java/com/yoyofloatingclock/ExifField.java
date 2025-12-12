package com.yoyofloatingclock;

/**
 * EXIF字段枚举 - 可供用户选择的照片信息
 */
public enum ExifField {
    // 时间信息
    DATETIME("拍摄时间", "datetime"),
    DATETIME_MODIFIED("修改时间", "datetime_modified"),
    GPS_DATETIME("GPS时间", "gps_datetime"),
    
    // 位置信息
    GPS_LOCATION("GPS坐标", "gps_location"),
    GPS_ADDRESS("地址", "gps_address"),
    GPS_ALTITUDE("海拔", "gps_altitude"),
    
    // 设备信息
    MAKE("相机品牌", "make"),
    MODEL("相机型号", "model"),
    LENS_MODEL("镜头型号", "lens_model"),
    FOCAL_LENGTH("焦距", "focal_length"),
    
    // 拍摄参数
    APERTURE("光圈", "aperture"),
    SHUTTER_SPEED("快门", "shutter_speed"),
    ISO("ISO", "iso"),
    EXPOSURE_COMPENSATION("曝光补偿", "exposure_compensation"),
    WHITE_BALANCE("白平衡", "white_balance"),
    FLASH("闪光灯", "flash"),
    METERING_MODE("测光模式", "metering_mode"),
    
    // 图片属性
    IMAGE_SIZE("图片尺寸", "image_size"),
    RESOLUTION("分辨率", "resolution"),
    
    // 自定义
    CUSTOM_TEXT("自定义文本", "custom_text"),
    COPYRIGHT("版权信息", "copyright"),
    AUTHOR("作者", "author");
    
    private final String displayName;
    private final String key;
    
    ExifField(String displayName, String key) {
        this.displayName = displayName;
        this.key = key;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getKey() {
        return key;
    }
    
    /**
     * 默认选中的字段
     */
    public static ExifField[] getDefaultFields() {
        return new ExifField[]{
            DATETIME,
            GPS_ADDRESS,
            MODEL
        };
    }
    
    /**
     * 按类别分组
     */
    public static ExifField[][] getGroupedFields() {
        return new ExifField[][]{
            // 时间
            {DATETIME, DATETIME_MODIFIED, GPS_DATETIME},
            // 位置
            {GPS_LOCATION, GPS_ADDRESS, GPS_ALTITUDE},
            // 设备
            {MAKE, MODEL, LENS_MODEL, FOCAL_LENGTH},
            // 参数
            {APERTURE, SHUTTER_SPEED, ISO, EXPOSURE_COMPENSATION, WHITE_BALANCE, FLASH, METERING_MODE},
            // 属性
            {IMAGE_SIZE, RESOLUTION},
            // 自定义
            {CUSTOM_TEXT, COPYRIGHT, AUTHOR}
        };
    }
    
    public static String[] getGroupNames() {
        return new String[]{
            "时间信息",
            "位置信息",
            "设备信息",
            "拍摄参数",
            "图片属性",
            "自定义内容"
        };
    }
}
