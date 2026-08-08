package com.darkonly.dvdbattleultimate;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;
import java.nio.charset.StandardCharsets;

@RequiresApi(24)
public class LanManager {
    private static final Strategy STRATEGY = Strategy.P2P_STAR;
    private static final String SERVICE_ID = "com.darkonly.dvdbattleultimate.LAN_SERVICE";

    private static LanManager instance;
    private final ConnectionsClient connectionsClient;
    private final Context context;

    private String connectedEndpointId;
    private boolean isHost = false;

    public interface LanListener {
        void onDeviceFound(String endpointId, String deviceName);
        void onConnected(String endpointId);
        void onDisconnected();
        void onDataReceived(String data);
        void onError(String error);
    }

    private LanListener listener;

    private LanManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectionsClient = Nearby.getConnectionsClient(this.context);
    }

    public static synchronized LanManager getInstance(Context context) {
        if (instance == null) instance = new LanManager(context);
        return instance;
    }

    public void setListener(LanListener listener) { this.listener = listener; }

    public void startHost(String name) {
        isHost = true;
        AdvertisingOptions options = new AdvertisingOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startAdvertising(name, SERVICE_ID, connectionLifecycleCallback, options)
                .addOnFailureListener(e -> { if(listener != null) listener.onError("Falha ao criar sala: " + e.getMessage()); });
    }

    public void startDiscovery() {
        isHost = false;
        DiscoveryOptions options = new DiscoveryOptions.Builder().setStrategy(STRATEGY).build();
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
                .addOnFailureListener(e -> { if(listener != null) listener.onError("Falha ao buscar salas: " + e.getMessage()); });
    }

    public void stopAll() {
        connectionsClient.stopAdvertising();
        connectionsClient.stopDiscovery();
        connectionsClient.stopAllEndpoints();
        connectedEndpointId = null;
    }

    public void connect(String endpointId) {
        connectionsClient.requestConnection(android.os.Build.MODEL, endpointId, connectionLifecycleCallback)
                .addOnFailureListener(e -> { if(listener != null) listener.onError("Falha ao conectar: " + e.getMessage()); });
    }

    public void sendData(String data) {
        if (connectedEndpointId != null) {
            connectionsClient.sendPayload(connectedEndpointId, Payload.fromBytes(data.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info) {
            connectionsClient.acceptConnection(endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            if (result.getStatus().isSuccess()) {
                connectedEndpointId = endpointId;
                connectionsClient.stopAdvertising();
                connectionsClient.stopDiscovery();
                if (listener != null) listener.onConnected(endpointId);
            } else {
                if (listener != null) listener.onError("Conexão recusada ou falhou.");
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            connectedEndpointId = null;
            if (listener != null) listener.onDisconnected();
        }
    };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
            if (listener != null) listener.onDeviceFound(endpointId, info.getEndpointName());
        }

        @Override
        public void onEndpointLost(@NonNull String endpointId) {}
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            if (payload.getType() == Payload.Type.BYTES) {
                byte[] bytes = payload.asBytes();
                if (bytes != null) {
                    String data = new String(bytes, StandardCharsets.UTF_8);
                    if (listener != null) listener.onDataReceived(data);
                }
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {}
    };
    
    public boolean isHost() { return isHost; }
}
