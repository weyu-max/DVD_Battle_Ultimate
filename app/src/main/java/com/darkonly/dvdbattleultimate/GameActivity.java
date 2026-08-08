package com.darkonly.dvdbattleultimate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    private View overlay, overlayPause, sysadminPanel, p2ActionContainer;
    private TextView textStatus, textScore, textMatchTimer, textPowerUpP1, textPowerUpP2, textAdminPower;
    private GameView gameView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable uiRunnable;

    @Override
    protected void onResume() {
        super.onResume();
        GameUtils.applyFullscreenIfEnabled(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GameUtils.applyLanguage(this);
        setContentView(R.layout.activity_game);

        overlay = findViewById(R.id.overlay_game_over);
        overlayPause = findViewById(R.id.overlay_pause);
        sysadminPanel = findViewById(R.id.sysadmin_panel);
        p2ActionContainer = findViewById(R.id.p2_action_container);
        textStatus = findViewById(R.id.text_status);
        textScore = findViewById(R.id.text_score);
        textMatchTimer = findViewById(R.id.text_match_timer);
        textPowerUpP1 = findViewById(R.id.text_powerup_p1);
        textPowerUpP2 = findViewById(R.id.text_powerup_p2);
        textAdminPower = findViewById(R.id.text_admin_power);
        gameView = findViewById(R.id.game_view);

        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        String selectedMode = prefs.getString("selected_mode", "classic");
        boolean infinite = "infinite".equals(selectedMode);
        boolean walls = "walls".equals(selectedMode);
        boolean ranked = "ranked".equals(selectedMode);
        boolean sysadmin = "sysadmin".equals(selectedMode);
        boolean isOnline = prefs.getBoolean("is_online_match", false);
        boolean items = prefs.getBoolean("items_enabled", true);
        int numPlayers = prefs.getInt("num_players", 1);
        String p1Name = prefs.getString("p1_name", "P1");
        String p2Name = prefs.getString("p2_name", "P2");

        gameView.setupPlayers(numPlayers, p1Name, p2Name);
        
        if ("lan".equals(selectedMode)) {
            if (isOnline) {
                OnlineManager onlineManager = OnlineManager.getInstance();
                gameView.setOnlineManager(onlineManager, true); // Por enquanto simplificado
                onlineManager.setListener(new OnlineManager.OnlineListener() {
                    @Override public void onConnected() {}
                    @Override public void onDisconnected() {
                        runOnUiThread(() -> {
                            android.widget.Toast.makeText(GameActivity.this, "Conexão Online Perdida!", android.widget.Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                    @Override public void onMessageReceived(String data) {
                        runOnUiThread(() -> gameView.handleLanData(data));
                    }
                    @Override public void onRoomsReceived(java.util.List<String> rooms) {}
                    @Override public void onError(String error) {}
                });
            } else {
                LanManager lanManager = LanManager.getInstance(this);
                gameView.setLanManager(lanManager, !lanManager.isHost());
                lanManager.setListener(new LanManager.LanListener() {
                    @Override public void onDeviceFound(String id, String name) {}
                    @Override public void onConnected(String id) {}
                    @Override public void onDisconnected() {
                        runOnUiThread(() -> {
                            android.widget.Toast.makeText(GameActivity.this, "Conexão Perdida!", android.widget.Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                    @Override public void onDataReceived(String data) {
                        runOnUiThread(() -> gameView.handleLanData(data));
                    }
                    @Override public void onError(String error) {}
                });
            }
        }

        gameView.startGameSession();
        
        if (numPlayers > 1 && !"lan".equals(selectedMode)) {
            p2ActionContainer.setVisibility(View.VISIBLE);
        }
        
        if (sysadmin) {
            sysadminPanel.setVisibility(View.VISIBLE);
            setupAdminButtons();
        }

        if (infinite) textScore.setVisibility(View.VISIBLE);

        if (ranked && prefs.getInt("player_rp", 0) >= 100) {
            View btnStartPromotion = findViewById(R.id.btn_start_promotion);
            btnStartPromotion.setVisibility(View.VISIBLE);
            btnStartPromotion.setOnClickListener(v -> {
                btnStartPromotion.setVisibility(View.GONE);
                gameView.startPromotionBoss();
            });
        }

        gameView.setGameListener(new GameView.GameListener() {
            @Override
            public void onGameOver(boolean win) {
                runOnUiThread(() -> {
                    findViewById(R.id.btn_pause).setVisibility(View.GONE);
                    overlay.setVisibility(View.VISIBLE);
                    textStatus.setText(win ? R.string.win_text : R.string.lose_text);
                    textStatus.setTextColor(win ? android.graphics.Color.GREEN : android.graphics.Color.RED);
                    
                    SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
                    String mode = prefs.getString("selected_mode", "classic");

                    // 1. Mostrar Rank se for Ranqueada
                    if ("ranked".equals(mode)) {
                        findViewById(R.id.container_game_over_rank).setVisibility(View.VISIBLE);
                        updateGameOverRankUI(prefs);
                    }

                    // 2. Bloquear Continuar se precisar de promoção
                    androidx.appcompat.widget.AppCompatButton btnRestart = findViewById(R.id.btn_recomecar);
                    if (btnRestart != null) {
                        btnRestart.setText(win ? R.string.btn_continuar : R.string.btn_recomecar);
                        if (win && "ranked".equals(mode) && prefs.getInt("player_rp", 0) >= 100) {
                            btnRestart.setEnabled(false);
                            btnRestart.setText("PROMOÇÃO NECESSÁRIA");
                            btnRestart.setAlpha(0.5f);
                        }
                    }
                    
                    // Salvar blocks imperiais ganhos na partida
                    SaveManager.processAllRewards(GameActivity.this);
                });
            }

            @Override
            public void onScoreUpdate(int raid, int best) {
                runOnUiThread(() -> textScore.setText(getString(R.string.raid_text, raid) + " | " + getString(R.string.best_text, best)));
            }

            @Override
            public void onPowerUpUpdate(int playerIndex, int type) {
                runOnUiThread(() -> {
                    ImageView img = (playerIndex == 0) ? findViewById(R.id.img_powerup_p1) : findViewById(R.id.img_powerup_p2);
                    TextView text = (playerIndex == 0) ? textPowerUpP1 : textPowerUpP2;
                    
                    if (img == null) return;

                    if (type == 0) {
                        img.setVisibility(View.GONE);
                        text.setText(R.string.powerup_none);
                    } else {
                        int iconRes = 0;
                        int nameRes = 0;
                        switch (type) {
                            case 1: iconRes = R.drawable.item_bolt; nameRes = R.string.powerup_bolt; break;
                            case 2: iconRes = R.drawable.item_shield; nameRes = R.string.powerup_shield; break;
                            case 4: iconRes = R.drawable.star_pink; nameRes = R.string.powerup_bullet; break;
                            case 5: iconRes = R.drawable.star_pink; nameRes = R.string.powerup_bullet; break;
                            case 6: iconRes = R.drawable.item_bomb; nameRes = R.string.powerup_bomb; break;
                            case 7: iconRes = R.drawable.item_clock; nameRes = R.string.powerup_freeze; break;
                        }
                        if (iconRes != 0) {
                            img.setImageResource(iconRes);
                            img.setVisibility(View.VISIBLE);
                            text.setText(nameRes);
                        }
                    }
                });
            }

            @Override
            public void onNotificationRequest(String title, String message, int iconRes) {
                runOnUiThread(() -> GameUtils.showNotification(GameActivity.this, title, message, iconRes));
            }
        });

        findViewById(R.id.btn_action).setOnClickListener(v -> gameView.usePowerUp(0));
        findViewById(R.id.btn_parry_p1).setOnClickListener(v -> gameView.parry(0));
        findViewById(R.id.btn_action_p2).setOnClickListener(v -> gameView.usePowerUp(1));
        findViewById(R.id.btn_parry_p2).setOnClickListener(v -> gameView.parry(1));

        findViewById(R.id.btn_pause).setOnClickListener(v -> {
            gameView.setPaused(true);
            overlayPause.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.btn_resume).setOnClickListener(v -> {
            gameView.setPaused(false);
            overlayPause.setVisibility(View.GONE);
        });

        View.OnClickListener restartListener = v -> {
            if ("lan".equals(selectedMode)) {
                Intent intent = new Intent(this, LanActivity.class);
                intent.putExtra("return_to_lobby", true);
                startActivity(intent);
                finish();
            } else {
                recreate();
            }
        };
        findViewById(R.id.btn_recomecar).setOnClickListener(restartListener);
        findViewById(R.id.btn_pause_recomecar).setOnClickListener(restartListener);

        View.OnClickListener menuListener = v -> finish();
        findViewById(R.id.btn_menu).setOnClickListener(menuListener);
        findViewById(R.id.btn_pause_menu).setOnClickListener(menuListener);

        startUiLoop();
    }

    private void setupAdminButtons() {
        findViewById(R.id.btn_inject_shield).setOnClickListener(v -> gameView.adminInjectShield());
        findViewById(R.id.btn_force_heal).setOnClickListener(v -> gameView.adminHeal());
        findViewById(R.id.btn_system_lag).setOnClickListener(v -> gameView.adminApplyLag());
        findViewById(R.id.btn_admin_help).setOnClickListener(v -> showAdminHelp());
    }

    private void showAdminHelp() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("COMANDOS DE ADMIN");
        builder.setMessage("SHIELD (40% CPU): Protege o P1.\nHEAL (60% CPU): Cura 1 HP.\nLAG (50% CPU): Lerdeza nos inimigos por 5s.");
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void updateGameOverRankUI(SharedPreferences prefs) {
        int rank = prefs.getInt("player_rank", 0);
        int sub = prefs.getInt("player_sub_rank", 1);
        int rp = prefs.getInt("player_rp", 0);
        
        String[] rankNames = {
            getString(R.string.rank_bronze), getString(R.string.rank_silver), 
            getString(R.string.rank_gold), getString(R.string.rank_platinum), 
            getString(R.string.rank_diamond), getString(R.string.rank_master),
            getString(R.string.rank_legendary), getString(R.string.rank_supreme),
            getString(R.string.rank_imperial)
        };
        
        TextView rankText = findViewById(R.id.text_over_rank_name);
        rankText.setText(getString(R.string.label_rank, rankNames[rank], sub));
        
        android.widget.ProgressBar bar = findViewById(R.id.progress_over_rank);
        bar.setProgress(rp);
        
        TextView uploadText = findViewById(R.id.text_over_upload);
        uploadText.setText(getString(R.string.label_rp, rp));
    }

    private void startUiLoop() {
        uiRunnable = new Runnable() {
            @Override
            public void run() {
                if (textAdminPower != null && textAdminPower.getVisibility() == View.VISIBLE) {
                    int power = (int)gameView.getAdminPower();
                    textAdminPower.setText(getString(R.string.label_admin_power, power));
                }

                // Update HUD Timer
                if (textMatchTimer != null) {
                    long millis = gameView.getCurrentMatchTime();
                    int seconds = (int) (millis / 1000) % 60;
                    int minutes = (int) ((millis / (1000 * 60)) % 60);
                    textMatchTimer.setText(String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds));
                    if (gameView.getVisibility() == View.VISIBLE && textMatchTimer.getVisibility() == View.GONE) {
                        textMatchTimer.setVisibility(View.VISIBLE);
                    }
                }
                
                // Update P1 Bars
                android.widget.ProgressBar dashP1 = findViewById(R.id.progress_dash_p1);
                android.widget.ProgressBar parryP1 = findViewById(R.id.progress_parry_p1);
                if (dashP1 != null) dashP1.setProgress((int)(gameView.getDashProgress(0) * 100));
                if (parryP1 != null) parryP1.setProgress((int)(gameView.getParryProgress(0) * 100));
                
                // Update P2 Bars
                android.widget.ProgressBar dashP2 = findViewById(R.id.progress_dash_p2);
                android.widget.ProgressBar parryP2 = findViewById(R.id.progress_parry_p2);
                if (dashP2 != null && p2ActionContainer.getVisibility() == View.VISIBLE) {
                    dashP2.setProgress((int)(gameView.getDashProgress(1) * 100));
                    parryP2.setProgress((int)(gameView.getParryProgress(1) * 100));
                }

                handler.postDelayed(this, 50);
            }
        };
        handler.post(uiRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uiRunnable != null) handler.removeCallbacks(uiRunnable);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (overlayPause.getVisibility() == View.VISIBLE) {
            gameView.setPaused(false);
            overlayPause.setVisibility(View.GONE);
        } else {
            gameView.setPaused(true);
            overlayPause.setVisibility(View.VISIBLE);
        }
    }
}
