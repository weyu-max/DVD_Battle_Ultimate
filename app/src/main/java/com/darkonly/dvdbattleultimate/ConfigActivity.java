package com.darkonly.dvdbattleultimate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class ConfigActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "GamePrefs";
    public static final String KEY_LANG = "language";
    public static final String KEY_SOUND = "sound_enabled";
    public static final String KEY_SHAKE = "shake_enabled";

    private String[] languages = {"pt", "en", "es"};
    private int currentLangIndex = 0;
    
    private ImageView imgFlag;
    private TextView textLangName;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable starRunnable;

    @Override
    protected void onResume() {
        super.onResume();
        GameUtils.applyFullscreenIfEnabled(this);
        if (starRunnable != null) handler.removeCallbacks(starRunnable);
        starRunnable = GameUtils.startStarAnimation(this, (ViewGroup) findViewById(android.R.id.content), handler);
        GameUtils.startBackgroundFlicker(findViewById(android.R.id.content), handler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (starRunnable != null) handler.removeCallbacks(starRunnable);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GameUtils.applyLanguage(this);
        setContentView(R.layout.activity_config);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Easter Egg: Gray color
        if (!prefs.getBoolean("easter_egg_gray_unlocked", false)) {
            prefs.edit().putBoolean("easter_egg_gray_unlocked", true).apply();
            SaveManager.saveData(this);
            Toast.makeText(this, "Cor Cinza Desbloqueada! (Easter Egg)", Toast.LENGTH_SHORT).show();
        }

        CheckBox checkSound = findViewById(R.id.check_sound);
        boolean isSound = prefs.getBoolean(KEY_SOUND, true);
        checkSound.setChecked(isSound);
        checkSound.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean(KEY_SOUND, isChecked).apply();
            SaveManager.saveData(this);
        });

        CheckBox checkShake = findViewById(R.id.check_shake);
        boolean isShake = prefs.getBoolean(KEY_SHAKE, true);
        checkShake.setChecked(isShake);
        checkShake.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean(KEY_SHAKE, isChecked).apply();
            SaveManager.saveData(this);
        });

        CheckBox checkSW = findViewById(R.id.check_software_render);
        boolean isSW = prefs.getBoolean("software_render", true);
        checkSW.setChecked(isSW);
        checkSW.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("software_render", isChecked).apply();
            SaveManager.saveData(this);
            GameUtils.restartApp(this);
        });

        // Language Selector
        imgFlag = findViewById(R.id.img_flag);
        textLangName = findViewById(R.id.text_lang_name);
        
        String savedLang = prefs.getString(KEY_LANG, "pt");
        for(int i=0; i<languages.length; i++) {
            if(languages[i].equals(savedLang)) currentLangIndex = i;
        }
        updateLangUI();

        findViewById(R.id.btn_lang_prev).setOnClickListener(v -> {
            currentLangIndex = (currentLangIndex - 1 + languages.length) % languages.length;
            saveAndApplyLang();
        });

        findViewById(R.id.btn_lang_next).setOnClickListener(v -> {
            currentLangIndex = (currentLangIndex + 1) % languages.length;
            saveAndApplyLang();
        });

        findViewById(R.id.btn_back_config).setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_credits).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreditsActivity.class);
            startActivity(intent);
        });

        applyFullscreen(true);
    }

    private void updateLangUI() {
        switch (languages[currentLangIndex]) {
            case "pt":
                imgFlag.setImageResource(R.drawable.flag_br);
                textLangName.setText(R.string.lang_pt);
                break;
            case "en":
                imgFlag.setImageResource(R.drawable.flag_us);
                textLangName.setText(R.string.lang_en);
                break;
            case "es":
                imgFlag.setImageResource(R.drawable.flag_es);
                textLangName.setText(R.string.lang_es);
                break;
        }
    }

    private void saveAndApplyLang() {
        String lang = languages[currentLangIndex];
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(KEY_LANG, lang).apply();
        SaveManager.saveData(this);
        
        // Full App Restart to apply language correctly
        Intent intent = new Intent(this, LoadingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    private void applyFullscreen(boolean isFullscreen) {
        if (isFullscreen) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }
}
