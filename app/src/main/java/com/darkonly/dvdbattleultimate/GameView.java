package com.darkonly.dvdbattleultimate;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameView extends View {

    private Paint playerPaint, enemyPaint, hpBarPaint, hpBgPaint, auraPaint, dashBarPaint, dashBarBgPaint, wallPaint, flashPaint, namePaint, starPaint, borderPaint, countdownPaint;
    private Drawable eyesDrawable, mouthDrawable, boltDrawable, shieldDrawable, heartDrawable, pinkStarDrawable, bombDrawable, clockDrawable;
    
    private int auraType = 0, currentCombo = 0;
    private String currentTheme = "theme_space";
    private long lastKillTime = 0;
    private String comboMessage = "";
    private float comboMsgAlpha = 0;
    private int totalHits = 0;
    private final List<AuraParticle> particles = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<StaticStar> bgStars = new ArrayList<>();
    private long lastPowerUpSpawnTime = 0, lastStarSpawnTime = 0, lastFreezeTime = 0;
    private boolean isGlobalFreezeActive = false;

    private int killsCount = 0, totalBlocks = 0, dashesThisMatch = 0, killsThisDash = 0, healsThisMatch = 0, parriesThisMatch = 0;
    private boolean parriedBossThisMatch = false;
    private long infiniteStartTime = 0, matchStartTime = 0, lowHpStartTime = 0, currentMatchTime = 0;
    private int reviveCost = 50;
    private int storedPowerUpP1 = 0, storedPowerUpP2 = 0;
    private int wallHitsThisMatch = 0, pinkStarsThisMatch = 0;
    private boolean usedPowerUpInMatch = false;

    private float screenFlashAlpha = 0;
    private int screenFlashColor = Color.RED;
    private final List<DeathParticle> deathParticles = new ArrayList<>();
    private final List<RectF> playerTrail = new ArrayList<>();
    private static final int MAX_TRAIL = 8;

    private float shakeIntensity = 0;
    private final Random shakeRandom = new Random();
    private boolean isShakeEnabled = true;

    private ToneGenerator toneGenerator;
    private boolean isSoundEnabled = true;

    private int currentRaid = 1, bestRaid = 0;
    private boolean isInfiniteMode = false, isWallsMode = false, isRankedMode = false, isSysAdminMode = false, isPromotionMatch = false, isItemsEnabled = true, isPaused = false;
    private float adminPower = 100f;
    private long lastLagTime = 0, lastHealTime = 0;
    private final List<RectF> walls = new ArrayList<>();
    private final List<Square> enemiesToSpawn = new ArrayList<>();

    private final List<Square> players = new ArrayList<>();
    private final List<Square> enemies = new ArrayList<>();
    private final Random random = new Random();
    
    private boolean isGameOver = false, isStarting = true;
    private int countdown = 3;
    private final RectF arenaRect = new RectF();
    private static final float TARGET_ASPECT_RATIO = 4f / 3f;
    private float mGameScale = 1.0f; // Default scale 1.0

    private LanManager lanManager;
    private OnlineManager onlineManager;
    private boolean isLanMode = false;
    private boolean isOnlineMode = false;
    private boolean isLanClient = false;

    public void setLanManager(LanManager manager, boolean isClient) {
        this.lanManager = manager;
        this.isLanMode = true;
        this.isLanClient = isClient;
    }

    public void setOnlineManager(OnlineManager manager, boolean isClient) {
        this.onlineManager = manager;
        this.isOnlineMode = true;
        this.isLanClient = isClient; // Reutilizando a flag para "não-host"
    }

    private enum EnemyType {
        BASIC, TANK, OVERCLOCK, TROJAN, FIREWALL, MALWARE, CURSOR, 
        BOSS_SYSTEM32, BOSS_KERNEL, BOSS_RECYCLE, BOSS_TASKMGR, BOSS_DIALUP
    }

    private int lvCooling, lvBuffer, lvGpu, lvEthernet, lvData, lvPsu, lvMotherboard, lvFirewall;
    private long dashCooldown = 3000, regenInterval = 10000;
    private int invulnDuration = 500;

    public interface GameListener {
        void onGameOver(boolean win);
        void onScoreUpdate(int raid, int best);
        void onPowerUpUpdate(int playerIndex, int type);
        void onNotificationRequest(String title, String message, int iconRes);
    }
    
    private GameListener listener;
    public void setGameListener(GameListener listener) { this.listener = listener; }

    private final Handler gameHandler = new Handler(Looper.getMainLooper());

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void playTone(int type) {
        if (!isSoundEnabled) return;
        if (toneGenerator == null) toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        toneGenerator.startTone(type, 100);
    }

    private void init() {
        SharedPreferences prefs = getContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        isSoundEnabled = prefs.getBoolean("sound_enabled", true);
        isShakeEnabled = prefs.getBoolean("shake_enabled", true);
        isItemsEnabled = prefs.getBoolean("items_enabled", true);
        
        totalHits = prefs.getInt("total_hits", 0);
        totalBlocks = prefs.getInt("total_blocks", 0);
        matchStartTime = System.currentTimeMillis();
        usedPowerUpInMatch = false;

        lvCooling = prefs.getInt("driver_cooling_level", 0);
        lvBuffer = prefs.getInt("driver_buffer_level", 0);
        lvGpu = prefs.getInt("driver_gpu_level", 0);
        lvEthernet = prefs.getInt("driver_ethernet_level", 0);
        lvData = prefs.getInt("driver_data_level", 0);
        lvPsu = prefs.getInt("driver_psu_level", 0);
        lvMotherboard = prefs.getInt("driver_motherboard_level", 0);
        lvFirewall = prefs.getInt("driver_firewall_level", 0);
        currentTheme = prefs.getString("current_theme", "theme_space");

        dashCooldown = Math.max(1000, 3000L - (lvCooling * 400L));
        invulnDuration = 500 + (lvBuffer * 200);
        regenInterval = Math.max(5000, 10000L - (lvPsu * 1000L));

        playerPaint = new Paint(); 
        enemyPaint = new Paint(); enemyPaint.setColor(ContextCompat.getColor(getContext(), R.color.enemy_color));
        hpBarPaint = new Paint(); hpBarPaint.setColor(ContextCompat.getColor(getContext(), R.color.hp_green));
        hpBgPaint = new Paint(); hpBgPaint.setColor(ContextCompat.getColor(getContext(), R.color.hp_red));
        auraPaint = new Paint();
        dashBarPaint = new Paint(); dashBarPaint.setColor(ContextCompat.getColor(getContext(), R.color.imperial_blue));
        dashBarBgPaint = new Paint(); dashBarBgPaint.setColor(Color.DKGRAY);
        wallPaint = new Paint(); wallPaint.setColor(Color.LTGRAY);
        flashPaint = new Paint(); flashPaint.setColor(Color.WHITE);
        namePaint = new Paint(); namePaint.setColor(Color.WHITE); namePaint.setTextSize(20); namePaint.setTextAlign(Paint.Align.CENTER);
        starPaint = new Paint(); starPaint.setColor(Color.WHITE);
        borderPaint = new Paint(); borderPaint.setColor(Color.parseColor("#050005"));
        countdownPaint = new Paint(); countdownPaint.setColor(Color.WHITE); countdownPaint.setTextSize(200); countdownPaint.setTextAlign(Paint.Align.CENTER); countdownPaint.setTypeface(Typeface.DEFAULT_BOLD);

        boltDrawable = ContextCompat.getDrawable(getContext(), R.drawable.item_bolt);
        shieldDrawable = ContextCompat.getDrawable(getContext(), R.drawable.item_shield);
        heartDrawable = ContextCompat.getDrawable(getContext(), R.drawable.item_heart);
        pinkStarDrawable = ContextCompat.getDrawable(getContext(), R.drawable.star_pink);
        bombDrawable = ContextCompat.getDrawable(getContext(), R.drawable.item_bomb);
        clockDrawable = ContextCompat.getDrawable(getContext(), R.drawable.item_clock);

        for(int i=0; i<60; i++) bgStars.add(new StaticStar());

        gameHandler.post(updateTask);
    }

    public void startPromotionBoss() {
        if (isPromotionMatch && isStarting) {
            isStarting = false;
            int rank = getContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE).getInt("player_rank", 0);
            spawnBoss(rank >= 2 ? EnemyType.BOSS_KERNEL : EnemyType.BOSS_SYSTEM32);
            if (rank == 0 && !enemies.isEmpty()) {
                enemies.get(0).hp = 10;
            }
        }
    }

    public void startGameSession() {
        SharedPreferences prefs = getContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        String mode = prefs.getString("selected_mode", "classic");
        
        isInfiniteMode = "infinite".equals(mode) || "dual_core_infinite".equals(mode);
        isWallsMode = "walls".equals(mode);
        isRankedMode = "ranked".equals(mode);
        isSysAdminMode = "sysadmin".equals(mode);

        if ("sandbox".equals(mode) && prefs.getBoolean("sb_max_upgrades", true)) {
            lvCooling = lvBuffer = lvGpu = lvEthernet = lvData = lvPsu = lvMotherboard = lvFirewall = 5;
            dashCooldown = 500; invulnDuration = 2000; regenInterval = 2000;
        }
        
        auraType = prefs.getInt("player_aura", 0);

        if (isRankedMode && prefs.getInt("player_rp", 0) >= 100) {
            isPromotionMatch = true;
            isStarting = true; 
            // Carregamento antecipado do cenário de promoção
            int rank = prefs.getInt("player_rank", 0);
            spawnBoss(rank >= 2 ? EnemyType.BOSS_KERNEL : EnemyType.BOSS_SYSTEM32);
        } else {
            isPromotionMatch = false;
            int bots = "sandbox".equals(mode) ? prefs.getInt("sb_bots_count", 3) : 3;
            
            // Pré-carregamento absoluto da arena
            if (isWallsMode) generateWallPattern(800, 600); // Valores base temporários até onDraw
            
            if ("boss_rush".equals(mode)) spawnBoss(EnemyType.BOSS_SYSTEM32);
            else spawnEnemies(bots);
            
            startCountdown();
        }
    }

    private void startCountdown() {
        isStarting = true;
        countdown = 3;
        Runnable cdTask = new Runnable() {
            @Override public void run() {
                if (countdown > 1) { countdown--; playTone(ToneGenerator.TONE_PROP_BEEP); gameHandler.postDelayed(this, 1000); }
                else { isStarting = false; playTone(ToneGenerator.TONE_DTMF_D); }
            }
        };
        gameHandler.postDelayed(cdTask, 1000);
    }

    public void setPaused(boolean paused) { this.isPaused = paused; }
    public void setupPlayers(int count, String p1, String p2) {
        players.clear(); SharedPreferences pr = getContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        Square sq1 = new Square(100, 100, 80, true); sq1.name = p1; sq1.hp = 3 + lvMotherboard; sq1.color = pr.getInt("player_color", Color.YELLOW); players.add(sq1);
        if (count > 1) { Square sq2 = new Square(300, 100, 80, true); sq2.name = p2; sq2.hp = 3; sq2.color = pr.getInt("p2_color", Color.YELLOW); players.add(sq2); }
    }

    public float getDashProgress(int idx) {
        if (idx >= players.size()) return 0;
        Square p = players.get(idx);
        if (p.isSuperDashActive) return 1.0f;
        return Math.min(1f, (System.currentTimeMillis() - p.lastDashTime) / (float) dashCooldown);
    }

    public float getParryProgress(int idx) {
        if (idx >= players.size()) return 0;
        Square p = players.get(idx);
        return Math.min(1f, (System.currentTimeMillis() - p.lastParryTime) / 1000f);
    }

    private void spawnEnemies(int count) {
        SharedPreferences prefs = getContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        int rank = prefs.getInt("player_rank", 0);
        
        for (int i = 0; i < count; i++) {
            float[] pos = findValidPosition(80);
            Square e = new Square(pos[0], pos[1], 80, false);
            
            // Variedade Dinâmica baseada em Rank e Raid
            int effectiveLevel = rank + (isInfiniteMode ? currentRaid / 5 : 0);
            float r = random.nextFloat();
            
            if (effectiveLevel >= 1 && r < 0.2f) e.setEnemyType(EnemyType.TANK);
            else if (effectiveLevel >= 1 && r < 0.4f) e.setEnemyType(EnemyType.OVERCLOCK);
            else if (effectiveLevel >= 2 && r < 0.55f) e.setEnemyType(EnemyType.TROJAN);
            else if (effectiveLevel >= 2 && r < 0.70f) e.setEnemyType(EnemyType.FIREWALL);
            else if (effectiveLevel >= 3 && r < 0.85f) e.setEnemyType(EnemyType.MALWARE);
            else if (effectiveLevel >= 4 && r < 0.95f) e.setEnemyType(EnemyType.CURSOR);
            else e.setEnemyType(EnemyType.BASIC);
            
            // HP Escalonável no Infinito
            if (isInfiniteMode) {
                int hpBonus = currentRaid / 10;
                e.hp += hpBonus;
            }
            
            enemies.add(e);
        }
    }

    private void spawnBoss(EnemyType type) {
        float[] pos = findValidPosition(200);
        Square boss = new Square(pos[0], pos[1], 200, false);
        boss.isBoss = true; boss.setEnemyType(type);
        
        // Dificuldade progressiva no Infinito
        int bonusHp = isInfiniteMode ? (currentRaid / 5) : 0;
        boss.hp = (type == EnemyType.BOSS_KERNEL ? 20 : 15) + bonusHp;
        if (isInfiniteMode) boss.currentSpeedMultiplier = boss.originalSpeedMultiplier * (1.0f + (currentRaid * 0.05f));
        
        enemies.add(boss); shakeIntensity = 20;
    }

    private float[] findValidPosition(float size) {
        float w = arenaRect.width() > 0 ? arenaRect.width() : 800;
        float h = arenaRect.height() > 0 ? arenaRect.height() : 600;

        if (!players.isEmpty()) {
            Square p1 = players.get(0);
            float pX = p1.rect.centerX();
            float pY = p1.rect.centerY();

            float targetMinX = pX > w / 2 ? 0 : w / 2;
            float targetMaxX = pX > w / 2 ? w / 2 : w;
            float targetMinY = pY > h / 2 ? 0 : h / 2;
            float targetMaxY = pY > h / 2 ? h / 2 : h;

            for (int i = 0; i < 40; i++) {
                float x = targetMinX + random.nextInt((int) Math.max(1, (targetMaxX - targetMinX) - size));
                float y = targetMinY + random.nextInt((int) Math.max(1, (targetMaxY - targetMinY) - size));
                RectF temp = new RectF(x, y, x + size, y + size);
                
                boolean blocked = false;
                for (RectF wall : walls) if (RectF.intersects(temp, wall)) { blocked = true; break; }
                if (blocked) continue;

                // Check distance to ALL players
                for(Square p : players) {
                    if (Math.sqrt(Math.pow(temp.centerX()-p.rect.centerX(),2) + Math.pow(temp.centerY()-p.rect.centerY(),2)) < 400) { blocked = true; break; }
                }
                if (blocked) continue;

                // Avoid other enemies
                for(Square e : enemies) if(RectF.intersects(temp, e.rect)) { blocked = true; break; }
                if (blocked) continue;

                return new float[]{x, y};
            }
        }
        return new float[]{0, 0};
    }

    private void generateWallPattern(float w, float h) {
        walls.clear();
        int pattern = random.nextInt(10);
        float tw = 40; 
        switch (pattern) {
            case 0: walls.add(new RectF(w/2-tw, h/4, w/2+tw, 3*h/4)); walls.add(new RectF(w/4, h/2-tw, 3*w/4, h/2+tw)); break;
            case 1: walls.add(new RectF(w/5, h/5, w/5+tw*2, h/5+tw*2)); walls.add(new RectF(4*w/5-tw*2, h/5, 4*w/5, h/5+tw*2)); break;
            case 8: walls.add(new RectF(0, h/3, tw*2, 2*h/3)); walls.add(new RectF(w-tw*2, h/3, w, 2*h/3)); break;
            default: walls.add(new RectF(w/2-tw, h/2-tw, w/2+tw, h/2+tw)); break;
        }
    }

    private final Runnable updateTask = new Runnable() {
        @Override public void run() { update(); invalidate(); gameHandler.postDelayed(this, 33); }
    };

    private void update() {
        if (isGameOver || isPaused || isStarting) return;
        long now = System.currentTimeMillis();
        currentMatchTime = now - matchStartTime;

        if ((isLanMode || isOnlineMode) && isLanClient) {
            // No modo cliente, apenas atualizamos estrelas e partículas locais
            // A posição dos jogadores vem via handleLanData
            for(StaticStar s : bgStars) s.draw(null); // Só pra atualizar alpha/pos
            Iterator<AuraParticle> itAP = particles.iterator(); while (itAP.hasNext()) { AuraParticle ap = itAP.next(); ap.update(); if (ap.life <= 0) itAP.remove(); }
            Iterator<DeathParticle> itDP = deathParticles.iterator(); while (itDP.hasNext()) { DeathParticle dp = itDP.next(); dp.update(); if (dp.life <= 0) itDP.remove(); }
            if (shakeIntensity > 0) shakeIntensity *= 0.9f;
            if (screenFlashAlpha > 0) screenFlashAlpha -= 0.1f;
            return;
        }

        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        float screenRatio = (float) w / h;
        float aW, aH;
        if (screenRatio > TARGET_ASPECT_RATIO) { aH = h; aW = h * TARGET_ASPECT_RATIO; }
        else { aW = w; aH = w / TARGET_ASPECT_RATIO; }
        arenaRect.set((w - aW) / 2, (h - aH) / 2, (w + aW) / 2, (h + aH) / 2);
        
        // Calcular escala baseado na altura (Padrão: 600px de altura para arena)
        float baseH = arenaRect.height();
        if (baseH <= 0) baseH = 600f; // Force a sane default if height not yet ready
        
        mGameScale = baseH / 600f;
        if (mGameScale < 0.25f) mGameScale = 0.25f; 

        // Update existing objects scale if any
        for (Square p : players) p.rescale();
        for (Square en : enemies) en.rescale();

        SharedPreferences prefs = getContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        for (Square p : players) {
            int idx = players.indexOf(p);
            String prefix = idx == 0 ? "player_" : "p2_";
            
            String eyesName = prefs.getString(prefix + "eyes_name", "");
            int eyesRes = SaveManager.getResId(getContext(), eyesName, "drawable");
            p.eyesRes = eyesRes != 0 ? eyesRes : (idx == 0 ? prefs.getInt("player_eyes", R.drawable.eye_normal) : prefs.getInt("p2_eyes", R.drawable.eye_normal));
            
            String mouthName = prefs.getString(prefix + "mouth_name", "");
            int mouthRes = SaveManager.getResId(getContext(), mouthName, "drawable");
            p.mouthRes = mouthRes != 0 ? mouthRes : (idx == 0 ? prefs.getInt("player_mouth", R.drawable.mouth_happy) : prefs.getInt("p2_mouth", R.drawable.mouth_happy));
        }

        if (!players.isEmpty() && players.get(0).hp == 1) {
            if (lowHpStartTime == 0) lowHpStartTime = now;
            else if (now - lowHpStartTime > 30000 && !prefs.getBoolean("ach_bsod_unlocked", false)) {
                prefs.edit().putBoolean("ach_bsod_unlocked", true).apply();
                if (listener != null) listener.onNotificationRequest("CONQUISTA!", getContext().getString(R.string.ach_bsod_title), R.drawable.star_yellow);
            }
        } else lowHpStartTime = 0;

        if (shakeIntensity > 0) shakeIntensity *= 0.9f;
        if (screenFlashAlpha > 0) screenFlashAlpha -= 0.1f;
        
        if (random.nextFloat() < 0.008f) { screenFlashAlpha = 0.2f; screenFlashColor = Color.parseColor("#4B0082"); }

        if (isGlobalFreezeActive && now > lastFreezeTime + 5000) isGlobalFreezeActive = false;
        if (isSysAdminMode && adminPower < 100) adminPower = Math.min(100, adminPower + 0.1f);

        if (isWallsMode && walls.isEmpty()) generateWallPattern(arenaRect.width(), arenaRect.height());
        if (isItemsEnabled && now - lastPowerUpSpawnTime > 15000) { spawnPowerUp(false); lastPowerUpSpawnTime = now; }
        if (now - lastStarSpawnTime > 20000) { spawnPowerUp(true); lastStarSpawnTime = now; }

        for (Square p : players) if (p.hp > 0) { p.updateTimers(now); p.move((int)arenaRect.width(), (int)arenaRect.height()); }
        if (!players.isEmpty()) { playerTrail.add(0, new RectF(players.get(0).rect)); if (playerTrail.size() > MAX_TRAIL) playerTrail.remove(playerTrail.size() - 1); }
        
        Iterator<DeathParticle> itDP = deathParticles.iterator(); while (itDP.hasNext()) { DeathParticle dp = itDP.next(); dp.update(); if (dp.life <= 0) itDP.remove(); }
        Iterator<AuraParticle> itAP = particles.iterator(); while (itAP.hasNext()) { AuraParticle ap = itAP.next(); ap.update(); if (ap.life <= 0) itAP.remove(); }
        Iterator<Bullet> itB = bullets.iterator(); while (itB.hasNext()) { Bullet b = itB.next(); b.update(enemies); if (b.life <= 0 || b.hit) itB.remove(); }

        for (Square p : players) if (p.hp > 0 && auraType > 0 && auraType != 9) {
            int count = p.isDashing ? 3 : 1;
            for (int i = 0; i < count; i++) particles.add(new AuraParticle(p.rect.centerX() + arenaRect.left, p.rect.centerY() + arenaRect.top, auraType));
        }

        List<PowerUp> caught = new ArrayList<>();
        for (PowerUp pu : powerUps) {
            if (lvEthernet > 0 && !players.isEmpty()) {
                Square p1 = players.get(0);
                float dx = (p1.rect.centerX() + arenaRect.left) - pu.rect.centerX(), dy = (p1.rect.centerY() + arenaRect.top) - pu.rect.centerY();
                float d = (float)Math.sqrt(dx*dx + dy*dy);
                if (d < 300 + (lvEthernet * 50)) {
                    float pull = 1.5f + (lvEthernet * 0.5f);
                    pu.vx = (pu.vx * 0.92f) + (dx/d)*pull;
                    pu.vy = (pu.vy * 0.92f) + (dy/d)*pull;
                    // Limitar velocidade para não atravessar
                    float maxS = 15f;
                    float curS = (float)Math.sqrt(pu.vx*pu.vx + pu.vy*pu.vy);
                    if (curS > maxS) { pu.vx = (pu.vx/curS)*maxS; pu.vy = (pu.vy/curS)*maxS; }
                }
            }
            pu.move((int)arenaRect.width(), (int)arenaRect.height());
            for (Square p : players) if (p.hp > 0) {
                int pIdx = players.indexOf(p);
                boolean canCollect = (pIdx == 0 && storedPowerUpP1 == 0) || (pIdx == 1 && storedPowerUpP2 == 0);
                boolean isInstant = pu.type == 3; // Estrela rosa (5) agora é armazenável estrategicamente

                if (canCollect || isInstant) {
                    RectF pHitbox = new RectF(p.getHitbox()); pHitbox.offset(arenaRect.left, arenaRect.top);
                    if (p.isDashing) pHitbox.inset(-40, -40); // Aumentada área no dash para maior precisão
                    else pHitbox.inset(-10, -10); // Ligeira margem de erro para itens rápidos
                    
                    if (RectF.intersects(pHitbox, pu.rect)) {
                        if (pu.type == 3) { int m = pIdx == 0 ? (3+lvMotherboard) : 3; if (p.hp < m) { p.hp++; playTone(ToneGenerator.TONE_DTMF_1); } caught.add(pu); }
                        else {
                            storePowerUp(pu.type, pIdx);
                            if (pu.type == 5) pinkStarsThisMatch++;
                            prefs.edit().putInt("total_items_collected", prefs.getInt("total_items_collected", 0) + 1).apply();
                            caught.add(pu);
                        }
                        if (caught.contains(pu)) break;
                    }
                }
            }
        }
        powerUps.removeAll(caught);

        if (isInfiniteMode) {
            if (now - lastHealTime > regenInterval) {
                for (Square p : players) { int m = players.indexOf(p) == 0 ? (3+lvMotherboard) : 3; if (p.hp < m) { p.hp++; healsThisMatch++; } }
                lastHealTime = now;
            }
            if (enemies.isEmpty() && !isGameOver) {
                currentRaid++; if (currentRaid > bestRaid) { bestRaid = currentRaid; prefs.edit().putInt("best_raid", bestRaid).apply(); SaveManager.saveData(getContext()); }
                if (listener != null) listener.onScoreUpdate(currentRaid, bestRaid);
                
                // Evolução da Biblioteca de Processos (Raid-based)
                if (currentRaid % 25 == 0) spawnBoss(EnemyType.BOSS_DIALUP); // Dial-up Modem
                else if (currentRaid % 20 == 0) spawnBoss(EnemyType.BOSS_TASKMGR); // Task Manager
                else if (currentRaid % 15 == 0) spawnBoss(EnemyType.BOSS_RECYCLE); // Recycle Bin
                else if (currentRaid % 10 == 0) spawnBoss(EnemyType.BOSS_KERNEL); // Kernel
                else if (currentRaid % 5 == 0) spawnBoss(EnemyType.BOSS_SYSTEM32); // System32
                else if ("boss_rush".equals(prefs.getString("selected_mode", ""))) {
                    EnemyType[] bosses = {EnemyType.BOSS_SYSTEM32, EnemyType.BOSS_KERNEL, EnemyType.BOSS_RECYCLE, EnemyType.BOSS_TASKMGR, EnemyType.BOSS_DIALUP};
                    spawnBoss(bosses[random.nextInt(bosses.length)]);
                }
                else spawnEnemies(3 + currentRaid / 2);
            }
        }

        List<Square> deadE = new ArrayList<>();
        for (Square e : enemies) {
            float sM = e.currentSpeedMultiplier * (isGlobalFreezeActive ? 0.1f : 1f);
            if (isSysAdminMode && now < lastLagTime + 5000) sM *= 0.5f;
            e.move((int)arenaRect.width(), (int)arenaRect.height(), sM);
            for (Square p : players) if (p.hp > 0 && RectF.intersects(p.getHitbox(), e.getHitbox())) handleCollision(p, e);
            if (e.hp <= 0) { deadE.add(e); onEnemyDeath(e); }
        }
        enemies.removeAll(deadE);
        if (!enemiesToSpawn.isEmpty()) { enemies.addAll(enemiesToSpawn); enemiesToSpawn.clear(); }
        if (isLanMode && !isLanClient) {
            sendHostState();
        }
        if (isOnlineMode && !isLanClient) {
            sendHostState();
        }
        checkGameOver();
    }

    private void sendHostState() {
        if (lanManager == null && onlineManager == null) return;
        StringBuilder sb = new StringBuilder("H|");
        for (Square p : players) {
            sb.append(p.rect.left).append(",").append(p.rect.top).append(",")
              .append(p.hp).append(",").append(p.isDashing?1:0).append(",")
              .append(p.isParrying?1:0).append(",").append(p.color).append("|");
        }
        sb.append("#");
        for (Square e : enemies) {
            sb.append(e.rect.left).append(",").append(e.rect.top).append(",")
              .append(e.hp).append(",").append(e.enemyType.ordinal()).append(",")
              .append(e.isSniping?1:0).append(",").append(e.color).append(";");
        }
        sb.append("#");
        for (PowerUp pu : powerUps) {
            sb.append(pu.rect.left).append(",").append(pu.rect.top).append(",").append(pu.type).append(";");
        }
        sb.append("#");
        sb.append(screenFlashAlpha).append(",").append(screenFlashColor).append(",").append(shakeIntensity);
        
        String data = sb.toString();
        if (isLanMode && lanManager != null) lanManager.sendData(data);
        if (isOnlineMode && onlineManager != null) onlineManager.send(data);
    }

    public void handleLanData(String data) {
        if (data.startsWith("H|")) processHostState(data.substring(2));
        else if (data.startsWith("I|")) processClientInput(data.substring(2));
    }

    private void processClientInput(String data) {
        if (players.size() < 2) return;
        Square p2 = players.get(1); // O cliente sempre controla o P2 no Host
        if ("DASH".equals(data)) p2.dash();
        else if ("PARRY".equals(data)) p2.parry();
        else if ("USE_PU".equals(data)) usePowerUp(1);
    }

    private void processHostState(String data) {
        try {
            String[] parts = data.split("#");
            if (parts.length < 4) return;

            // Players
            String[] pData = parts[0].split("\\|");
            for (int i = 0; i < pData.length && i < players.size(); i++) {
                String[] v = pData[i].split(",");
                Square p = players.get(i);
                p.rect.offsetTo(Float.parseFloat(v[0]), Float.parseFloat(v[1]));
                p.hp = Integer.parseInt(v[2]);
                p.isDashing = v[3].equals("1");
                p.isParrying = v[4].equals("1");
                p.color = Integer.parseInt(v[5]);
            }

            // Enemies
            String[] eData = parts[1].split(";");
            enemies.clear();
            for (String s : eData) {
                if (s.isEmpty()) continue;
                String[] v = s.split(",");
                Square e = new Square(Float.parseFloat(v[0]), Float.parseFloat(v[1]), 80, false);
                e.hp = Integer.parseInt(v[2]);
                e.enemyType = EnemyType.values()[Integer.parseInt(v[3])];
                e.isSniping = v[4].equals("1");
                e.color = Integer.parseInt(v[5]);
                if (e.enemyType == EnemyType.BOSS_SYSTEM32 || e.enemyType == EnemyType.BOSS_KERNEL) e.size = 200;
                e.rect.set(e.rect.left, e.rect.top, e.rect.left + e.size, e.rect.top + e.size);
                enemies.add(e);
            }

            // Powerups
            String[] puData = parts[2].split(";");
            powerUps.clear();
            for (String s : puData) {
                if (s.isEmpty()) continue;
                String[] v = s.split(",");
                powerUps.add(new PowerUp(Float.parseFloat(v[0]), Float.parseFloat(v[1]), Integer.parseInt(v[2])));
            }

            // Effects
            String[] eff = parts[3].split(",");
            screenFlashAlpha = Float.parseFloat(eff[0]);
            screenFlashColor = Integer.parseInt(eff[1]);
            shakeIntensity = Float.parseFloat(eff[2]);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void spawnPowerUp(boolean forceStar) {
        int type = forceStar ? 5 : ( (random.nextFloat() < 0.4f && isLowHp()) ? 3 : (new int[]{1,2,4,6,7})[random.nextInt(5)] );
        float[] pos = findValidPosition(40);
        powerUps.add(new PowerUp(pos[0] + arenaRect.left, pos[1] + arenaRect.top, type));
    }
    private boolean isLowHp() { for(Square p : players) if(p.hp == 1) return true; return false; }

    private void storePowerUp(int type, int idx) {
        if (idx == 0) storedPowerUpP1 = type; else storedPowerUpP2 = type;
        if (listener != null) listener.onPowerUpUpdate(idx, type);
        playTone(ToneGenerator.TONE_DTMF_A);
    }

    public void usePowerUp(int idx) {
        if (isLanMode && isLanClient) {
            lanManager.sendData("I|USE_PU");
            return;
        }
        if (isOnlineMode && isLanClient) {
            onlineManager.send("I|USE_PU");
            return;
        }
        int type = (idx == 0) ? storedPowerUpP1 : storedPowerUpP2;
        if (type == 0 || isGameOver || isPaused || idx >= players.size()) return;
        Square p = players.get(idx); if (p.hp <= 0) return;
        usedPowerUpInMatch = true;
        switch (type) {
            case 1: p.isSuperDashActive = true; p.superDashEndTime = System.currentTimeMillis() + 5000; break;
            case 2: p.isShielded = true; break;
            case 4: 
            case 5: bullets.add(new Bullet(p.rect.centerX(), p.rect.centerY())); break;
            case 6: screenFlashAlpha = 0.8f; screenFlashColor = Color.WHITE; for(Square e : enemies) { e.hp -= (e.isBoss ? 3 : 1); if (e.hp < 0) e.hp = 0; } playTone(ToneGenerator.TONE_CDMA_PIP); break;
            case 7: isGlobalFreezeActive = true; lastFreezeTime = System.currentTimeMillis(); playTone(ToneGenerator.TONE_DTMF_C); break;
        }
        if (idx == 0) storedPowerUpP1 = 0; else storedPowerUpP2 = 0;
        if (listener != null) listener.onPowerUpUpdate(idx, 0);
        playTone(ToneGenerator.TONE_DTMF_B);
    }

    public void parry(int idx) {
        if (isLanMode && isLanClient) {
            lanManager.sendData("I|PARRY");
            return;
        }
        if (isOnlineMode && isLanClient) {
            onlineManager.send("I|PARRY");
            return;
        }
        if (idx < players.size()) players.get(idx).parry();
    }
    public float getAdminPower() { return adminPower; }
    public long getCurrentMatchTime() { return currentMatchTime; }
    public int getReviveCost() { return reviveCost; }
    
    public boolean revive() {
        SharedPreferences prefs = getContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        int blocks = prefs.getInt("total_blocks", 0);
        if (blocks >= reviveCost) {
            prefs.edit().putInt("total_blocks", blocks - reviveCost).apply();
            reviveCost *= 2;
            isGameOver = false;
            for (Square p : players) {
                p.hp = 1;
                p.makeInvulnerable(2000);
            }
            return true;
        }
        return false;
    }
    public boolean adminInjectShield() { if (adminPower >= 40 && !players.isEmpty()) { adminPower -= 40; players.get(0).isShielded = true; playTone(ToneGenerator.TONE_DTMF_A); return true; } return false; }
    public boolean adminHeal() { if (adminPower >= 60 && !players.isEmpty()) { Square p = players.get(0); if (p.hp < 3 + lvMotherboard) { adminPower -= 60; p.hp++; playTone(ToneGenerator.TONE_DTMF_1); return true; } } return false; }
    public boolean adminApplyLag() { if (adminPower >= 50) { adminPower -= 50; lastLagTime = System.currentTimeMillis(); screenFlashAlpha = 0.2f; screenFlashColor = Color.BLUE; playTone(ToneGenerator.TONE_DTMF_B); return true; } return false; }

    private void checkGameOver() {
        boolean dead = true; for (Square p : players) if (p.hp > 0) dead = false;
        if (dead && !players.isEmpty()) finalizeMatch(false);
        else if (enemies.isEmpty() && !isInfiniteMode) finalizeMatch(true);
    }

    private void finalizeMatch(boolean win) {
        isGameOver = true; SharedPreferences prefs = getContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE); SharedPreferences.Editor editor = prefs.edit();
        int matches = prefs.getInt("total_matches_played", 0) + 1;
        editor.putInt("total_matches_played", matches);
        boolean friendly = "sandbox".equals(prefs.getString("selected_mode", "")) || "boss_rush".equals(prefs.getString("selected_mode", ""));
        if (!friendly) totalBlocks += killsCount;
        editor.putInt("total_deaths", prefs.getInt("total_deaths", 0) + (win ? 0 : 1));
        saveMatchStats(editor, prefs, friendly, win);
        editor.putInt("total_blocks", totalBlocks);
        if (isInfiniteMode) { int s = (int)((System.currentTimeMillis()-infiniteStartTime)/1000); if(s > prefs.getInt("best_infinite_time", 0)) editor.putInt("best_infinite_time", s); }
        if (isRankedMode || "lan".equals(prefs.getString("selected_mode", ""))) {
            editor.putInt("season_games_played", prefs.getInt("season_games_played", 0) + 1);
            if ("lan".equals(prefs.getString("selected_mode", ""))) updateCoopRank(editor, prefs, win ? 25 : -10);
            else if (win) { if(isPromotionMatch) { processPromotion(prefs, editor); editor.remove("promotion_reach_time"); } else updateRank(editor, prefs, 20); }
            else if (isRankedMode) { if (isPromotionMatch) editor.putInt("player_rp", 0); else updateRank(editor, prefs, -15); }
        }
        editor.apply(); SaveManager.saveData(getContext());
        if (listener != null) listener.onGameOver(win);
    }

    private void updateRank(SharedPreferences.Editor ed, SharedPreferences prefs, int gain) {
        int rp = prefs.getInt("player_rp", 0) + gain;
        if (rp < 0) { int sub = prefs.getInt("player_sub_rank", 1); if (sub > 1) { ed.putInt("player_sub_rank", sub - 1); rp = 50; } else rp = 0; }
        else if (rp >= 100) { int sub = prefs.getInt("player_sub_rank", 1), rank = prefs.getInt("player_rank", 0); if (sub < (rank <= 1 ? 3 : rank <= 3 ? 4 : 5)) { ed.putInt("player_sub_rank", sub + 1); rp = 0; } else rp = 100; }
        ed.putInt("player_rp", rp);
    }

    private void updateCoopRank(SharedPreferences.Editor ed, SharedPreferences prefs, int gain) {
        int rp = Math.max(0, prefs.getInt("coop_rp", 0) + gain);
        if (rp >= 100) { int r = prefs.getInt("coop_rank", 0); if (r < 4) { ed.putInt("coop_rank", r + 1); rp = 0; } else rp = 100; }
        ed.putInt("coop_rp", rp);
    }

    private void onEnemyDeath(Square e) {
        long now = System.currentTimeMillis();
        if (now - lastKillTime < 1500) {
            currentCombo++;
            comboMsgAlpha = 1.0f;
            if (currentCombo == 2) comboMessage = "DOUBLE KILL!";
            else if (currentCombo == 3) comboMessage = "TRIPLE KILL!";
            else if (currentCombo == 4) comboMessage = "MULTI KILL!";
            else if (currentCombo >= 5) comboMessage = "HACKER!!!";
        } else {
            currentCombo = 1;
        }
        lastKillTime = now;

        spawnDeathExplosion(e.rect.centerX() + arenaRect.left, e.rect.centerY() + arenaRect.top, e.color);
        killsCount++;
        
        if (e.enemyType == EnemyType.TROJAN) {
            for(int i=0; i<4; i++) { 
                Square m = new Square(e.rect.centerX(), e.rect.centerY(), 40, false); 
                m.setEnemyType(EnemyType.BASIC); m.hp = 1; enemiesToSpawn.add(m); 
            }
        }
        
        if (e.enemyType == EnemyType.BOSS_SYSTEM32) {
            for(int i=0; i<6; i++) { 
                Square fr = new Square(e.rect.centerX(), e.rect.centerY(), 80, false); 
                fr.setEnemyType(EnemyType.BASIC); enemiesToSpawn.add(fr); 
            }
        }

        // BOSS: Task Manager (Clones ao morrer ou spawn de clones secundários)
        if (e.enemyType == EnemyType.BOSS_TASKMGR) {
            for(int i=0; i<3; i++) {
                Square cl = new Square(e.rect.centerX(), e.rect.centerY(), 80, false);
                cl.setEnemyType(EnemyType.OVERCLOCK);
                enemiesToSpawn.add(cl);
            }
        }

        if (random.nextFloat() < (lvData * 0.03f)) for (Square p : players) { int m = players.indexOf(p) == 0 ? (3+lvMotherboard) : 3; if(p.hp < m) p.hp++; }
        shakeIntensity = e.isBoss ? 50 : 25; playTone(ToneGenerator.TONE_PROP_NACK);
    }

    private void saveMatchStats(SharedPreferences.Editor ed, SharedPreferences prefs, boolean friendly, boolean win) {
        ed.putInt("total_kills", prefs.getInt("total_kills", 0) + killsCount);
        if (killsCount > prefs.getInt("max_kills_match", 0)) ed.putInt("max_kills_match", killsCount);
        int th = prefs.getInt("total_heals", 0) + healsThisMatch; ed.putInt("total_heals", th);
        if (th >= 50 && !prefs.getBoolean("ach_heal_master_unlocked", false)) { if(!friendly) totalBlocks += 40; ed.putBoolean("ach_heal_master_unlocked", true); }
        int tp = prefs.getInt("total_parries", 0) + parriesThisMatch; ed.putInt("total_parries", tp);
        if (tp >= 20 && !prefs.getBoolean("ach_parry_master_unlocked", false)) { if(!friendly) totalBlocks += 50; ed.putBoolean("ach_parry_master_unlocked", true); }
        if (parriedBossThisMatch && !prefs.getBoolean("ach_boss_parry_unlocked", false)) { if(!friendly) totalBlocks += 100; ed.putBoolean("ach_boss_parry_unlocked", true); }
        int twh = prefs.getInt("total_wall_hits", 0) + wallHitsThisMatch; ed.putInt("total_wall_hits", twh);
        if (twh >= 100 && !prefs.getBoolean("ach_ice_unlocked", false)) { if(!friendly) totalBlocks += 20; ed.putBoolean("ach_ice_unlocked", true); }
        int ts = prefs.getInt("total_pink_stars", 0) + pinkStarsThisMatch; ed.putInt("total_pink_stars", ts);
        if (ts >= 30 && !prefs.getBoolean("ach_star_collector_unlocked", false)) { if(!friendly) totalBlocks += 60; ed.putBoolean("ach_star_collector_unlocked", true); }
        if (killsCount >= 20 && !prefs.getBoolean("ach_glasses_sq_unlocked", false)) { if(!friendly) totalBlocks += 30; ed.putBoolean("ach_glasses_sq_unlocked", true); }
        if (killsCount >= 50 && !prefs.getBoolean("ach_glasses_rd_unlocked", false)) { if(!friendly) totalBlocks += 60; ed.putBoolean("ach_glasses_rd_unlocked", true); }
        if (prefs.getInt("total_matches_played", 0) >= 500 && !prefs.getBoolean("ach_dizzy_unlocked", false)) { if(!friendly) totalBlocks += 200; ed.putBoolean("ach_dizzy_unlocked", true); }
        if (prefs.getInt("total_deaths", 0) >= 200 && !prefs.getBoolean("ach_grumpy_unlocked", false)) { if(!friendly) totalBlocks += 50; ed.putBoolean("ach_grumpy_unlocked", true); }
        if (win && !usedPowerUpInMatch && !friendly) { if (!prefs.getBoolean("ach_pirate_unlocked", false)) { totalBlocks += 150; ed.putBoolean("ach_pirate_unlocked", true); } }
    }

    private void processPromotion(SharedPreferences prefs, SharedPreferences.Editor ed) {
        int r = prefs.getInt("player_rank", 0); if (r < 4) { ed.putInt("player_rank", r + 1); ed.putInt("player_sub_rank", 1); ed.putInt("player_rp", 0);
        gameHandler.post(() -> { playTone(ToneGenerator.TONE_DTMF_D); if (listener != null) listener.onNotificationRequest("RANK UP!", getContext().getString(R.string.promotion_success), R.drawable.btn_color_selection); }); }
    }

    private void spawnDeathExplosion(float x, float y, int color) { 
        int count = shakeIntensity > 30 ? 50 : 15; // Mais partículas para Boss
        for (int i = 0; i < count; i++) deathParticles.add(new DeathParticle(x, y, color)); 
    }

    private void handleCollision(Square p, Square e) {
        if (p.hp <= 0 || e.hp <= 0 || isGameOver) return;
        if (p.isParrying) { parriesThisMatch++; if (e.isBoss) parriedBossThisMatch = true; p.vx = (p.rect.centerX() < e.rect.centerX()) ? -3f : 3f; p.vy = (p.rect.centerY() < e.rect.centerY()) ? -3f : 3f; e.vx = -p.vx * 1.5f; e.vy = -p.vy * 1.5f; p.makeInvulnerable(300); e.hitByFlash = true; gameHandler.postDelayed(() -> e.hitByFlash = false, 100); playTone(ToneGenerator.TONE_DTMF_D); shakeIntensity = 12; return; }
        float oX = Math.min(p.rect.right, e.rect.right) - Math.max(p.rect.left, e.rect.left), oY = Math.min(p.rect.bottom, e.rect.bottom) - Math.max(p.rect.top, e.rect.top);
        if (p.isDashing) { if (!e.hitByDash) { e.hp--; e.hitByFlash = true; gameHandler.postDelayed(() -> e.hitByFlash = false, 100); e.hitByDash = true; shakeIntensity = 10; playTone(ToneGenerator.TONE_PROP_BEEP); } if (oX < oY) { if (p.rect.centerX() < e.rect.centerX()) e.rect.offset(oX + 5, 0); else e.rect.offset(-oX - 5, 0); } else { if (p.rect.centerY() < e.rect.centerY()) e.rect.offset(0, oY + 5); else e.rect.offset(0, -oY - 5); } return; }
        if (oX < oY) { if (p.rect.centerX() < e.rect.centerX()) { p.rect.offset(-oX/2-1, 0); e.rect.offset(oX/2+1, 0); } else { p.rect.offset(oX/2+1, 0); e.rect.offset(-oX/2-1, 0); } } else { if (p.rect.centerY() < e.rect.centerY()) { p.rect.offset(0, -oY/2-1); e.rect.offset(0, oY/2+1); } else { p.rect.offset(0, oY/2+1); p.rect.offset(0, -oY/2-1); } }
        if (e.enemyType == EnemyType.FIREWALL && !p.isDashing && random.nextBoolean()) { playTone(ToneGenerator.TONE_PROP_ACK); p.vx *= -1.5f; p.vy *= -1.5f; p.makeInvulnerable(300); return; }
        if (!p.isInvulnerable) { if (p.isShielded) { p.isShielded = false; e.hp--; e.hitByFlash = true; gameHandler.postDelayed(() -> e.hitByFlash = false, 100); shakeIntensity = 10; playTone(ToneGenerator.TONE_PROP_ACK); }
            else { if (e.enemyType == EnemyType.MALWARE) { p.lastDashTime = System.currentTimeMillis() + 3000; gameHandler.post(() -> Toast.makeText(getContext(), "ERRO: Dash Corrompido!", Toast.LENGTH_SHORT).show()); }
                p.hp--; p.hitByFlash = true; gameHandler.postDelayed(() -> p.hitByFlash = false, 100); e.hp--; e.hitByFlash = true; gameHandler.postDelayed(() -> e.hitByFlash = false, 100);
                screenFlashAlpha = 0.5f; screenFlashColor = Color.RED; shakeIntensity = 15; playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE); }
            p.vx *= -1; p.vy *= -1; e.vx *= -1; e.vy *= -1; p.makeInvulnerable(invulnDuration);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (getWidth() == 0) return;
        
        // Temas de Arena Imperiais
        int bgColor = Color.BLACK;
        if ("theme_matrix".equals(currentTheme)) bgColor = Color.parseColor("#000500");
        else if ("theme_cyber".equals(currentTheme)) bgColor = Color.parseColor("#050005");
        
        if (isInfiniteMode) {
            if (currentRaid % 5 == 0 || currentRaid % 5 == 4) {
                // Glitch effect near boss
                if (random.nextFloat() < 0.1f) bgColor = Color.parseColor("#1A0000"); // Red tint
                else if (random.nextFloat() < 0.05f) bgColor = Color.parseColor("#001A00"); // Green tint
            }
        }
        canvas.drawColor(bgColor);
        
        // Matrix bits effect if theme selected
        if ("theme_matrix".equals(currentTheme) && random.nextFloat() < 0.15f) {
            Paint mP = new Paint(); mP.setColor(Color.GREEN); mP.setTextSize(20); mP.setAlpha(100);
            canvas.drawText(random.nextBoolean()?"0":"1", random.nextInt(getWidth()), random.nextInt(getHeight()), mP);
        }
        Paint bp = new Paint(); bp.setColor(Color.parseColor("#050005"));
        canvas.drawRect(0, 0, getWidth(), arenaRect.top, bp); canvas.drawRect(0, arenaRect.bottom, getWidth(), getHeight(), bp);
        canvas.drawRect(0, arenaRect.top, arenaRect.left, arenaRect.bottom, bp); canvas.drawRect(arenaRect.right, arenaRect.top, getWidth(), arenaRect.bottom, bp);
        for(StaticStar s : bgStars) s.draw(canvas);
        if (screenFlashAlpha > 0.01f) { Paint flashP = new Paint(); flashP.setAlpha((int)(screenFlashAlpha * 255)); flashP.setColor(screenFlashColor); canvas.drawRect(0, 0, getWidth(), getHeight(), flashP); android.graphics.RadialGradient gradient = new android.graphics.RadialGradient(arenaRect.centerX(), arenaRect.centerY(), arenaRect.width() * 0.8f, screenFlashColor, Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP); flashP.setShader(gradient); flashP.setAlpha((int)(screenFlashAlpha * 180)); canvas.drawRect(arenaRect, flashP); }
        for (Square p : players) if (p.hp > 0 && auraType > 0) { auraPaint.setColor(p.color); auraPaint.setAlpha(40); canvas.drawCircle(p.rect.centerX() + arenaRect.left, p.rect.centerY() + arenaRect.top, p.size, auraPaint); }
        Paint op = new Paint(); op.setStyle(Paint.Style.STROKE); op.setStrokeWidth(6); op.setColor(Color.parseColor("#330033")); canvas.drawRect(arenaRect, op);
        if (isStarting && !isPromotionMatch) { Paint cp = new Paint(); cp.setColor(Color.WHITE); cp.setTextSize(200); cp.setTextAlign(Paint.Align.CENTER); cp.setTypeface(Typeface.DEFAULT_BOLD); float textHeight = cp.descent() - cp.ascent(); float centerOffset = (textHeight / 2) - cp.descent(); canvas.drawText(String.valueOf(countdown), arenaRect.centerX(), arenaRect.centerY() + centerOffset, cp); }

        if (comboMsgAlpha > 0.01f) {
            Paint comboP = new Paint(); comboP.setColor(Color.YELLOW); comboP.setTextSize(40 * mGameScale); comboP.setTextAlign(Paint.Align.CENTER); comboP.setTypeface(Typeface.DEFAULT_BOLD);
            comboP.setAlpha((int)(comboMsgAlpha * 255));
            canvas.drawText(comboMessage, arenaRect.centerX(), arenaRect.top + (80 * mGameScale), comboP);
            comboMsgAlpha -= 0.02f;
        }

        if (shakeIntensity > 0.1f && isShakeEnabled) { canvas.save(); canvas.translate((shakeRandom.nextFloat()-0.5f)*shakeIntensity, (shakeRandom.nextFloat()-0.5f)*shakeIntensity); }
        for (RectF w : walls) { RectF dW = new RectF(w); dW.offset(arenaRect.left, arenaRect.top); canvas.drawRect(dW, wallPaint); auraPaint.setStyle(Paint.Style.STROKE); auraPaint.setColor(Color.WHITE); auraPaint.setAlpha(50); auraPaint.setStrokeWidth(2); canvas.drawRect(dW, auraPaint); auraPaint.setStyle(Paint.Style.FILL); auraPaint.setAlpha(255); }
        for (AuraParticle p : particles) drawAuraParticle(canvas, p);
        for (DeathParticle dp : deathParticles) { auraPaint.setColor(dp.color); auraPaint.setAlpha((int)(dp.life * 255)); canvas.drawRect(dp.x, dp.y, dp.x + dp.size, dp.y + dp.size, auraPaint); }
        for (int i = 0; i < playerTrail.size(); i++) { playerPaint.setAlpha(80 - (i * 10)); RectF dT = new RectF(playerTrail.get(i)); dT.offset(arenaRect.left, arenaRect.top); canvas.drawRect(dT, playerPaint); }
        for (Square p : players) if (p.hp > 0) {
            playerPaint.setColor(p.color); playerPaint.setAlpha(p.isDashing ? 128 : 255); if (p.isParrying) playerPaint.setColor(Color.WHITE);
            RectF dR = new RectF(p.rect); dR.offset(arenaRect.left, arenaRect.top);
            canvas.drawRect(dR, p.hitByFlash ? flashPaint : playerPaint);
            
            auraPaint.setStyle(Paint.Style.STROKE); auraPaint.setStrokeWidth(4); auraPaint.setColor(p.color); auraPaint.setAlpha(100);
            canvas.drawRect(dR, auraPaint); auraPaint.setStyle(Paint.Style.FILL); auraPaint.setAlpha(255);
            
            if (p.isParrying) {
                auraPaint.setStyle(Paint.Style.STROKE); auraPaint.setStrokeWidth(10); auraPaint.setColor(Color.WHITE);
                canvas.drawRect(dR, auraPaint); auraPaint.setStyle(Paint.Style.FILL);
                Drawable hand = ContextCompat.getDrawable(getContext(), R.drawable.ic_parry_hand);
                if (hand != null) {
                    int s = (int)(p.size * 0.8f);
                    hand.setBounds((int)dR.centerX() - s/2, (int)dR.centerY() - s/2, (int)dR.centerX() + s/2, (int)dR.centerY() + s/2);
                    hand.draw(canvas);
                }
            }
            
            Drawable e = ContextCompat.getDrawable(getContext(), p.eyesRes), m = ContextCompat.getDrawable(getContext(), p.mouthRes);
            if (e != null && !p.hitByFlash) { e.setBounds((int)dR.left, (int)dR.top, (int)dR.right, (int)dR.bottom); e.draw(canvas); }
            if (m != null && !p.hitByFlash) { m.setBounds((int)dR.left, (int)dR.top, (int)dR.right, (int)dR.bottom); m.draw(canvas); }
            
            drawHpBarAt(canvas, p, dR);
            if (p.isShielded) {
                auraPaint.setStyle(Paint.Style.STROKE); auraPaint.setStrokeWidth(5); auraPaint.setColor(Color.CYAN);
                canvas.drawRect(dR, auraPaint); auraPaint.setStyle(Paint.Style.FILL);
            }
            if (p.name != null) canvas.drawText(p.name, dR.centerX(), dR.top - 20, namePaint);
        }
        for (PowerUp pu : powerUps) { 
            auraPaint.setColorFilter(null); // Clear any filter
            auraPaint.setStyle(Paint.Style.STROKE); 
            auraPaint.setStrokeWidth(2); 
            auraPaint.setColor(Color.WHITE); 
            canvas.drawRect(pu.rect, auraPaint); 
            auraPaint.setStyle(Paint.Style.FILL); 
            Drawable d = (pu.type==1?boltDrawable:(pu.type==2?shieldDrawable:(pu.type==3?heartDrawable:(pu.type==4?pinkStarDrawable:(pu.type==5?pinkStarDrawable:(pu.type==6?bombDrawable:clockDrawable)))))); 
            if(d!=null){ 
                d.setColorFilter(null);
                d.setBounds((int)pu.rect.left, (int)pu.rect.top, (int)pu.rect.right, (int)pu.rect.bottom); 
                d.draw(canvas); 
            } 
        }
        for (Bullet b : bullets) { canvas.save(); canvas.rotate(b.rotation, b.x + arenaRect.left, b.y + arenaRect.top); if (pinkStarDrawable != null) { int s = 20; pinkStarDrawable.setAlpha(255); pinkStarDrawable.setBounds((int)(b.x + arenaRect.left)-s, (int)(b.y+arenaRect.top)-s, (int)(b.x+arenaRect.left)+s, (int)(b.y+arenaRect.top)+s); pinkStarDrawable.draw(canvas); } canvas.restore(); }
        for (Square en : enemies) if (en.hp > 0) { RectF dR = new RectF(en.rect); dR.offset(arenaRect.left, arenaRect.top); auraPaint.setStyle(Paint.Style.STROKE); auraPaint.setStrokeWidth(3); auraPaint.setColor(Color.WHITE); auraPaint.setAlpha(255); canvas.drawRect(dR, auraPaint); auraPaint.setStyle(Paint.Style.FILL); if (en.enemyType == EnemyType.CURSOR && en.isSniping) { auraPaint.setColor(Color.RED); auraPaint.setAlpha(150); auraPaint.setStrokeWidth(4); canvas.drawLine(dR.centerX(), dR.centerY(), dR.centerX() + (en.vx * 200), dR.centerY() + (en.vy * 200), auraPaint); auraPaint.setAlpha(255); } enemyPaint.setColor(en.isSniping ? Color.YELLOW : en.color); canvas.drawRect(dR, en.hitByFlash ? flashPaint : enemyPaint); if (en.isBoss && !en.hitByFlash) { Drawable a = ContextCompat.getDrawable(getContext(), R.drawable.eye_angry); if (a != null) { a.setBounds((int)dR.left, (int)dR.top, (int)dR.right, (int)dR.bottom); a.draw(canvas); } } drawHpBarAt(canvas, en, dR); }
        if (shakeIntensity > 0.1f && isShakeEnabled) canvas.restore();
    }

    private void drawHpBarAt(Canvas canvas, Square s, RectF dR) { float bw = s.size, bh = getResources().getDimension(R.dimen.hp_bar_height), x = dR.left, y = dR.bottom + 10; canvas.drawRect(x, y, x + bw, y + bh, hpBgPaint); float m = s.isPlayer ? (3f+lvMotherboard) : (s.isBoss ? (s.enemyType==EnemyType.BOSS_KERNEL?20f:15f) : 3f); canvas.drawRect(x, y, x + (s.hp/m)*bw, y + bh, hpBarPaint); }

    private void drawAuraParticle(Canvas canvas, AuraParticle p) {
        auraPaint.setAlpha((int)(p.life*255)); auraPaint.setColor(p.color);
        if (p.type == 1) { auraPaint.setStyle(Paint.Style.STROKE); auraPaint.setStrokeWidth(2); canvas.drawCircle(p.x, p.y, p.radius, auraPaint); auraPaint.setStyle(Paint.Style.FILL); auraPaint.setAlpha((int)(p.life*100)); canvas.drawCircle(p.x, p.y, p.radius*0.8f, auraPaint); }
        else if (p.type == 3) { auraPaint.setStrokeWidth(3); canvas.drawLine(p.x, p.y, p.x + p.offsetX, p.y + p.offsetY, auraPaint); }
        else if (p.type == 6) { if (pinkStarDrawable != null) { pinkStarDrawable.setAlpha((int)(p.life * 255)); int s = (int)p.radius; pinkStarDrawable.setBounds((int)p.x - s, (int)p.y - s, (int)p.x + s, (int)p.y + s); pinkStarDrawable.draw(canvas); } }
        else canvas.drawCircle(p.x, p.y, p.radius, auraPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isGameOver || isPaused) return false;
        int act = event.getActionMasked();
        if (act == MotionEvent.ACTION_DOWN || act == MotionEvent.ACTION_POINTER_DOWN) {
            if (isLanMode && isLanClient) {
                lanManager.sendData("I|DASH");
                performClick();
                return true;
            }
            if (isOnlineMode && isLanClient) {
                onlineManager.send("I|DASH");
                performClick();
                return true;
            }
            float x = event.getX(event.getActionIndex());
            if (isLanMode || isOnlineMode) {
                if (!players.isEmpty()) players.get(0).dash();
            } else if (players.size() > 1) {
                if (x < getWidth() / 2f) players.get(1).dash(); else players.get(0).dash();
            }
            else if (!players.isEmpty()) players.get(0).dash();
            performClick(); return true;
        }
        return super.onTouchEvent(event);
    }

    @Override public boolean performClick() { return super.performClick(); }

    private class StaticStar {
        float x, y, size, alphaSpeed; float alpha = 0; boolean growing = true;
        StaticStar() { x = random.nextFloat(); y = random.nextFloat(); size = 1 + random.nextFloat() * 3; alphaSpeed = 0.005f + random.nextFloat() * 0.02f; }
        void draw(Canvas canvas) {
            if (growing) { alpha += alphaSpeed; if (alpha >= 0.6f) growing = false; }
            else { alpha -= alphaSpeed; if (alpha <= 0.1f) growing = true; }
            starPaint.setAlpha((int)(alpha * 255));
            canvas.drawCircle(x * getWidth(), y * getHeight(), size, starPaint);
            x += 0.0001f; y += 0.0001f; if (x > 1) x = 0; if (y > 1) y = 0;
        }
    }

    private class Square {
        RectF rect; float vx, vy, size = 80, originalSpeedMultiplier = 3f, currentSpeedMultiplier = 3f;
        int hp = 3, color, eyesRes = R.drawable.eye_normal, mouthRes = R.drawable.mouth_happy;
        String name; boolean isPlayer, isBoss, isShielded, isSuperDashActive, isDashing, isParrying, isInvulnerable, isSniping;
        long superDashEndTime, lastDashTime, lastParryTime, nextRandomMoveTime, nextSnipeTime;
        EnemyType enemyType = EnemyType.BASIC; boolean hitByDash, hitByFlash;

        Square(float x, float y, float baseSize, boolean player) { 
            this.isPlayer = player; 
            this.size = baseSize * mGameScale;
            this.rect = new RectF(x, y, x + size, y + size); 
            this.vx = (random.nextBoolean()?1:-1)*3f*mGameScale; 
            this.vy = (random.nextBoolean()?1:-1)*3f*mGameScale; 
            rescale();
        }
        void setEnemyType(EnemyType type) { 
            this.enemyType = type; 
            int[] cs = {Color.RED, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.WHITE, Color.parseColor("#FF8C00"), Color.parseColor("#FF1493"), Color.parseColor("#7FFF00")}; 
            this.color = cs[random.nextInt(cs.length)]; 
            switch(type){
                case TANK: hp=3; originalSpeedMultiplier=1.5f; color=Color.DKGRAY; break; 
                case OVERCLOCK: hp=1; originalSpeedMultiplier=6f; color=Color.YELLOW; break; 
                case TROJAN: hp=2; color=Color.parseColor("#A020F0"); break; 
                case FIREWALL: hp=2; color=Color.BLUE; break; 
                case MALWARE: hp=1; color=Color.GREEN; break; 
                case CURSOR: hp=1; color=Color.WHITE; break; 
                case BOSS_SYSTEM32: hp=15; color=Color.RED; break; 
                case BOSS_KERNEL: hp=20; color=Color.MAGENTA; break; 
                case BOSS_RECYCLE: hp=25; color=Color.GREEN; break;
                case BOSS_TASKMGR: hp=30; color=Color.CYAN; break;
                case BOSS_DIALUP: hp=40; color=Color.parseColor("#FF8C00"); break;
                case BASIC: hp=1; break;
            } 
            currentSpeedMultiplier=originalSpeedMultiplier;
            rescale();
        }

        void rescale() {
            float baseSize = 80;
            switch(enemyType){
                case TANK: baseSize = 80; break;
                case OVERCLOCK: baseSize = 40; break;
                case BOSS_SYSTEM32: case BOSS_KERNEL: case BOSS_RECYCLE: case BOSS_TASKMGR: case BOSS_DIALUP: baseSize = 200; break;
                default: baseSize = 80; break;
            }
            this.size = baseSize * mGameScale;
            this.vx = (vx > 0 ? 1 : -1) * 3f * mGameScale;
            this.vy = (vy > 0 ? 1 : -1) * 3f * mGameScale;
            rect.set(rect.left, rect.top, rect.left + size, rect.top + size);
        }
        RectF getHitbox() { if(isPlayer && isDashing && lvGpu > 0) { float b = lvGpu*15f; return new RectF(rect.left-b, rect.top-b, rect.right+b, rect.bottom+b); } return rect; }
        void move(int w, int h) { move(w, h, currentSpeedMultiplier); }
        void move(int w, int h, float s) {
            long now = System.currentTimeMillis();
            float scaledSpeed = s * mGameScale;
            if (!isPlayer) {
                if (enemyType == EnemyType.OVERCLOCK && now > nextRandomMoveTime) { vx=(random.nextBoolean()?1:-1)*3f*mGameScale; vy=(random.nextBoolean()?1:-1)*3f*mGameScale; nextRandomMoveTime=now+500+random.nextInt(1000); }
                
                // BOSS: Dial-up Modem (Lag hostil)
                if (enemyType == EnemyType.BOSS_DIALUP && random.nextFloat() < 0.01f) {
                    screenFlashAlpha = 0.3f; screenFlashColor = Color.BLACK;
                    playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE);
                }

                // BOSS: Recycle Bin (Revive inimigos)
                if (enemyType == EnemyType.BOSS_RECYCLE && random.nextFloat() < 0.005f && enemies.size() < 10) {
                    Square e = new Square(rect.centerX(), rect.centerY(), 80, false);
                    e.setEnemyType(EnemyType.BASIC);
                    enemiesToSpawn.add(e);
                }

                if (enemyType == EnemyType.CURSOR && !isSniping && now > nextSnipeTime) { Square t = null; float min = Float.MAX_VALUE; for(Square p : players) if(p.hp>0) { float d = (float)Math.sqrt(Math.pow(p.rect.centerX()-rect.centerX(),2)+Math.pow(p.rect.centerY()-rect.centerY(),2)); if(d<min){ min=d; t=p; } } if(t!=null){ isSniping=true; float dx=t.rect.centerX()-rect.centerX(), dy=t.rect.centerY()-rect.centerY(); vx=(dx/min)*15*mGameScale; vy=(dy/min)*15*mGameScale; nextSnipeTime=now+2000; gameHandler.postDelayed(()->isSniping=false, 500); } }
                if (isSniping) scaledSpeed *= 2f;
            }
            rect.offset(vx*scaledSpeed, 0); if(rect.left<0) { rect.left=0; rect.right=size; vx*=-1; onWallHit(); } else if(rect.right>w) { rect.right=w; rect.left=w-size; vx*=-1; onWallHit(); } else if(enemyType!=EnemyType.MALWARE && collidesWithAnyWall(rect)) { vx*=-1; onWallHit(); }
            rect.offset(0, vy*scaledSpeed); if(rect.top<0) { rect.top=0; rect.bottom=size; vy*=-1; onWallHit(); } else if(rect.bottom>h) { rect.bottom=h; rect.top=h-size; vy*=-1; onWallHit(); } else if(enemyType!=EnemyType.MALWARE && collidesWithAnyWall(rect)) { vy*=-1; onWallHit(); }
        }
        private void onWallHit() { 
            if(isPlayer) { 
                wallHitsThisMatch++; 
                // Apenas paredes internas (obstacles) dão dano no modo Walls
                // As bordas do dispositivo (vistas aqui como colisão externa) não dão dano
                if(isWallsMode && !isDashing && !isInvulnerable){ 
                    if (collidesWithAnyWall(rect)) { // Verifica se é uma parede interna
                        if(random.nextFloat()<(lvFirewall*0.1f)) playTone(ToneGenerator.TONE_PROP_ACK); 
                        else { hp--; makeInvulnerable(invulnDuration); }
                    }
                } 
            } 
            if(isBoss) shakeIntensity=15; 
            playTone(ToneGenerator.TONE_PROP_PROMPT); 
        }
        private boolean collidesWithAnyWall(RectF r) { for(RectF wl : walls) if(RectF.intersects(r, wl)) return true; return false; }
        void updateTimers(long now) { if(isSuperDashActive && now > superDashEndTime) isSuperDashActive=false; if(isParrying && now > lastParryTime+250) isParrying=false; }
        void dash() { if(isParrying) return; long now = System.currentTimeMillis(); if(isSuperDashActive || now-lastDashTime > dashCooldown) { isDashing=true; isInvulnerable=true; currentSpeedMultiplier=30f; lastDashTime=now; dashesThisMatch++; SharedPreferences pr = getContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE); if(dashesThisMatch > pr.getInt("max_dashes_match", 0)) pr.edit().putInt("max_dashes_match", dashesThisMatch).apply(); gameHandler.postDelayed(()->{ isDashing=false; killsThisDash=0; isInvulnerable=false; currentSpeedMultiplier=originalSpeedMultiplier; for(Square e:enemies) e.hitByDash=false; makeInvulnerable(invulnDuration); }, 200); } }
        void parry() { if(isGameOver || isPaused || isParrying || isDashing) return; long now = System.currentTimeMillis(); if(now < lastParryTime+1000) return; isParrying=true; lastParryTime=now; playTone(ToneGenerator.TONE_PROP_ACK); }
        void makeInvulnerable(int d) { isInvulnerable=true; gameHandler.postDelayed(()->isInvulnerable=false, d); }
    }

    private class AuraParticle {
        float x, y, radius, life = 1.0f, offsetX, offsetY; int color, type; boolean isSpecial;
        AuraParticle(float x, float y, int t) { this.x=x+(random.nextFloat()-0.5f)*40; this.y=y+(random.nextFloat()-0.5f)*40; this.radius=8+random.nextFloat()*15; this.type=t; switch(t){ case 1: color=Color.BLUE; break; case 2: color=(new int[]{Color.RED, Color.YELLOW, Color.parseColor("#FFA500")})[random.nextInt(3)]; break; case 3: color=Color.YELLOW; offsetX=(random.nextFloat()-0.5f)*60; offsetY=(random.nextFloat()-0.5f)*60; break; case 5: color=Color.parseColor("#A020F0"); isSpecial=random.nextFloat()>0.7f; break; default: color=Color.parseColor("#FF69B4"); break; } }
        void update() { life -= 0.05f; radius *= 0.98f; if(type==2) y-=5; if(type==6){ x+=(random.nextFloat()-0.5)*10; y+=(random.nextFloat()-0.5)*10; } }
    }

    private class Bullet {
        float x, y, vx, vy, rotation = 0, life = 3.0f; boolean hit = false;
        Bullet(float x, float y) { this.x=x; this.y=y; Square t=null; float min=Float.MAX_VALUE; for(Square e:enemies) { float d=(float)Math.sqrt(Math.pow(e.rect.centerX()-x,2)+Math.pow(e.rect.centerY()-y,2)); if(d<min){ min=d; t=e; } } if(t!=null){ float dx=t.rect.centerX()-x, dy=t.rect.centerY()-y; vx=(dx/min)*15; vy=(dy/min)*15; } }
        public void update(List<Square> ens) {
            x += vx; y += vy; rotation += 25; life -= 0.033f; 
            particles.add(new AuraParticle(x + arenaRect.left, y + arenaRect.top, 6));
            Square target = null; float minDist = 800;
            for (Square e : ens) { if (e.hp <= 0) continue; float d = (float) Math.sqrt(Math.pow(e.rect.centerX()-x,2)+Math.pow(e.rect.centerY()-y,2)); if (d < minDist) { minDist = d; target = e; } }
            if (target != null) { float dx = target.rect.centerX() - x, dy = target.rect.centerY() - y; vx = (vx * 0.88f) + (dx / minDist) * 2.5f; vy = (vy * 0.88f) + (dy / minDist) * 2.5f; }
            for (Square e : ens) { if (e.hp > 0 && e.rect.contains(x, y)) { e.hp--; hit = true; totalHits++; getContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE).edit().putInt("total_hits", totalHits).apply(); break; } }
        }
    }

    private class PowerUp {
        RectF rect; float vx, vy, size = 30; int type;
        PowerUp(float x, float y, int t) { this.rect=new RectF(x, y, x+size, y+size); this.type=t; this.vx=(random.nextBoolean()?1:-1)*3f; this.vy=(random.nextBoolean()?1:-1)*3f; }
        void move(int w, int h) { rect.offset(vx, vy); if(rect.left < arenaRect.left) { rect.left = arenaRect.left; rect.right = arenaRect.left + size; vx *= -1; } else if(rect.right > arenaRect.right) { rect.right = arenaRect.right; rect.left = arenaRect.right - size; vx *= -1; } if(rect.top < arenaRect.top) { rect.top = arenaRect.top; rect.bottom = arenaRect.top + size; vy *= -1; } else if(rect.bottom > arenaRect.bottom) { rect.bottom = arenaRect.bottom; rect.top = arenaRect.bottom - size; vy *= -1; } }
    }

    private class DeathParticle {
        float x, y, vx, vy, size, life = 1.0f; int color;
        DeathParticle(float x, float y, int c) { this.x=x; this.y=y; this.color=c; this.size=10+random.nextFloat()*15; this.vx=(random.nextFloat()-0.5f)*20; this.vy=(random.nextFloat()-0.5f)*20; }
        void update() { x+=vx; y+=vy; life-=0.05f; vx*=0.95f; vy*=0.95f; }
    }
}
