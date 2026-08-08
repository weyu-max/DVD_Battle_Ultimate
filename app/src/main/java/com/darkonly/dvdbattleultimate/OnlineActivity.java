package com.darkonly.dvdbattleultimate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import java.util.List;
import java.util.Random;

public class OnlineActivity extends AppCompatActivity {

    private View selectionContainer, lobbyContainer, onlineSetupContainer;
    private LinearLayout playersLobbyList, roomsList;
    private TextView textWaitingStatus, textSearching;
    private EditText editRoomCode;
    private View btnPlay;
    
    private OnlineManager onlineManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isHost = false;

    @Override
    protected void onResume() {
        super.onResume();
        GameUtils.applyFullscreenIfEnabled(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (onlineManager != null) onlineManager.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GameUtils.applyLanguage(this);
        setContentView(R.layout.activity_lan); // Reutilizando layout base por enquanto, mas com ajustes

        onlineManager = OnlineManager.getInstance();
        setupOnlineListener();

        selectionContainer = findViewById(R.id.lan_selection_container);
        lobbyContainer = findViewById(R.id.lobby_container);
        
        // Customizando UI para Online
        ((TextView)findViewById(R.id.lan_title)).setText("BATALHA ONLINE");
        ((AppCompatButton)findViewById(R.id.btn_host_lan)).setText("CRIAR SALA PÚBLICA");
        ((AppCompatButton)findViewById(R.id.btn_join_lan)).setText("SALAS PRIVADAS");

        roomsList = findViewById(R.id.rooms_list);
        textSearching = findViewById(R.id.text_searching_rooms);
        playersLobbyList = findViewById(R.id.players_lobby_list);
        textWaitingStatus = findViewById(R.id.text_waiting_status);
        btnPlay = findViewById(R.id.btn_lan_play);

        findViewById(R.id.btn_host_lan).setOnClickListener(v -> createPublicRoom());
        findViewById(R.id.btn_join_lan).setOnClickListener(v -> showPrivateJoin());
        
        findViewById(R.id.btn_back_lan).setOnClickListener(v -> finish());
        
        btnPlay.setOnClickListener(v -> startGame());
        
        // Auto-connect to see public rooms
        onlineManager.connect(OnlineManager.DEFAULT_SERVER_URL, null);
    }

    private void setupOnlineListener() {
        onlineManager.setListener(new OnlineManager.OnlineListener() {
            @Override public void onConnected() {
                runOnUiThread(() -> textSearching.setText("Servidor Conectado! Buscando salas..."));
            }
            @Override public void onDisconnected() {
                runOnUiThread(() -> {
                    Toast.makeText(OnlineActivity.this, "Servidor Offline.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
            @Override public void onMessageReceived(String message) {
                if (message.startsWith("JOINED:")) {
                    runOnUiThread(() -> {
                        addPlayerToLobby("AMIGO ONLINE", false);
                        if (isHost) btnPlay.setEnabled(true);
                        textWaitingStatus.setText("PRONTO PARA RODAR!");
                    });
                } else if ("START_GAME".equals(message)) {
                    runOnUiThread(OnlineActivity.this::startGame);
                }
            }
            @Override public void onRoomsReceived(List<String> rooms) {
                if (rooms == null) return;
                runOnUiThread(() -> {
                    if (roomsList == null) return;
                    roomsList.removeAllViews();
                    textSearching.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);
                    if (rooms.isEmpty()) textSearching.setText("Nenhuma sala pública ativa.");
                    for (String code : rooms) addRoomButton(code);
                });
            }
            @Override public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(OnlineActivity.this, error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void addRoomButton(String code) {
        AppCompatButton btn = new AppCompatButton(this);
        btn.setText("SALA: " + code);
        btn.setBackgroundResource(R.drawable.btn_retro_classic);
        btn.setTextColor(Color.BLACK);
        btn.setOnClickListener(v -> joinRoom(code));
        roomsList.addView(btn);
    }

    private void createPublicRoom() {
        isHost = true;
        String code = generateRandomCode();
        onlineManager.send("CREATE:" + code);
        enterLobby(code);
    }

    private void showPrivateJoin() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("CÓDIGO DE 5 DÍGITOS");
        input.setGravity(android.view.Gravity.CENTER);
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("ENTRAR EM SALA PRIVADA")
               .setView(input)
               .setPositiveButton("ENTRAR", (d, w) -> {
                   String code = input.getText().toString().toUpperCase();
                   if (!code.isEmpty()) joinRoom(code);
               })
               .setNegativeButton("CANCELAR", null);
        
        final android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void joinRoom(String code) {
        isHost = false;
        onlineManager.send("JOIN:" + code);
        enterLobby(code);
    }

    private String generateRandomCode() {
        Random r = new Random();
        return String.format("%05d", r.nextInt(100000));
    }

    private void enterLobby(String code) {
        selectionContainer.setVisibility(View.GONE);
        lobbyContainer.setVisibility(View.VISIBLE);
        playersLobbyList.removeAllViews();
        addPlayerToLobby("VOCÊ", true);
        
        textWaitingStatus.setText("SALA: " + code + "\nAguardando amigo...");
        if (!isHost) btnPlay.setVisibility(View.GONE);
    }

    private void addPlayerToLobby(String name, boolean isMe) {
        LinearLayout playerView = new LinearLayout(this);
        playerView.setOrientation(LinearLayout.VERTICAL);
        playerView.setGravity(Gravity.CENTER);
        playerView.setPadding(20, 20, 20, 20);

        ImageView skinIcon = new ImageView(this);
        skinIcon.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        if (isMe) {
            skinIcon.setBackgroundColor(prefs.getInt("player_color", Color.YELLOW));
            skinIcon.setImageResource(prefs.getInt("player_eyes", R.drawable.eye_normal));
        } else {
            skinIcon.setBackgroundColor(Color.WHITE);
            skinIcon.setImageResource(R.drawable.eye_angry);
        }

        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextColor(Color.WHITE);

        playerView.addView(skinIcon);
        playerView.addView(nameText);
        playersLobbyList.addView(playerView);
    }

    private void startGame() {
        if (isHost) onlineManager.send("START_GAME");
        
        getSharedPreferences("GamePrefs", MODE_PRIVATE).edit()
            .putString("selected_mode", "lan") // Usa modo lan (multijogador) no GameView
            .putInt("num_players", 2)
            .putBoolean("is_online_match", true).apply();
        
        startActivity(new Intent(this, GameActivity.class));
        finish();
    }
}
