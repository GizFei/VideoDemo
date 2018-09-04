package com.giz.videodemo;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class PixelUtil {

    private static Context sContext;

    public static void initContext(Context context){
        sContext = context;
    }

    public static int px2dp(float value) throws NullPointerException{
        if(sContext == null){
            throw new NullPointerException("No context in PixelUtil. Please use method " +
                    "<b>initContext</b> first.");
        }
        final DisplayMetrics metrics = sContext.getResources().getDisplayMetrics();
        return (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics) + 0.5f);
    }
}
