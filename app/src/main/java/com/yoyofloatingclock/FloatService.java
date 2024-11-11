package com.yoyofloatingclock;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextClock;

import androidx.annotation.Nullable;

public class FloatService extends Service {
    WindowManager windowManager;

    WindowManager.LayoutParams layoutParams;

    TextClock mTextClock;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uninit();
    }

    private void uninit() {
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
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.x = 1;
        layoutParams.y = 1;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            mTextClock = new TextClock(getApplicationContext());
            mTextClock.setFormat24Hour("HH:mm:ss");
            mTextClock.setFormat12Hour("hh:mm:ss");
            mTextClock.setTextSize(20);
            mTextClock.setGravity(Gravity.CENTER);
            mTextClock.setPaddingRelative(5, 5, 5, 5);

            mTextClock.setTextColor(Color.WHITE);
//            mTextClock.setBackgroundColor(Color.BLACK);
            mTextClock.setOnTouchListener(new FloatingOnTouchListener());

            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(32);
            drawable.setStroke(1, Color.WHITE);
            drawable.setColor(Color.rgb(85, 26, 139));
            mTextClock.setBackground(drawable);

            windowManager.addView(mTextClock, layoutParams);
            windowManager.updateViewLayout(mTextClock.getRootView(), layoutParams);

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
