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
        private long touchDownTime;
        private static final int CLICK_THRESHOLD = 200; // 200ms内算点击
        private static final int MOVE_THRESHOLD = 10; // 移动小于10px算点击

        private boolean isControlsVisible = false;
        private View controlFrame;
        private View resizeHandle;
        private View closeButton;
        
        private boolean isDraggingResize = false;
        private float initialDistance = 0;
        private float currentScale = 1.0f;
        private float baseTextSize = 24f;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    touchDownTime = System.currentTimeMillis();
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

                    // 根据控制框是否可见，更新不同的视图
                    if (isControlsVisible && controlFrame != null) {
                        windowManager.updateViewLayout(controlFrame, layoutParams);
                    } else {
                        windowManager.updateViewLayout(v, layoutParams);
                    }
                    break;
                    
                case MotionEvent.ACTION_UP:
                    long touchDuration = System.currentTimeMillis() - touchDownTime;
                    int deltaX = Math.abs((int)event.getRawX() - x);
                    int deltaY = Math.abs((int)event.getRawY() - y);
                    int totalMoved = deltaX + deltaY;
                    
                    // 判断是否为点击（时间短且移动距离小）
                    if (touchDuration < CLICK_THRESHOLD && totalMoved < MOVE_THRESHOLD) {
                        toggleControls();
                    }
                    break;

                default:
                    break;
            }
            return true;
        }
        
        private void toggleControls() {
            if (isControlsVisible) {
                hideControls();
            } else {
                showControls();
            }
        }
        
        private void showControls() {
            if (controlFrame != null) return;
            
            isControlsVisible = true;
            
            // 创建控制框容器
            android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(getApplicationContext());
            controlFrame = frameLayout;
            
            // 从 WindowManager 移除时钟，添加到框架中
            try {
                windowManager.removeView(mTextClock);
            } catch (Exception e) {
                // 如果已经被移除，忽略错误
            }
            
            // 添加时钟视图到框架中
            android.widget.FrameLayout.LayoutParams clockParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            );
            clockParams.gravity = android.view.Gravity.CENTER;
            frameLayout.addView(mTextClock, clockParams);
            
            // 设置边框
            GradientDrawable border = new GradientDrawable();
            border.setCornerRadius(32);
            border.setStroke(3, Color.parseColor("#00FF00"));
            border.setColor(Color.TRANSPARENT);
            frameLayout.setForeground(border);
            
            // 创建缩放手柄（左下角）
            resizeHandle = new View(getApplicationContext());
            android.widget.FrameLayout.LayoutParams handleParams = new android.widget.FrameLayout.LayoutParams(40, 40);
            handleParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.START;
            resizeHandle.setLayoutParams(handleParams);
            GradientDrawable handleBg = new GradientDrawable();
            handleBg.setShape(GradientDrawable.OVAL);
            handleBg.setColor(Color.parseColor("#00FF00"));
            resizeHandle.setBackground(handleBg);
            resizeHandle.setOnTouchListener(new ResizeHandleTouchListener());
            frameLayout.addView(resizeHandle);
            
            // 创建关闭按钮（右上角）
            closeButton = new android.widget.TextView(getApplicationContext());
            android.widget.FrameLayout.LayoutParams closeParams = new android.widget.FrameLayout.LayoutParams(40, 40);
            closeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            closeButton.setLayoutParams(closeParams);
            ((android.widget.TextView)closeButton).setText("×");
            ((android.widget.TextView)closeButton).setTextSize(20);
            ((android.widget.TextView)closeButton).setTextColor(Color.WHITE);
            ((android.widget.TextView)closeButton).setGravity(android.view.Gravity.CENTER);
            GradientDrawable closeBg = new GradientDrawable();
            closeBg.setShape(GradientDrawable.OVAL);
            closeBg.setColor(Color.parseColor("#FF0000"));
            closeButton.setBackground(closeBg);
            closeButton.setOnClickListener(v -> {
                stopSelf();
            });
            frameLayout.addView(closeButton);
            
            // 添加控制框到窗口
            windowManager.addView(frameLayout, layoutParams);
            
            // 设置触摸监听器到控制框
            frameLayout.setOnTouchListener(this);
        }
        
        private void hideControls() {
            if (controlFrame == null) return;
            
            isControlsVisible = false;
            
            // 从控制框中移除时钟视图
            ((android.view.ViewGroup)controlFrame).removeView(mTextClock);
            
            // 移除控制框
            try {
                windowManager.removeView(controlFrame);
            } catch (Exception e) {
                // 忽略错误
            }
            
            // 恢复原始时钟视图到窗口
            windowManager.addView(mTextClock, layoutParams);
            
            // 恢复触摸监听器
            mTextClock.setOnTouchListener(this);
            
            controlFrame = null;
            resizeHandle = null;
            closeButton = null;
        }
        
        private class ResizeHandleTouchListener implements View.OnTouchListener {
            private int startX, startY;
            private float startScale;
            private int startWidth, startHeight;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = (int) event.getRawX();
                        startY = (int) event.getRawY();
                        startScale = currentScale;
                        
                        // 记录初始尺寸
                        startWidth = controlFrame.getWidth();
                        startHeight = controlFrame.getHeight();
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        int currentX = (int) event.getRawX();
                        int currentY = (int) event.getRawY();
                        
                        // 计算拖动距离（向左下为正，向右上为负）
                        int deltaX = currentX - startX;
                        int deltaY = currentY - startY;
                        
                        // 使用对角线距离计算缩放比例
                        // 向左下拖动（deltaX < 0, deltaY > 0）= 放大
                        // 向右上拖动（deltaX > 0, deltaY < 0）= 缩小
                        float diagonalDelta = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                        
                        // 判断方向：左下为正（放大），右上为负（缩小）
                        boolean isEnlarging = (deltaX < 0 && deltaY > 0) || 
                                            (deltaX < 0 && Math.abs(deltaX) > Math.abs(deltaY)) ||
                                            (deltaY > 0 && Math.abs(deltaY) > Math.abs(deltaX));
                        
                        float scaleDelta = diagonalDelta / 300f; // 调整灵敏度
                        if (!isEnlarging) {
                            scaleDelta = -scaleDelta;
                        }
                        
                        currentScale = startScale + scaleDelta;
                        currentScale = Math.max(0.5f, Math.min(currentScale, 3.0f));
                        
                        // 更新文字大小
                        mTextClock.setTextSize(baseTextSize * currentScale);
                        
                        // 实时更新窗口布局以反映大小变化
                        // 强制重新测量和布局
                        controlFrame.requestLayout();
                        
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        return true;
                }
                return false;
            }
        }
    }
}
