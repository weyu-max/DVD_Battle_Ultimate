package com.darkonly.dvdbattleultimate;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class UpgradesActivity extends AppCompatActivity {

    private int totalBlocks;
    private TextView textBlocks;
    private LinearLayout container;
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
        setContentView(R.layout.activity_upgrades);

        prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        totalBlocks = prefs.getInt("total_blocks", 0);
        
        textBlocks = findViewById(R.id.text_upgrades_blocks);
        container = findViewById(R.id.container_upgrades);
        updateBlocksDisplay();

        setupUpgrades();

        findViewById(R.id.btn_back_upgrades).setOnClickListener(v -> finish());
    }

    private void updateBlocksDisplay() {
        textBlocks.setText(getString(R.string.label_blocks, totalBlocks));
    }

    private void setupUpgrades() {
        addUpgradeItem("driver_cooling", "Cooling Fan", "Reduz recarga do Dash", 1);
        addUpgradeItem("driver_buffer", "Buffer RAM", "Mais invencibilidade pós-dano", 2);
        addUpgradeItem("driver_gpu", "GPU Boost", "Aumenta área do Dash", 3);
        addUpgradeItem("driver_ethernet", "Ethernet Cable", "Ímã de Power-ups", 4);
        addUpgradeItem("driver_data", "Data Recovery", "Chance de vida ao matar", 5);
        addUpgradeItem("driver_psu", "Power Supply", "Regeneração no Infinito", 6);
        addUpgradeItem("driver_motherboard", "Motherboard", "Vida Extra (+1 HP)", 7);
        addUpgradeItem("driver_firewall", "Firewall", "Proteção contra paredes", 8);
    }

    private void addUpgradeItem(String key, String name, String desc, int iconId) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.btn_retro_classic);
        
        int padding = getResources().getDimensionPixelSize(R.dimen.upgrade_item_padding);
        layout.setPadding(padding, padding, padding, padding);
        
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 16);
        layout.setLayoutParams(lp);

        int level = prefs.getInt(key + "_level", 0);
        int cost = getCostForLevel(level + 1, key.equals("driver_motherboard"));

        TextView title = new TextView(this);
        title.setText(name + " (LV " + level + "/5)");
        title.setTextColor(Color.BLACK);
        float titleSize = getResources().getDimension(R.dimen.upgrade_title_size) / getResources().getDisplayMetrics().density;
        title.setTextSize(titleSize);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView description = new TextView(this);
        description.setText(desc);
        description.setTextColor(Color.DKGRAY);
        description.setTextSize(12);

        AppCompatButton btnBuy = new AppCompatButton(this);
        btnBuy.setBackgroundResource(R.drawable.btn_retro_classic);
        btnBuy.setPadding(16, 8, 16, 8);
        
        // Force black text even when disabled
        int[][] states = new int[][] { new int[] { android.R.attr.state_enabled }, new int[] { -android.R.attr.state_enabled } };
        int[] colors = new int[] { Color.BLACK, Color.BLACK };
        btnBuy.setTextColor(new android.content.res.ColorStateList(states, colors));
        
        if (level >= 5) {
            btnBuy.setText("MAX");
            btnBuy.setEnabled(false);
        } else {
            btnBuy.setText("UPGRADE: " + cost + " BLOCKS");
            btnBuy.setOnClickListener(v -> {
                if (totalBlocks >= cost) {
                    if (checkMission(level + 1, key)) {
                        buyUpgrade(key, cost);
                        layout.removeAllViews(); 
                        refreshUpgradeItemLayout(layout, key, name, desc);
                    }
                } else {
                    Toast.makeText(this, "BLOCKS Insuficientes!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        layout.addView(title);
        layout.addView(description);
        layout.addView(btnBuy);
        container.addView(layout);
    }

    private void refreshUpgradeItemLayout(LinearLayout layout, String key, String name, String desc) {
        int level = prefs.getInt(key + "_level", 0);
        int cost = getCostForLevel(level + 1, key.equals("driver_motherboard"));

        TextView title = new TextView(this);
        title.setText(name + " (LV " + level + "/5)");
        title.setTextColor(Color.BLACK);
        float titleSize = getResources().getDimension(R.dimen.upgrade_title_size) / getResources().getDisplayMetrics().density;
        title.setTextSize(titleSize);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView description = new TextView(this);
        description.setText(desc);
        description.setTextColor(Color.DKGRAY);
        description.setTextSize(12);

        AppCompatButton btnBuy = new AppCompatButton(this);
        btnBuy.setBackgroundResource(R.drawable.btn_retro_classic);
        
        int[][] states = new int[][] { new int[] { android.R.attr.state_enabled }, new int[] { -android.R.attr.state_enabled } };
        int[] colors = new int[] { Color.BLACK, Color.BLACK };
        btnBuy.setTextColor(new android.content.res.ColorStateList(states, colors));

        if (level >= 5) {
            btnBuy.setText("MAX");
            btnBuy.setEnabled(false);
        } else {
            btnBuy.setText("UPGRADE: " + cost + " BLOCKS");
            btnBuy.setOnClickListener(v -> {
                if (totalBlocks >= cost && checkMission(level + 1, key)) {
                    buyUpgrade(key, cost);
                    layout.removeAllViews();
                    refreshUpgradeItemLayout(layout, key, name, desc);
                }
            });
        }
        layout.addView(title);
        layout.addView(description);
        layout.addView(btnBuy);
    }

    private int getCostForLevel(int level, boolean isMotherboard) {
        if (isMotherboard) return level == 1 ? 500 : 2000;
        switch (level) {
            case 1: return 50;
            case 2: return 125;
            case 3: return 300;
            case 4: return 700;
            case 5: return 1500;
            default: return 0;
        }
    }

    private boolean checkMission(int nextLevel, String key) {
        if (nextLevel < 4) return true;
        
        if (nextLevel == 4) {
            int bestRaid = prefs.getInt("best_raid", 0);
            if (bestRaid < 5) {
                Toast.makeText(this, "Missão: Chegue ao Raid 5 no Infinito!", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        
        if (nextLevel == 5) {
            boolean bossNoDamage = prefs.getBoolean("mission_boss_nodamage", false);
            if (!bossNoDamage) {
                Toast.makeText(this, "Missão: Vença um Boss sem tomar dano!", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        
        if (key.equals("driver_firewall")) {
            int wallsTime = prefs.getInt("best_walls_time", 0);
            if (wallsTime < 120) { 
                Toast.makeText(this, "Missão: Sobreviva 2 minutos no Modo Paredes!", Toast.LENGTH_LONG).show();
                return false;
            }
        }

        if (key.equals("driver_motherboard")) {
            int deaths = prefs.getInt("total_deaths", 0);
            if (deaths < 25) {
                Toast.makeText(this, "Missão: Tenha pelo menos 25 mortes!", Toast.LENGTH_LONG).show();
                return false;
            }
        }

        return true;
    }

    private void buyUpgrade(String key, int cost) {
        totalBlocks -= cost;
        int nextLevel = prefs.getInt(key + "_level", 0) + 1;
        prefs.edit()
            .putInt("total_blocks", totalBlocks)
            .putInt(key + "_level", nextLevel)
            .apply();
        SaveManager.saveData(this);
        updateBlocksDisplay();
        Toast.makeText(this, "Upgrade realizado!", Toast.LENGTH_SHORT).show();
    }
}
