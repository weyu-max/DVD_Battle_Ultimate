package com.darkonly.dvdbattleultimate;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

public class CharacterActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "GamePrefs";
    public static final String KEY_PLAYER_COLOR = "player_color";
    public static final String KEY_EYES = "player_eyes";
    public static final String KEY_MOUTH = "player_mouth";
    public static final String KEY_AURA = "player_aura";
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable starRunnable;

    private View playerPreview;
    private ImageView eyesPreview;
    private ImageView mouthPreview;
    
    private View sectionCores, sectionOlhos, sectionBocas, sectionAura;
    private int playerIndex = 0; 
    private String keyColor, keyEyes, keyMouth, keyAura;

    @Override
    protected void onResume() {
        super.onResume();
        GameUtils.applyLanguage(this);
        GameUtils.applyFullscreenIfEnabled(this);
        if (starRunnable != null) handler.removeCallbacks(starRunnable);
        starRunnable = GameUtils.startStarAnimation(this, (ViewGroup) findViewById(android.R.id.content), handler);
        GameUtils.startBackgroundFlicker(findViewById(android.R.id.content), handler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eyesPreview != null) eyesPreview.setImageDrawable(null);
        if (mouthPreview != null) mouthPreview.setImageDrawable(null);
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_character);

        playerIndex = getIntent().getIntExtra("player_index", 0);
        updateKeys();

        playerPreview = findViewById(R.id.player_preview);
        eyesPreview = findViewById(R.id.preview_eyes);
        mouthPreview = findViewById(R.id.preview_mouth);
        
        sectionCores = findViewById(R.id.section_cores);
        sectionOlhos = findViewById(R.id.section_olhos);
        sectionBocas = findViewById(R.id.section_bocas);
        sectionAura = findViewById(R.id.section_aura);

        setupTabs();
        setupPlayerTabs();
        loadSelections();
        setupOptions();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void updateKeys() {
        if (playerIndex == 0) {
            keyColor = KEY_PLAYER_COLOR;
            keyEyes = KEY_EYES;
            keyMouth = KEY_MOUTH;
            keyAura = KEY_AURA;
        } else {
            keyColor = "p2_color";
            keyEyes = "p2_eyes";
            keyMouth = "p2_mouth";
            keyAura = "p2_aura";
        }
    }

    private void setupPlayerTabs() {
        View btnP1 = findViewById(R.id.btn_tab_p1);
        View btnP2 = findViewById(R.id.btn_tab_p2);
        if (btnP1 == null || btnP2 == null) return;

        btnP1.setOnClickListener(v -> switchPlayer(0));
        btnP2.setOnClickListener(v -> switchPlayer(1));
        btnP1.setAlpha(playerIndex == 0 ? 1.0f : 0.6f);
        btnP2.setAlpha(playerIndex == 1 ? 1.0f : 0.6f);
    }

    private void switchPlayer(int index) {
        if (playerIndex == index) return;
        playerIndex = index;
        updateKeys();
        setupPlayerTabs();
        loadSelections();
        setupOptions();
    }

    private void setupTabs() {
        View tabCores = findViewById(R.id.tab_cores);
        tabCores.setOnClickListener(v -> showSection(sectionCores));
        findViewById(R.id.tab_olhos).setOnClickListener(v -> showSection(sectionOlhos));
        findViewById(R.id.tab_bocas).setOnClickListener(v -> showSection(sectionBocas));
        findViewById(R.id.tab_aura).setOnClickListener(v -> showSection(sectionAura));

        tabCores.setOnTouchListener(new View.OnTouchListener() {
            private final Handler h = new Handler(Looper.getMainLooper());
            private final Runnable r = () -> {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean("secret_cyan_unlocked", true).apply();
                SaveManager.saveData(CharacterActivity.this);
                Toast.makeText(CharacterActivity.this, "Cor Ciano Desbloqueada! (Segredo)", Toast.LENGTH_SHORT).show();
                setupOptions();
            };
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) h.postDelayed(r, 10000);
                else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                    h.removeCallbacks(r);
                    if (event.getAction() == android.view.MotionEvent.ACTION_UP) v.performClick();
                }
                return true;
            }
        });
    }

    private void showSection(View section) {
        sectionCores.setVisibility(View.GONE);
        sectionOlhos.setVisibility(View.GONE);
        sectionBocas.setVisibility(View.GONE);
        sectionAura.setVisibility(View.GONE);
        section.setVisibility(View.VISIBLE);
    }

    private void loadSelections() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Default P1 & P2: Yellow, Normal, Happy
        int color = prefs.getInt(keyColor, Color.YELLOW);
        playerPreview.setBackgroundColor(color);
        
        String eyesName = prefs.getString(keyEyes + "_name", "");
        int eyesRes = SaveManager.getResId(this, eyesName, "drawable");
        if (eyesRes == 0) {
            eyesRes = R.drawable.eye_normal;
        }
        eyesPreview.setImageResource(eyesRes);
        
        String mouthName = prefs.getString(keyMouth + "_name", "");
        int mouthRes = SaveManager.getResId(this, mouthName, "drawable");
        if (mouthRes == 0) {
            mouthRes = R.drawable.mouth_happy;
        }
        mouthPreview.setImageResource(mouthRes);
    }

    private void setupOptions() {
        ((GridLayout)sectionCores).removeAllViews();
        ((GridLayout)sectionOlhos).removeAllViews();
        ((GridLayout)sectionBocas).removeAllViews();
        ((GridLayout)sectionAura).removeAllViews();

        int columns = DisplayUtils.isSmallPhone(this) ? 2 : 4;
        ((GridLayout)sectionCores).setColumnCount(columns);
        ((GridLayout)sectionOlhos).setColumnCount(columns);
        ((GridLayout)sectionBocas).setColumnCount(columns);
        ((GridLayout)sectionAura).setColumnCount(columns);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int totalKills = prefs.getInt("total_kills", 0);
        int rank = prefs.getInt("player_rank", 0);

        // --- CORES ---
        addColorOption(R.color.imperial_yellow, R.string.color_yellow, (GridLayout) sectionCores);
        if (prefs.getBoolean("ach_heal_master_unlocked", false)) addColorOption(R.color.hp_green, R.string.color_green, (GridLayout) sectionCores);
        if (prefs.getBoolean("ach_die_hard_unlocked", false)) addColorOption(R.color.enemy_color, R.string.color_red, (GridLayout) sectionCores);
        if (rank >= 4) addColorOption(Color.parseColor("#FFD700"), R.string.color_gold, (GridLayout) sectionCores);

        // --- OLHOS ---
        addPartOption(R.drawable.eye_normal, R.string.eye_normal, (GridLayout) sectionOlhos, eyesPreview);
        addPartOption(R.drawable.eye_angry, R.string.eye_angry, (GridLayout) sectionOlhos, eyesPreview);
        if (totalKills >= 50) addPartOption(R.drawable.eye_glasses_round, R.string.eye_glasses_round, (GridLayout) sectionOlhos, eyesPreview);

        // --- BOCAS ---
        addPartOption(R.drawable.mouth_happy, R.string.mouth_happy, (GridLayout) sectionBocas, mouthPreview);
        addPartOption(R.drawable.mouth_sad, R.string.mouth_sad, (GridLayout) sectionBocas, mouthPreview);

        // --- AURA ---
        addAuraOption(9, R.string.aura_none, (GridLayout) sectionAura, R.drawable.aura_symbol_basic);
        if (prefs.getInt("best_infinite_time", 0) >= 60) addAuraOption(1, R.string.aura_water, (GridLayout) sectionAura, R.drawable.aura_symbol_water);
    }

    private void addColorOption(int resOrVal, int nameRes, GridLayout parent) {
        int color = (resOrVal < 0 || resOrVal > 0x01000000) ? resOrVal : ContextCompat.getColor(this, resOrVal);
        LinearLayout layout = createOptionLayout(nameRes);
        AppCompatButton btn = (AppCompatButton) ((ViewGroup)layout.getChildAt(0)).getChildAt(0);
        
        // Fix translucent: Apply tint to the background drawable
        btn.setBackgroundResource(R.drawable.btn_color_selection);
        if (btn.getBackground() != null) {
            btn.getBackground().setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY);
        }
        
        btn.setOnClickListener(v -> {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(keyColor, color).apply();
            playerPreview.setBackgroundColor(color);
            SaveManager.saveData(this);
            Toast.makeText(this, getString(R.string.toast_color_selected, getString(nameRes)), Toast.LENGTH_SHORT).show();
        });
        parent.addView(layout);
    }

    private void addPartOption(int res, int nameRes, GridLayout parent, ImageView preview) {
        LinearLayout layout = createOptionLayout(nameRes);
        AppCompatButton btn = (AppCompatButton) ((ViewGroup)layout.getChildAt(0)).getChildAt(0);
        ImageView icon = (ImageView) ((ViewGroup)layout.getChildAt(0)).getChildAt(1);
        icon.setImageResource(res);
        btn.setOnClickListener(v -> {
            String key = (preview == eyesPreview) ? keyEyes : keyMouth;
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putString(key + "_name", SaveManager.getResName(this, res)).apply();
            preview.setImageResource(res);
            SaveManager.saveData(this);
        });
        parent.addView(layout);
    }

    private void addAuraOption(int type, int nameRes, GridLayout parent, int iconRes) {
        LinearLayout layout = createOptionLayout(nameRes);
        AppCompatButton btn = (AppCompatButton) ((ViewGroup)layout.getChildAt(0)).getChildAt(0);
        ImageView icon = (ImageView) ((ViewGroup)layout.getChildAt(0)).getChildAt(1);
        icon.setImageResource(iconRes);
        btn.setOnClickListener(v -> {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putInt(keyAura, type).apply();
            SaveManager.saveData(this);
        });
        parent.addView(layout);
    }

    private int dpToPx(int dp) {
        return (int) android.util.TypedValue.applyDimension(
                android.util.TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private LinearLayout createOptionLayout(int nameRes) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(android.view.Gravity.CENTER);
        l.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        
        float uiScale = DisplayUtils.getUIScale(this);
        int size = dpToPx((int)(60 * uiScale)); // Use 60dp as base for small icons
        
        android.widget.FrameLayout f = new android.widget.FrameLayout(this);
        f.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        
        AppCompatButton b = new AppCompatButton(this);
        b.setLayoutParams(new android.widget.FrameLayout.LayoutParams(-1, -1));
        b.setBackgroundResource(R.drawable.btn_retro_classic);
        
        ImageView i = new ImageView(this);
        i.setLayoutParams(new android.widget.FrameLayout.LayoutParams(-1, -1));
        i.setPadding(10, 10, 10, 10);
        i.setClickable(false);

        f.addView(b); f.addView(i);
        TextView t = new TextView(this);
        t.setText(getString(nameRes));
        t.setTextColor(Color.WHITE);
        t.setTextSize(8);
        t.setGravity(android.view.Gravity.CENTER);

        l.addView(f); l.addView(t);
        return l;
    }
}
