package com.yoyofloatingclock;

import android.content.Context;
import android.content.Intent;

public class Util {


    public static void startService(Context context, Class<?> cls) {
        if (!(context instanceof Context)) {
            return;
        }
        // do something

        context.startService(new Intent(context, cls));
    }


    public static void stopService(Context context, Class<?> cls) {
        context.stopService(new Intent(context, cls));
    }

}
