package com.genymobile.scrcpy;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by seanzhou on 3/14/17.
 */
@SuppressLint("PrivateApi")
public final class ScreenCaptorUtils {

    private static final String METHOD_SCREENSHOT = "screenshot";

    private static final Class<?> clazz;
    private static Method getBuiltInDisplayMethod;

    static {
        try {
            String className;
            int sdkInt = Build.VERSION.SDK_INT;
            if (sdkInt > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                className = "android.view.SurfaceControl";
            } else {
                className = "android.view.Surface";
            }

            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Bitmap screenshot(int w, int h) {
        Bitmap bitmap = null;

        try {
            Method declaredMethod;

            int sdkInt = Build.VERSION.SDK_INT;
            if (sdkInt >= Build.VERSION_CODES.P) { // Pie+
                declaredMethod =
                        clazz.getDeclaredMethod(
                                METHOD_SCREENSHOT,
                                Rect.class,
                                Integer.TYPE,
                                Integer.TYPE,
                                Integer.TYPE);
                bitmap = (Bitmap) declaredMethod.invoke(null, new Rect(), w, h, 0);
            } else {
                declaredMethod =
                        clazz.getDeclaredMethod(METHOD_SCREENSHOT, Integer.TYPE, Integer.TYPE);
                bitmap = (Bitmap) declaredMethod.invoke(null, new Object[]{w, h});
            }

            // if (bitmap != null) System.out.println(">>> bmp generated.");

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private static Method getGetBuiltInDisplayMethod() throws NoSuchMethodException {
        if (getBuiltInDisplayMethod == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                getBuiltInDisplayMethod = clazz.getMethod("getBuiltInDisplay", Integer.TYPE);
            } else { // The method signature has been changed in Android Q+
                getBuiltInDisplayMethod = clazz.getMethod("getInternalDisplayToken");
            }
        }
        return getBuiltInDisplayMethod;
    }

    public static IBinder getBuiltInDisplay() {

        try {
            Method method = getGetBuiltInDisplayMethod();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // default display 0
                return (IBinder) method.invoke(null, 0);
            }

            return (IBinder) method.invoke(null);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            System.err.println("Failed to invoke method " + e);
            return null;
        }
    }
}