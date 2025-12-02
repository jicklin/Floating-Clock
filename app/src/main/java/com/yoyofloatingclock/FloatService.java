package com.yoyofloatingclock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FloatService extends Service {
    WindowManager windowManager;

    WindowManager.LayoutParams layoutParams;

    TextView mTextClock;
    
    Handler mHandler;
    
    Runnable mRunnable;
    
    PowerManager.WakeLock wakeLock;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        acquireWakeLock();
        init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uninit();
        releaseWakeLock();
    }
    
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FloatingClock::ClockWakeLock"
            );
            wakeLock.acquire();
        }
    }
    
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void uninit() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        windowManager.removeView(mTextClock);
    }

    private void init() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL 
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.x = 1;
        layoutParams.y = 1;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            mTextClock = new TextView(getApplicationContext());
            mTextClock.setTextSize(24);
            mTextClock.setGravity(Gravity.CENTER);
            mTextClock.setPaddingRelative(10, 8, 10, 8);
            // 使用等宽字体避免数字宽度变化导致的抖动
            mTextClock.setTypeface(android.graphics.Typeface.MONOSPACE);
            mTextClock.setTextColor(Color.WHITE);
            mTextClock.setOnTouchListener(new FloatingOnTouchListener());

            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(32);
            drawable.setStroke(1, Color.WHITE);
            drawable.setColor(Color.rgb(85, 26, 139));
            mTextClock.setBackground(drawable);

            windowManager.addView(mTextClock, layoutParams);
            windowManager.updateViewLayout(mTextClock.getRootView(), layoutParams);

            // 初始化 Handler 和 Runnable 用于更新时间（精确到0.1秒）
            mHandler = new Handler();
            
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    long currentTime = System.currentTimeMillis();
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    String timeStr = sdf.format(new Date(currentTime));
                    // 只显示到十分之一秒，减少抖动
                    int decisecond = (int) ((currentTime % 1000) / 100);
                    mTextClock.setText(timeStr + "." + decisecond);
                    mHandler.postDelayed(this, 100); // 每100毫秒更新一次
                }
            };
            
            mHandler.post(mRunnable);
        }

    }

    private class FloatingOnTouchListener implements View.OnTouchListener {

        private int x;

        private int y;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    System.out.println("x" + x + "y" + y);

                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;

                    x = nowX;
                    y = nowY;


                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;

                    windowManager.updateViewLayout(v, layoutParams);
                    break;

                default:
                    break;

            }
            return false;
        }
    }
}
