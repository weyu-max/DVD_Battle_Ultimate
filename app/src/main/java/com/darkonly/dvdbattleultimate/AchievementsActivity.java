package com.darkonly.dvdbattleultimate;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class AchievementsActivity extends AppCompatActivity {

    private LinearLayout achievementsContainer;
    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable starRunnable;

    @Override
    protected void onResume() {
        super.onResume();
        GameUtils.applyFullscreenIfEnabled(this);
        if (starRunnable != null) handler.removeCallbacks(starRunnable);
        starRunnable = GameUtils.startStarAnimation(this, findViewById(android.R.id.content), handler);
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
        
        prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);

        LinearLayout mainRoot = new LinearLayout(this);
        mainRoot.setOrientation(LinearLayout.VERTICAL);
        mainRoot.setBackgroundColor(Color.BLACK);
        mainRoot.setPadding(16, 16, 16, 16);

        TextView title = new TextView(this);
        title.setText(R.string.title_achievements);
        title.setTextColor(Color.YELLOW);
        title.setTextSize(32);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 16);
        mainRoot.addView(title);

        // Tab Row
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(0, 0, 0, 16);

        addTab(tabs, R.string.tab_general, "combat");
        addTab(tabs, R.string.tab_modes, "modes");
        addTab(tabs, R.string.tab_ranks, "ranks");
        addTab(tabs, R.string.tab_elite, "elite");

        mainRoot.addView(tabs);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));

        achievementsContainer = new LinearLayout(this);
        achievementsContainer.setOrientation(LinearLayout.VERTICAL);
        achievementsContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        achievementsContainer.setPadding(16, 16, 16, 16);
        
        scroll.addView(achievementsContainer);
        mainRoot.addView(scroll);

        AppCompatButton btnBack = new AppCompatButton(this);
        btnBack.setText(R.string.btn_back);
        btnBack.setBackgroundResource(R.drawable.btn_retro_classic);
        btnBack.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(300, 100);
        btnParams.gravity = Gravity.CENTER;
        btnParams.setMargins(0, 16, 0, 16);
        btnBack.setLayoutParams(btnParams);
        btnBack.setOnClickListener(v -> finish());
        mainRoot.addView(btnBack);

        setContentView(mainRoot);

        loadAchievements("combat");
    }

    private void addTab(LinearLayout parent, int labelRes, String category) {
        AppCompatButton btn = new AppCompatButton(this);
        btn.setText(getString(labelRes));
        btn.setBackgroundResource(R.drawable.btn_retro_classic);
        btn.setTextColor(Color.BLACK);
        btn.setTextSize(10);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 80, 1.0f);
        lp.setMargins(4, 0, 4, 0);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> loadAchievements(category));
        parent.addView(btn);
    }

    private void loadAchievements(String category) {
        achievementsContainer.removeAllViews();

        switch (category) {
            case "combat":
                addAchItem(achievementsContainer, R.string.ach_die_hard_title, R.string.ach_die_hard_desc, prefs.getInt("total_deaths", 0), 100);
                addAchItem(achievementsContainer, R.string.ach_fast_title, R.string.ach_fast_desc, prefs.getInt("max_dashes_match", 0), 10);
                addAchItem(achievementsContainer, R.string.ach_on_target_title, R.string.ach_on_target_desc, prefs.getInt("total_hits", 0), 10);
                addAchItem(achievementsContainer, R.string.ach_heal_master_title, R.string.ach_heal_master_desc, prefs.getInt("total_heals", 0), 50);
                addAchItem(achievementsContainer, R.string.ach_parry_master_title, R.string.ach_parry_master_desc, prefs.getInt("total_parries", 0), 20);
                addAchItem(achievementsContainer, R.string.ach_big_data_title, R.string.ach_big_data_desc, prefs.getInt("total_items_collected", 0), 50);
                break;
            case "modes":
                addAchItem(achievementsContainer, R.string.aura_water, R.string.ach_water_desc, prefs.getInt("best_infinite_time", 0), 60);
                addAchItem(achievementsContainer, R.string.aura_veneno, R.string.ach_poison_desc, prefs.getBoolean("won_vs_malware", false) ? 1 : 0, 1);
                addAchItem(achievementsContainer, R.string.aura_wind, R.string.ach_wind_desc, prefs.getBoolean("won_walls_match", false) ? 1 : 0, 1);
                addAchItem(achievementsContainer, R.string.ach_sync_title, R.string.ach_sync_desc, prefs.getBoolean("ach_sync_unlocked", false) ? 1 : 0, 1);
                addAchItem(achievementsContainer, R.string.ach_it_support_title, R.string.ach_it_support_desc, prefs.getInt("admin_powers_used", 0), 20);
                addAchItem(achievementsContainer, R.string.ach_secure_server_title, R.string.ach_secure_server_desc, prefs.getBoolean("ach_secure_server_unlocked", false) ? 1 : 0, 1);
                addAchItem(achievementsContainer, R.string.ach_speedrun_title, R.string.ach_speedrun_desc, 0, 1); // Logic handled in GameView
                break;
            case "ranks":
                int rank = prefs.getInt("player_rank", 0);
                addAchItem(achievementsContainer, R.string.ach_silver_title, R.string.ach_silver_desc, rank >= 1 ? 1 : 0, 1);
                addAchItem(achievementsContainer, R.string.ach_gold_title, R.string.ach_gold_desc, rank >= 2 ? 1 : 0, 1);
                addAchItem(achievementsContainer, R.string.ach_platinum_title, R.string.ach_platinum_desc, rank >= 3 ? 1 : 0, 1);
                addAchItem(achievementsContainer, R.string.ach_diamond_title, R.string.ach_diamond_desc, rank >= 4 ? 1 : 0, 1);
                break;
            case "elite":
                addAchItem(achievementsContainer, R.string.ach_impossible_dream_title, R.string.ach_impossible_dream_desc, prefs.getBoolean("ach_impossible_dream", false) ? 1 : 0, 1);
                addAchItem(achievementsContainer, R.string.ach_ctrl_title, R.string.ach_ctrl_desc, prefs.getBoolean("ach_ctrl_unlocked", false) ? 1 : 0, 1);
                addAchItem(achievementsContainer, R.string.ach_pirate_title, R.string.ach_pirate_desc, prefs.getBoolean("ach_pirate_unlocked", false) ? 1 : 0, 1);
                addAchItem(achievementsContainer, R.string.ach_streak_title, R.string.ach_streak_desc, prefs.getInt("ach_win_streak", 0), 10);
                addAchItem(achievementsContainer, R.string.ach_bsod_title, R.string.ach_bsod_desc, 0, 1); 
                addAchItem(achievementsContainer, R.string.ach_double_shield_title, R.string.ach_double_shield_desc, prefs.getBoolean("ach_double_shield_unlocked", false) ? 1 : 0, 1);
                addAchItem(achievementsContainer, R.string.ach_boss_parry_title, R.string.ach_boss_parry_desc, prefs.getBoolean("ach_boss_parry_unlocked", false) ? 1 : 0, 1);
                
                // Completionist Check
                String[] requiredKeys = {
                    "ach_okay_unlocked", "ach_fast_unlocked", "ach_on_target_unlocked",
                    "ach_sync_unlocked", "ach_it_support_unlocked", "ach_secure_server_unlocked",
                    "ach_ctrl_unlocked", "ach_pirate_unlocked", "ach_master_unlocked",
                    "ach_ice_unlocked", "ach_heal_master_unlocked", "ach_die_hard_unlocked",
                    "ach_star_collector_unlocked", "ach_double_shield_unlocked",
                    "ach_parry_master_unlocked", "ach_boss_parry_unlocked",
                    "ach_big_data_unlocked", "ach_bsod_unlocked", "ach_speedrun_unlocked"
                };
                int compCount = 0;
                for (String k : requiredKeys) if (prefs.getBoolean(k, false)) compCount++;
                addAchItem(achievementsContainer, R.string.ach_completionist_title, R.string.ach_completionist_desc, compCount, requiredKeys.length);
                break;
        }
    }

    private void addAchItem(LinearLayout root, int titleRes, int descRes, int current, int target) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setBackgroundResource(R.drawable.btn_retro_classic);
        item.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(600, -2);
        lp.setMargins(0, 16, 0, 16);
        item.setLayoutParams(lp);

        TextView t = new TextView(this);
        t.setText(getString(titleRes));
        t.setTextColor(Color.BLACK);
        t.setTextSize(18);
        t.setGravity(Gravity.CENTER);
        item.addView(t);

        TextView d = new TextView(this);
        d.setText(getString(descRes));
        d.setTextColor(Color.DKGRAY);
        d.setTextSize(12);
        d.setGravity(Gravity.CENTER);
        item.addView(d);

        TextView status = new TextView(this);
        boolean unlocked = current >= target;
        status.setText(unlocked ? getString(R.string.ach_unlocked) : getString(R.string.ach_locked) + " (" + current + "/" + target + ")");
        status.setTextColor(unlocked ? Color.BLUE : Color.RED);
        status.setGravity(Gravity.CENTER);
        item.addView(status);

        root.addView(item);
    }
}
