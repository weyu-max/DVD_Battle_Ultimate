package com.darkonly.dvdbattleultimate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.LayoutInflater;
import java.util.Locale;

public class GameUtils {
    private static final Handler handler = new Handler(android.os.Looper.getMainLooper());

    public static void applyFullscreenIfEnabled(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("software_render", false)) {
            activity.getWindow().getDecorView().setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        
        activity.getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        validateWindowSize(activity);
    }

    public static void validateWindowSize(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (activity.isInMultiWindowMode()) {
                if (!DisplayUtils.isTablet(activity) && !DisplayUtils.isSmallPhone(activity)) {
                    android.widget.Toast.makeText(activity, 
                        "Arena 4:3 reduzida em tela dividida. Use tela cheia para melhor precisão.", 
                        android.widget.Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public static void applyLanguage(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        String langCode = prefs.getString("language", "pt");
        
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Resources resources = activity.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        activity.getBaseContext().getResources().updateConfiguration(config, resources.getDisplayMetrics());
    }

    public static Runnable startStarAnimation(Activity activity, ViewGroup root, Handler handlerIgnored) {
        if (root == null) return null;
        
        final int starCount = 30;
        final float[] starX = new float[starCount];
        final float[] starY = new float[starCount];
        final View[] starViews = new View[starCount];
        
        for (int i = 0; i < starCount; i++) {
            View dot = new View(activity);
            int size = (int)(Math.random() * 2) + 2;
            dot.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            dot.setBackgroundColor(android.graphics.Color.WHITE);
            dot.setAlpha((float)Math.random() * 0.4f + 0.1f);
            root.addView(dot, 0); 
            
            starX[i] = (float)(Math.random() * 1000);
            starY[i] = (float)(Math.random() * 1000);
            starViews[i] = dot;
        }

        final Runnable moveTask = new Runnable() {
            @Override
            public void run() {
                int w = root.getWidth();
                int h = root.getHeight();
                if (w <= 0 || h <= 0) {
                    handler.postDelayed(this, 100);
                    return;
                }
                
                for (int i = 0; i < starCount; i++) {
                    starX[i] += 0.8f;
                    starY[i] += 0.5f;
                    if (starX[i] > w) starX[i] = -5;
                    if (starY[i] > h) starY[i] = -5;
                    starViews[i].setX(starX[i]);
                    starViews[i].setY(starY[i]);
                }
                handler.postDelayed(this, 40); // Constant frame rate
            }
        };
        handler.post(moveTask);
        return moveTask;
    }

    public static void startBackgroundFlicker(View root, Handler handler) {
        if (root == null) return;
        Runnable flicker = new Runnable() {
            @Override public void run() {
                if (Math.random() > 0.8) {
                    root.setBackgroundColor(android.graphics.Color.parseColor("#1A0033"));
                    handler.postDelayed(() -> root.setBackgroundColor(android.graphics.Color.BLACK), 100);
                }
                handler.postDelayed(this, 3000 + (long)(Math.random() * 5000));
            }
        };
        handler.postDelayed(flicker, 3000);
    }

    public static void showNotification(Activity activity, String title, String message, int iconRes) {
        ViewGroup rootView = (ViewGroup) activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        LayoutInflater inflater = activity.getLayoutInflater();
        View notification = inflater.inflate(R.layout.notification_banner, rootView, false);

        ((ImageView) notification.findViewById(R.id.notif_icon)).setImageResource(iconRes);
        ((TextView) notification.findViewById(R.id.notif_title)).setText(title);
        ((TextView) notification.findViewById(R.id.notif_message)).setText(message);

        rootView.addView(notification);
        notification.setTranslationY(-400);
        notification.animate().translationY(20).setDuration(600).withEndAction(() -> {
            handler.postDelayed(() -> notification.animate().translationY(-400).setDuration(600).withEndAction(() -> rootView.removeView(notification)).start(), 4000);
        }).start();
        
        SharedPreferences prefs = activity.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("sound_enabled", true)) {
            android.media.ToneGenerator tg = new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100);
            tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150);
        }
    }

    public static void showTutorial(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("tutorial_completed", false)) return;

        new android.app.AlertDialog.Builder(activity)
            .setTitle("BEM-VINDO AO DVD BATTLE!")
            .setMessage("Tutorial Rápido:\n\n" +
                    "1. Você é o quadrado que rebate nos cantos.\n" +
                    "2. Toque na tela para dar um DASH (Dano).\n" +
                    "3. Use PARRY para repelir inimigos.\n" +
                    "4. Colete itens para sobreviver!\n" +
                    "5. Mate quadrados para o bem da humanidade.\n\n" +
                    "Boa sorte, Imperial!")
            .setPositiveButton("VAMOS NESSA!", (dialog, which) -> {
                prefs.edit().putBoolean("tutorial_completed", true).apply();
            })
            .setCancelable(false)
            .show();
    }

    public static void restartApp(Activity activity) {
        Intent intent = new Intent(activity, LoadingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }
}
