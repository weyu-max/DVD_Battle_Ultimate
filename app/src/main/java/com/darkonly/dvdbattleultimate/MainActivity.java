package com.darkonly.dvdbattleultimate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private String currentLang = "";

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        String lang = prefs.getString("language", "pt");
        
        Locale currentLocale;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            currentLocale = getResources().getConfiguration().getLocales().get(0);
        } else {
            currentLocale = getResources().getConfiguration().locale;
        }
        String currentActivityLang = currentLocale.getLanguage();
        
        if (!currentActivityLang.equals(lang)) {
            GameUtils.applyLanguage(this);
            recreate();
            return;
        }

        GameUtils.applyLanguage(this);
        GameUtils.applyFullscreenIfEnabled(this);
        loadCharacterPreview();
        updateSelectedModeText();
        updateBlocksDisplay();
        updateRankDisplay();
        checkSeasonReset();
        startMenuAnimations();
    }

    private void updateRankDisplay() {
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        int rank = prefs.getInt("player_rank", 0);
        int sub = prefs.getInt("player_sub_rank", 1);
        int rp = prefs.getInt("player_rp", 0);
        
        String[] rankNames = {
            getString(R.string.rank_bronze), 
            getString(R.string.rank_silver), 
            getString(R.string.rank_gold), 
            getString(R.string.rank_platinum), 
            getString(R.string.rank_diamond),
            getString(R.string.rank_master),
            getString(R.string.rank_legendary),
            getString(R.string.rank_supreme),
            getString(R.string.rank_imperial)
        };
        
        TextView rankNameText = findViewById(R.id.text_menu_rank_name);
        if (rankNameText != null) {
            rankNameText.setText(getString(R.string.label_rank, rankNames[rank], sub));
        }
        
        android.widget.ProgressBar bar = findViewById(R.id.progress_menu_rank);
        if (bar != null) bar.setProgress(rp);

        TextView uploadPercentText = findViewById(R.id.text_menu_upload_percent);
        if (uploadPercentText != null) uploadPercentText.setText(rp + "%");

        View rankContainer = findViewById(R.id.profile_rank_container);
        if (rankContainer != null) {
            rankContainer.setOnClickListener(v -> showRankInfo());
        }

        handlePromotionButton(prefs, rp);
        
        // Show reward if new rank reached
        int lastRew = prefs.getInt("last_rewarded_rank", 0);
        if (rank > lastRew) {
            SaveManager.processAllRewards(this);
            showRankRewardDialog(rank, rankNames[rank]);
        }
    }

    private void showRankRewardDialog(int rank, String name) {
        int[] rankPrizes = {0, 100, 250, 500, 1000, 2000, 5000, 10000, 25000};
        new android.app.AlertDialog.Builder(this)
            .setTitle("NOVA CLASSE ALCANÇADA!")
            .setMessage("Parabéns por atingir a classe " + name + "!\n\nRECOMPENSA: +" + rankPrizes[rank] + " BLOCKS")
            .setPositiveButton("RECEBER", null)
            .show();
    }

    private void handlePromotionButton(SharedPreferences prefs, int rp) {
        androidx.appcompat.widget.AppCompatButton btnJogar = findViewById(R.id.btn_jogar);
        if (btnJogar == null) return;

        String mode = prefs.getString("selected_mode", "classic");

        if ("ranked".equals(mode) && rp >= 100) {
            long reachTime = prefs.getLong("promotion_reach_time", 0);
            if (reachTime == 0) {
                reachTime = System.currentTimeMillis();
                prefs.edit().putLong("promotion_reach_time", reachTime).apply();
            }

            long elapsed = System.currentTimeMillis() - reachTime;
            long remaining = 30000 - elapsed;

            if (remaining > 0) {
                btnJogar.setEnabled(false);
                btnJogar.setText("FINALIZANDO... (" + (remaining / 1000) + "s)");
                btnJogar.setTextColor(Color.GRAY);
                // Refresh after 1s
                animationHandler.postDelayed(this::updateRankDisplay, 1000);
            } else {
                btnJogar.setEnabled(true);
                btnJogar.setText(R.string.promotion_title); // "Finalizing Installation..."
                btnJogar.setTextColor(Color.RED);
            }
        } else {
            prefs.edit().remove("promotion_reach_time").apply();
            btnJogar.setEnabled(true);
            btnJogar.setText(R.string.btn_jogar);
            btnJogar.setTextColor(Color.BLACK);
        }
    }

    private void showRankInfo() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("CLASSES DE PROCESSAMENTO");
        
        String info = "BRONZE: 3 Sub-ranks. O início da jornada.\n\n" +
                     "PRATA: 3 Sub-ranks. Inimigos mais rápidos.\n\n" +
                     "OURO: 4 Sub-ranks. Inimigos de elite.\n\n" +
                     "PLATINA: 4 Sub-ranks. Bosses frequentes.\n\n" +
                     "DIAMANTE: 5 Sub-ranks. O Rank Máximo!";
        
        builder.setMessage(info);
        builder.setPositiveButton("ENTENDI", null);
        builder.show();
    }

    private void checkSeasonReset() {
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        int lastResetWeek = prefs.getInt("last_reset_week", -1);
        
        java.util.Calendar current = java.util.Calendar.getInstance();
        int currentWeek = current.get(java.util.Calendar.WEEK_OF_YEAR);
        
        // Se for segunda-feira e a semana for diferente da última processada
        if (current.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY && 
            currentWeek != lastResetWeek) {
            
            processSeasonRewards(prefs);
            prefs.edit().putInt("last_reset_week", currentWeek).apply();
            SaveManager.saveData(this);
        }
    }

    private void processSeasonRewards(SharedPreferences prefs) {
        int rank = prefs.getInt("player_rank", 0);
        int gamesPlayed = prefs.getInt("season_games_played", 0);
        int blocksReward;
        String rewardMsg = "";

        if (gamesPlayed < 5) {
            if (rank > 0) {
                rank--;
                prefs.edit().putInt("player_rank", rank).putInt("player_sub_rank", 1).apply();
                rewardMsg = getString(R.string.season_relegation);
            }
        } else {
            switch (rank) {
                case 1: blocksReward = 300; break;
                case 2: blocksReward = 700; break;
                case 3: blocksReward = 1500; break;
                case 4: blocksReward = 4000; break;
                default: blocksReward = 100; break;
            }
            rewardMsg = getString(R.string.season_reward_title) + " +" + blocksReward + " BLOCKS";
            int totalBlocks = prefs.getInt("total_blocks", 0) + blocksReward;
            prefs.edit().putInt("total_blocks", totalBlocks).apply();
        }

        if (!rewardMsg.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("SEASON RECAP")
                .setMessage(rewardMsg)
                .setPositiveButton("OK", null)
                .show();
        }
        
        prefs.edit().putInt("season_games_played", 0).apply();
        updateBlocksDisplay();
    }

    private void updateBlocksDisplay() {
        TextView blocksText = findViewById(R.id.text_total_blocks);
        if (blocksText != null) {
            SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
            int total = prefs.getInt("total_blocks", 0);
            blocksText.setText(getString(R.string.label_blocks, total));
            
            // Exibir evento da semana se houver espaço
            int day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH);
            int week = ((day - 1) / 7) + 1;
            String eventName = "";
            switch (week) {
                case 1: eventName = "Semana 1: Patch Day"; break;
                case 2: eventName = "Semana 2: Buffer Overflow"; break;
                case 3: eventName = "Semana 3: Safe Mode"; break;
                case 4: eventName = "Semana 4: Cryptomining"; break;
                case 5: eventName = "Semana 5: Y2K Bug"; break;
            }
            if (day > 28) eventName = "Semana 5: Y2K Bug"; // Força semana 5 no fim do mês
            
            Log.d("GameEvent", "Evento Atual: " + eventName);
        }
    }

    private final Handler animationHandler = new Handler(Looper.getMainLooper());
    private Runnable blinkRunnable;
    private Runnable starRunnable;

    private void startMenuAnimations() {
        View container = findViewById(R.id.menu_player_preview_container);
        if (container == null) return;
        
        TranslateAnimation floatAnim = new TranslateAnimation(0, 0, 0, -20);
        floatAnim.setDuration(1500);
        floatAnim.setRepeatMode(Animation.REVERSE);
        floatAnim.setRepeatCount(Animation.INFINITE);
        container.startAnimation(floatAnim);
        
        ImageView eyesView = findViewById(R.id.menu_player_eyes);
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        int originalEyes = prefs.getInt("player_eyes", R.drawable.eye_normal);
        
        if (blinkRunnable != null) animationHandler.removeCallbacks(blinkRunnable);
        
        blinkRunnable = new Runnable() {
            @Override
            public void run() {
                eyesView.setImageResource(R.drawable.eye_closed);
                animationHandler.postDelayed(() -> eyesView.setImageResource(originalEyes), 150);
                animationHandler.postDelayed(this, 3000 + (long)(Math.random() * 3000));
            }
        };
        animationHandler.postDelayed(blinkRunnable, 3000);
        
        if (starRunnable != null) animationHandler.removeCallbacks(starRunnable);
        starRunnable = GameUtils.startStarAnimation(this, findViewById(R.id.main_root), animationHandler);
        GameUtils.startBackgroundFlicker(findViewById(R.id.main_root), animationHandler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (blinkRunnable != null) animationHandler.removeCallbacks(blinkRunnable);
        if (starRunnable != null) animationHandler.removeCallbacks(starRunnable);
    }

    private void updateSelectedModeText() {
        TextView modeText = findViewById(R.id.text_selected_mode);
        if (modeText == null) return;
        
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        String mode = prefs.getString("selected_mode", "classic");
        
        int modeNameRes;
        
        switch (mode) {
            case "infinite": modeNameRes = R.string.label_mode_infinite; break;
            case "walls": modeNameRes = R.string.label_mode_walls; break;
            case "ranked": modeNameRes = R.string.label_mode_ranked; break;
            case "dual_core": modeNameRes = R.string.mode_dual_core; break;
            case "sysadmin": modeNameRes = R.string.mode_sysadmin; break;
            case "lan": modeNameRes = R.string.mode_lan; break;
            case "sandbox": modeNameRes = R.string.mode_sandbox; break;
            case "boss_rush": modeNameRes = R.string.mode_boss_rush; break;
            default: modeNameRes = R.string.label_mode_classic; break;
        }
        
        modeText.setText(getString(R.string.label_selected_mode, getString(modeNameRes)));
    }

    private void loadCharacterPreview() {
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        
        View colorView = findViewById(R.id.menu_player_color);
        ImageView eyesView = findViewById(R.id.menu_player_eyes);
        ImageView mouthView = findViewById(R.id.menu_player_mouth);
        
        View profileColor = findViewById(R.id.profile_color);
        ImageView profileEyes = findViewById(R.id.profile_eyes);
        ImageView profileMouth = findViewById(R.id.profile_mouth);

        if (colorView == null) return;

        // Default P1 is Yellow
        int color = prefs.getInt("player_color", Color.YELLOW);
        colorView.setBackgroundColor(color);
        if (profileColor != null) profileColor.setBackgroundColor(color);
        
        String eyesName = prefs.getString("player_eyes_name", "");
        int eyesRes = SaveManager.getResId(this, eyesName, "drawable");
        if (eyesRes == 0) eyesRes = R.drawable.eye_normal;
        eyesView.setImageResource(eyesRes);
        if (profileEyes != null) profileEyes.setImageResource(eyesRes);
        
        String mouthName = prefs.getString("player_mouth_name", "");
        int mouthRes = SaveManager.getResId(this, mouthName, "drawable");
        if (mouthRes == 0) mouthRes = R.drawable.mouth_happy;
        mouthView.setImageResource(mouthRes);
        if (profileMouth != null) profileMouth.setImageResource(mouthRes);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SaveManager.checkPermissions(this);
        SaveManager.loadData(this);
        DisplayUtils.logDisplayInfo(this);

        GameUtils.applyLanguage(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Scale UI based on device
        float scale = DisplayUtils.getUIScale(this);
        if (scale < 1.0f) {
            View wrapper = findViewById(R.id.main_content_wrapper);
            if (wrapper != null) {
                wrapper.setScaleX(scale);
                wrapper.setScaleY(scale);
            }
        }
        
        GameUtils.showTutorial(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btn_modos).setOnClickListener(v -> {
            Intent intent = new Intent(this, ModesActivity.class);
            startActivity(intent);
        });
            
        findViewById(R.id.btn_personagem).setOnClickListener(v -> {
            Intent intent = new Intent(this, CharacterActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_upgrades).setOnClickListener(v -> {
            Intent intent = new Intent(this, UpgradesActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_loja).setOnClickListener(v -> {
            Intent intent = new Intent(this, StoreActivity.class);
            startActivity(intent);
        });
            
        findViewById(R.id.btn_config).setOnClickListener(v -> {
            Intent intent = new Intent(this, ConfigActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_achievements).setOnClickListener(v -> {
            Intent intent = new Intent(this, AchievementsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_jogar).setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
            String mode = prefs.getString("selected_mode", "classic");
            
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("infinite_mode", "infinite".equals(mode));
            intent.putExtra("walls_mode", "walls".equals(mode));
            startActivity(intent);
        });
    }
}
