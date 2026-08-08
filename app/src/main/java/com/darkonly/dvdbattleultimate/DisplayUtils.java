package com.darkonly.dvdbattleultimate;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class DisplayUtils {
    private static final String TAG = "DisplayUtils";

    public static void logDisplayInfo(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return;

        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        float density = metrics.density;
        int dpi = metrics.densityDpi;

        double inches = getPhysicalScreenSize(context);

        Log.d(TAG, "Display Size: " + width + "x" + height);
        Log.d(TAG, "Density: " + density + " (DPI: " + dpi + ")");
        Log.d(TAG, "Physical Size: ~" + String.format("%.2f", inches) + " inches");
    }

    public static double getPhysicalScreenSize(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        float widthInches = dm.widthPixels / dm.xdpi;
        float heightInches = dm.heightPixels / dm.ydpi;
        return Math.sqrt(Math.pow(widthInches, 2) + Math.pow(heightInches, 2));
    }

    /**
     * Tabela Imperial de Dispositivos:
     * - Tablet: 6.8" ou mais (Inclui tablets de 7", 8" e 10")
     * - Telefone Padrão: 5.0" até 6.8"
     * - Telefone Compacto (Legacy): Menos de 5.0" (ex: Pocket)
     */
    public static boolean isTablet(Context context) {
        return getPhysicalScreenSize(context) >= 6.8;
    }

    public static boolean isSmallPhone(Context context) {
        return getPhysicalScreenSize(context) < 5.0;
    }
    
    public static float getUIScale(Context context) {
        if (isSmallPhone(context)) {
            return 0.85f; // Reduz tamanho para celulares compactos
        }
        return 1.0f;
    }
}
