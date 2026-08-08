package com.darkonly.dvdbattleultimate;

import androidx.annotation.NonNull;
import okhttp3.*;
import java.util.concurrent.TimeUnit;

public class OnlineManager {
    public static final String DEFAULT_SERVER_URL = "ws://192.168.1.100:8080";
    
    private static OnlineManager instance;
    private final OkHttpClient client;
    private WebSocket webSocket;
    private OnlineListener listener;
    private String currentRoomCode;

    public interface OnlineListener {
        void onConnected();
        void onDisconnected();
        void onMessageReceived(String message);
        void onRoomsReceived(java.util.List<String> rooms);
        void onError(String error);
    }

    private OnlineManager() {
        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    public static synchronized OnlineManager getInstance() {
        if (instance == null) instance = new OnlineManager();
        return instance;
    }

    public void setListener(OnlineListener listener) {
        this.listener = listener;
    }

    public void connect(String url, String roomCode) {
        this.currentRoomCode = roomCode;
        Request request = new Request.Builder().url(url).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                // Se roomCode for nulo, apenas conectamos para ver a lista
                if (currentRoomCode != null) {
                    send("JOIN:" + currentRoomCode);
                }
                send("LIST_ROOMS");
                if (listener != null) listener.onConnected();
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                if (text.startsWith("ROOMS:")) {
                    String[] rooms = text.substring(6).split(",");
                    java.util.List<String> roomList = new java.util.ArrayList<>();
                    for (String r : rooms) if (!r.isEmpty()) roomList.add(r);
                    if (listener != null) listener.onRoomsReceived(roomList);
                } else {
                    if (listener != null) listener.onMessageReceived(text);
                }
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                webSocket.close(1000, null);
                if (listener != null) listener.onDisconnected();
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                if (listener != null) listener.onError("Falha na conexão online: " + t.getMessage());
            }
        });
    }

    public void send(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        }
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "User logout");
        }
    }
}
