package com.darkonly.dvdbattleultimate;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ModesActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable starRunnable;
    private android.content.SharedPreferences prefs;

    @Override
    protected void onResume() {
        super.onResume();
        GameUtils.applyFullscreenIfEnabled(this);
        updateRankDisplay();
        
        View root = findViewById(R.id.modes_root);
        if (starRunnable != null) handler.removeCallbacks(starRunnable);
        starRunnable = GameUtils.startStarAnimation(this, (ViewGroup) root, handler);
        GameUtils.startBackgroundFlicker(root, handler);
    }

    private void updateRankDisplay() {
        prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        int rp = prefs.getInt("player_rp", 0);
        
        ProgressBar bar = findViewById(R.id.progress_upload);
        if (bar != null) bar.setProgress(rp);
        
        TextView percent = findViewById(R.id.text_upload_percent);
        if (percent != null) {
            percent.setText(getString(R.string.label_rp, rp));
        }

        displayCurrentEvent();
    }

    private void displayCurrentEvent() {
        int day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH);
        int week = ((day - 1) / 7) + 1;
        if (day > 28) week = 5;

        int nameRes, rulesRes;
        switch (week) {
            case 1: nameRes = R.string.event_1_name; rulesRes = R.string.event_1_rules; break;
            case 2: nameRes = R.string.event_2_name; rulesRes = R.string.event_2_rules; break;
            case 3: nameRes = R.string.event_3_name; rulesRes = R.string.event_3_rules; break;
            case 4: nameRes = R.string.event_4_name; rulesRes = R.string.event_4_rules; break;
            default: nameRes = R.string.event_5_name; rulesRes = R.string.event_5_rules; break;
        }

        TextView nameText = findViewById(R.id.text_event_name);
        TextView rulesText = findViewById(R.id.text_event_rules);
        if (nameText != null) nameText.setText(getString(nameRes));
        if (rulesText != null) rulesText.setText(getString(rulesRes));
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
        setContentView(R.layout.activity_modes);

        prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);

        // Click Listeners for Modes
        findViewById(R.id.btn_mode_classic).setOnClickListener(v -> startGame("classic"));
        findViewById(R.id.btn_mode_infinite).setOnClickListener(v -> startGame("infinite"));
        findViewById(R.id.btn_mode_walls).setOnClickListener(v -> startGame("walls"));
        findViewById(R.id.btn_mode_ranked).setOnClickListener(v -> startGame("ranked"));
        
        findViewById(R.id.btn_mode_online).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, OnlineActivity.class));
        });
        
        findViewById(R.id.btn_mode_lan).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, LanActivity.class));
        });

        findViewById(R.id.btn_mode_dual_core).setOnClickListener(v -> startGame("dual_core"));
        findViewById(R.id.btn_mode_sysadmin).setOnClickListener(v -> startGame("sysadmin"));
        
        findViewById(R.id.btn_mode_sandbox).setOnClickListener(v -> startGame("sandbox"));
        findViewById(R.id.btn_config_sandbox_icon).setOnClickListener(v -> showSandboxConfig());

        findViewById(R.id.btn_back_modes).setOnClickListener(v -> finish());
    }

    private void showSandboxConfig() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("CONFIGURAÇÃO IMPERIAL (SANDBOX)");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        CheckBox checkMax = new CheckBox(this);
        checkMax.setText(R.string.label_max_upgrades);
        checkMax.setChecked(prefs.getBoolean("sb_max_upgrades", true));

        CheckBox checkGod = new CheckBox(this);
        checkGod.setText("GOD MODE (INVENCÍVEL)");
        checkGod.setChecked(prefs.getBoolean("sb_god_mode", false));

        CheckBox checkBoss = new CheckBox(this);
        checkBoss.setText("PERMITIR BOSSES");
        checkBoss.setChecked(prefs.getBoolean("sb_boss_enabled", true));

        CheckBox checkP2 = new CheckBox(this);
        checkP2.setText("2 PLAYERS (LOCAL)");
        checkP2.setChecked(prefs.getInt("num_players", 1) == 2);

        EditText editBots = new EditText(this);
        editBots.setHint("Quantidade de Bots (Padrão: 3)");
        editBots.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        editBots.setText(String.valueOf(prefs.getInt("sb_bots_count", 3)));

        layout.addView(checkMax);
        layout.addView(checkGod);
        layout.addView(checkBoss);
        layout.addView(checkP2);
        layout.addView(editBots);

        builder.setView(layout);
        builder.setPositiveButton("SALVAR", (dialog, which) -> {
            int bots = 3;
            try { bots = Integer.parseInt(editBots.getText().toString()); } catch (Exception ignored) {}
            
            prefs.edit()
                .putBoolean("sb_max_upgrades", checkMax.isChecked())
                .putBoolean("sb_god_mode", checkGod.isChecked())
                .putBoolean("sb_boss_enabled", checkBoss.isChecked())
                .putInt("sb_bots_count", bots)
                .putInt("num_players", checkP2.isChecked() ? 2 : 1)
                .apply();
            Toast.makeText(this, "Configurações de Sandbox Salvas!", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("CANCELAR", null);
        builder.show();
    }

    private void startGame(String mode) {
        int players = (mode.equals("dual_core") || mode.equals("sysadmin") || mode.equals("lan")) ? 2 : 1;
        if (mode.equals("sandbox")) players = prefs.getInt("num_players", 1);
        
        prefs.edit()
            .putString("selected_mode", mode)
            .putInt("num_players", players)
            .putBoolean("items_enabled", true)
            .apply();
        finish(); 
    }
}
