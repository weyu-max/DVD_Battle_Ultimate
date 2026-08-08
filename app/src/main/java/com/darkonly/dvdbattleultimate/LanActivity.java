package com.darkonly.dvdbattleultimate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

public class LanActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final String[] REQUIRED_PERMISSIONS;

    static {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_ADVERTISE,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.NEARBY_WIFI_DEVICES,
                    android.Manifest.permission.ACCESS_WIFI_STATE,
                    android.Manifest.permission.CHANGE_WIFI_STATE
            };
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            REQUIRED_PERMISSIONS = new String[]{
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_ADVERTISE,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_WIFI_STATE,
                    android.Manifest.permission.CHANGE_WIFI_STATE
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_WIFI_STATE,
                    android.Manifest.permission.CHANGE_WIFI_STATE
            };
        }
    }

    private View selectionContainer, hostConfigContainer, lobbyContainer, joinMenuContainer;
    private LinearLayout playersLobbyList, roomsList;
    private TextView textWaitingStatus, textFriendsCount;
    private View btnPlay;
    private int lanBotsCount = 3;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable starRunnable;

    private LanManager lanManager;
    private final Map<String, String> foundDevices = new HashMap<>();

    @Override
    protected void onResume() {
        super.onResume();
        GameUtils.applyFullscreenIfEnabled(this);
        if (starRunnable != null) handler.removeCallbacks(starRunnable);
        starRunnable = GameUtils.startStarAnimation(this, (android.view.ViewGroup)findViewById(android.R.id.content), handler);
        GameUtils.startBackgroundFlicker(findViewById(android.R.id.content), handler);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (starRunnable != null) handler.removeCallbacks(starRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lanManager != null) lanManager.stopAll();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GameUtils.applyLanguage(this);
        setContentView(R.layout.activity_lan);

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                lanManager = LanManager.getInstance(this);
                setupLanListener();
            } else {
                Toast.makeText(this, "Aviso: Nearby API pode ser instável no Android 6.0", Toast.LENGTH_SHORT).show();
            }
        } else {
            findViewById(R.id.btn_host_lan).setVisibility(View.GONE);
            findViewById(R.id.btn_join_lan).setVisibility(View.GONE);
        }

        selectionContainer = findViewById(R.id.lan_selection_container);
        hostConfigContainer = findViewById(R.id.host_config_container);
        lobbyContainer = findViewById(R.id.lobby_container);
        joinMenuContainer = findViewById(R.id.join_menu_container);
        playersLobbyList = findViewById(R.id.players_lobby_list);
        roomsList = findViewById(R.id.rooms_list);
        textWaitingStatus = findViewById(R.id.text_waiting_status);
        textFriendsCount = findViewById(R.id.text_lan_friends_count);
        btnPlay = findViewById(R.id.btn_lan_play);

        findViewById(R.id.btn_host_lan).setOnClickListener(v -> checkPermissionsAndDo(this::showHostConfig));
        findViewById(R.id.btn_join_lan).setOnClickListener(v -> checkPermissionsAndDo(this::startDiscovery));
        findViewById(R.id.btn_create_room).setOnClickListener(v -> createRoom());
        findViewById(R.id.btn_back_lan).setOnClickListener(v -> {
            if (selectionContainer.getVisibility() == View.VISIBLE) finish();
            else {
                selectionContainer.setVisibility(View.VISIBLE);
                hostConfigContainer.setVisibility(View.GONE);
                joinMenuContainer.setVisibility(View.GONE);
            }
        });

        btnPlay.setOnClickListener(v -> startGame());
    }

    private void checkPermissionsAndDo(Runnable action) {
        boolean allGranted = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) action.run();
        else ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    private void setupLanListener() {
        lanManager.setListener(new LanManager.LanListener() {
            @Override
            public void onDeviceFound(String endpointId, String deviceName) {
                runOnUiThread(() -> {
                    if (!foundDevices.containsKey(endpointId)) {
                        foundDevices.put(endpointId, deviceName);
                        addRoomButton(endpointId, deviceName);
                    }
                });
            }

            @Override
            public void onConnected(String endpointId) {
                runOnUiThread(() -> {
                    enterLobby(lanManager.isHost());
                    if (lanManager.isHost()) {
                        addPlayerToLobby("AMIGO CONECTADO", false);
                        btnPlay.setEnabled(true);
                        textWaitingStatus.setText("AMIGO PRONTO!");
                    }
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Toast.makeText(LanActivity.this, "Amigo desconectado.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onDataReceived(String data) {
                if ("START_GAME".equals(data)) {
                    runOnUiThread(LanActivity.this::startGame);
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(LanActivity.this, error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void addRoomButton(String id, String name) {
        findViewById(R.id.text_searching_rooms).setVisibility(View.GONE);
        AppCompatButton roomBtn = new AppCompatButton(this);
        roomBtn.setText(name.toUpperCase());
        roomBtn.setBackgroundResource(R.drawable.btn_retro_classic);
        roomBtn.setTextColor(android.graphics.Color.BLACK);
        roomBtn.setOnClickListener(v -> lanManager.connect(id));
        roomsList.addView(roomBtn);
    }

    private void showHostConfig() {
        selectionContainer.setVisibility(View.GONE);
        hostConfigContainer.setVisibility(View.VISIBLE);
        findViewById(R.id.btn_create_room).setVisibility(View.VISIBLE);
        
        lanBotsCount = 3;
        textFriendsCount.setText("BOTS: " + lanBotsCount);
        textFriendsCount.setOnClickListener(v -> {
            lanBotsCount = (lanBotsCount % 5) + 1;
            textFriendsCount.setText("BOTS: " + lanBotsCount);
        });
    }

    private void startDiscovery() {
        selectionContainer.setVisibility(View.GONE);
        joinMenuContainer.setVisibility(View.VISIBLE);
        roomsList.removeAllViews();
        foundDevices.clear();
        lanManager.startDiscovery();
    }

    private void createRoom() {
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        String name = prefs.getString("p1_name", "JOGADOR") + " (SALA)";
        lanManager.startHost(name);
        enterLobby(true);
    }

    private void enterLobby(boolean isHost) {
        hostConfigContainer.setVisibility(View.GONE);
        selectionContainer.setVisibility(View.GONE);
        joinMenuContainer.setVisibility(View.GONE);
        findViewById(R.id.btn_create_room).setVisibility(View.GONE);
        lobbyContainer.setVisibility(View.VISIBLE);
        playersLobbyList.removeAllViews();

        addPlayerToLobby("VOCÊ", true);
        
        if (isHost) {
            textWaitingStatus.setText(R.string.status_waiting_players);
            textWaitingStatus.setVisibility(View.VISIBLE);
            btnPlay.setEnabled(false);
        } else {
            addPlayerToLobby("ANFITRIÃO", false);
            textWaitingStatus.setVisibility(View.GONE);
            btnPlay.setVisibility(View.GONE);
        }
    }

    private void addPlayerToLobby(String name, boolean isMe) {
        LinearLayout playerView = new LinearLayout(this);
        playerView.setOrientation(LinearLayout.VERTICAL);
        playerView.setGravity(Gravity.CENTER);
        playerView.setPadding(16, 16, 16, 16);

        ImageView skinIcon = new ImageView(this);
        skinIcon.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
        
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        if (isMe) {
            int color = prefs.getInt("player_color", ContextCompat.getColor(this, R.color.player_color));
            skinIcon.setBackgroundColor(color);
            skinIcon.setImageResource(prefs.getInt("player_eyes", R.drawable.eye_normal));
        } else {
            skinIcon.setBackgroundColor(android.graphics.Color.WHITE);
            skinIcon.setImageResource(R.drawable.eye_angry);
        }

        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextColor(android.graphics.Color.WHITE);
        nameText.setGravity(Gravity.CENTER);

        playerView.addView(skinIcon);
        playerView.addView(nameText);
        playersLobbyList.addView(playerView);
    }

    private void startGame() {
        if (lanManager != null && lanManager.isHost()) {
            lanManager.sendData("START_GAME");
        }
        
        getSharedPreferences("GamePrefs", MODE_PRIVATE).edit()
            .putString("selected_mode", "lan")
            .putInt("num_players", 2)
            .putInt("sb_bots_count", lanBotsCount)
            .putBoolean("is_online_match", false)
            .putBoolean("sb_power_ups", true).apply();
        
        android.content.Intent intent = new android.content.Intent(this, GameActivity.class);
        startActivity(intent);
        finish();
    }
}
