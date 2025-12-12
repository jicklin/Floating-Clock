package com.yoyofloatingclock;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EXIF数据读取工具类
 */
public class ExifHelper {
    
    private final Context context;
    private ExifInterface exif;
    private final Map<ExifField, String> cachedData = new HashMap<>();
    
    public ExifHelper(Context context) {
        this.context = context;
    }
    
    /**
     * 从Uri加载EXIF数据
     */
    public boolean loadFromUri(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                exif = new ExifInterface(inputStream);
                inputStream.close();
                cachedData.clear();
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * 获取指定字段的值
     */
    public String getValue(ExifField field) {
        if (cachedData.containsKey(field)) {
            return cachedData.get(field);
        }
        
        if (exif == null) {
            return null;
        }
        
        String value = extractFieldValue(field);
        cachedData.put(field, value);
        return value;
    }
    
    private String extractFieldValue(ExifField field) {
        try {
            switch (field) {
                case DATETIME:
                    return formatDateTime(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));
                    
                case DATETIME_MODIFIED:
                    return formatDateTime(exif.getAttribute(ExifInterface.TAG_DATETIME));
                    
                case GPS_DATETIME:
                    return exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP);
                    
                case GPS_LOCATION:
                    double[] latLong = exif.getLatLong();
                    if (latLong != null) {
                        return String.format(Locale.US, "%.6f, %.6f", latLong[0], latLong[1]);
                    }
                    break;
                    
                case GPS_ADDRESS:
                    return getAddressFromGPS();
                    
                case GPS_ALTITUDE:
                    double altitude = exif.getAltitude(0);
                    if (altitude != 0) {
                        return String.format(Locale.US, "%.1fm", altitude);
                    }
                    break;
                    
                case MAKE:
                    return exif.getAttribute(ExifInterface.TAG_MAKE);
                    
                case MODEL:
                    return exif.getAttribute(ExifInterface.TAG_MODEL);
                    
                case LENS_MODEL:
                    return exif.getAttribute(ExifInterface.TAG_LENS_MODEL);
                    
                case FOCAL_LENGTH:
                    String focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH);
                    if (focal != null) {
                        return formatFocalLength(focal) + "mm";
                    }
                    break;
                    
                case APERTURE:
                    String aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER);
                    if (aperture != null) {
                        return "f/" + aperture;
                    }
                    break;
                    
                case SHUTTER_SPEED:
                    String exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
                    if (exposureTime != null) {
                        return formatShutterSpeed(exposureTime);
                    }
                    break;
                    
                case ISO:
                    String iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS);
                    if (iso != null) {
                        return "ISO " + iso;
                    }
                    break;
                    
                case EXPOSURE_COMPENSATION:
                    String expComp = exif.getAttribute(ExifInterface.TAG_EXPOSURE_BIAS_VALUE);
                    if (expComp != null) {
                        return expComp + " EV";
                    }
                    break;
                    
                case WHITE_BALANCE:
                    int wb = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1);
                    if (wb == ExifInterface.WHITE_BALANCE_AUTO) {
                        return "自动白平衡";
                    } else if (wb == ExifInterface.WHITE_BALANCE_MANUAL) {
                        return "手动白平衡";
                    }
                    break;
                    
                case FLASH:
                    int flash = exif.getAttributeInt(ExifInterface.TAG_FLASH, -1);
                    if (flash >= 0) {
                        return (flash % 2 == 1) ? "闪光灯开启" : "闪光灯关闭";
                    }
                    break;
                    
                case METERING_MODE:
                    int metering = exif.getAttributeInt(ExifInterface.TAG_METERING_MODE, -1);
                    return getMeteringModeName(metering);
                    
                case IMAGE_SIZE:
                    int width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0);
                    int height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0);
                    if (width > 0 && height > 0) {
                        return width + " x " + height;
                    }
                    break;
                    
                case RESOLUTION:
                    String xRes = exif.getAttribute(ExifInterface.TAG_X_RESOLUTION);
                    if (xRes != null) {
                        return xRes + " DPI";
                    }
                    break;
                    
                case COPYRIGHT:
                    return exif.getAttribute(ExifInterface.TAG_COPYRIGHT);
                    
                case AUTHOR:
                    return exif.getAttribute(ExifInterface.TAG_ARTIST);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    private String formatDateTime(String dateTime) {
        if (dateTime == null) return null;
        try {
            SimpleDateFormat exifFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
            Date date = exifFormat.parse(dateTime);
            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return displayFormat.format(date);
        } catch (Exception e) {
            return dateTime;
        }
    }
    
    private String formatFocalLength(String focal) {
        try {
            String[] parts = focal.split("/");
            if (parts.length == 2) {
                double value = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                return String.format(Locale.US, "%.0f", value);
            }
            return focal;
        } catch (Exception e) {
            return focal;
        }
    }
    
    private String formatShutterSpeed(String exposureTime) {
        try {
            String[] parts = exposureTime.split("/");
            if (parts.length == 2) {
                int numerator = Integer.parseInt(parts[0]);
                int denominator = Integer.parseInt(parts[1]);
                if (numerator == 1) {
                    return "1/" + denominator + "s";
                } else {
                    double seconds = (double) numerator / denominator;
                    return String.format(Locale.US, "%.1fs", seconds);
                }
            }
            return exposureTime + "s";
        } catch (Exception e) {
            return exposureTime;
        }
    }
    
    private String getMeteringModeName(int mode) {
        switch (mode) {
            case ExifInterface.METERING_MODE_AVERAGE: return "平均测光";
            case ExifInterface.METERING_MODE_CENTER_WEIGHTED_AVERAGE: return "中央重点测光";
            case ExifInterface.METERING_MODE_SPOT: return "点测光";
            case ExifInterface.METERING_MODE_MULTI_SPOT: return "多点测光";
            case ExifInterface.METERNG_MODE_PATTERN: return "评价测光";
            default: return null;
        }
    }
    
    private String getAddressFromGPS() {
        try {
            double[] latLong = exif.getLatLong();
            if (latLong == null) return null;
            
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latLong[0], latLong[1], 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // 返回简短地址：城市 + 区域
                String locality = address.getLocality(); // 城市
                String subLocality = address.getSubLocality(); // 区
                if (locality != null) {
                    return subLocality != null ? locality + subLocality : locality;
                }
                // 备用：完整地址第一行
                return address.getAddressLine(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 获取所有可用的字段及其值
     */
    public Map<ExifField, String> getAllAvailableData() {
        Map<ExifField, String> result = new HashMap<>();
        for (ExifField field : ExifField.values()) {
            if (field != ExifField.CUSTOM_TEXT) {  // 排除自定义字段
                String value = getValue(field);
                if (value != null && !value.isEmpty()) {
                    result.put(field, value);
                }
            }
        }
        return result;
    }
}
