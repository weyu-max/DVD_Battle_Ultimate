package com.darkonly.dvdbattleultimate;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Base64;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SaveManager {

    private static final int REQUEST_CODE = 101;
    private static final String SECRET_KEY = "DVD_IMPERIAL_V13_KEY";
    private static final int CURRENT_SAVE_VERSION = 3; 
    private static boolean isLoaded = false;

    private static String getFolderPath(Context context) {
        return context.getExternalFilesDir(null).getPath() + "/saves";
    }

    private static String getFileName() {
        return "options.json";
    }

    public static void checkPermissions(Activity activity) {
        java.util.List<String> perms = new java.util.ArrayList<>();
        perms.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        perms.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        perms.add(android.Manifest.permission.INTERNET);
        perms.add(android.Manifest.permission.ACCESS_NETWORK_STATE);
        perms.add(android.Manifest.permission.ACCESS_WIFI_STATE);
        perms.add(android.Manifest.permission.CHANGE_WIFI_STATE);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            perms.add(android.Manifest.permission.BLUETOOTH_SCAN);
            perms.add(android.Manifest.permission.BLUETOOTH_ADVERTISE);
            perms.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            perms.add(android.Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        java.util.List<String> toRequest = new java.util.ArrayList<>();
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(p);
            }
        }
        
        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity, toRequest.toArray(new String[0]), REQUEST_CODE);
        }
    }

    public static String getResName(Context context, int resId) {
        try { return context.getResources().getResourceEntryName(resId); }
        catch (Exception e) { return ""; }
    }

    public static int getResId(Context context, String resName, String type) {
        if (resName == null || resName.isEmpty()) return 0;
        try { return context.getResources().getIdentifier(resName, type, context.getPackageName()); }
        catch (Exception e) { return 0; }
    }

    public static void saveData(Context context) {
        try {
            File folder = new File(getFolderPath(context));
            if (!folder.exists()) folder.mkdirs();

            SharedPreferences prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
            Map<String, ?> allEntries = prefs.getAll();

            JSONObject root = new JSONObject();
            JSONObject general = new JSONObject();
            JSONObject secure = new JSONObject();

            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if (isSecureKey(key)) secure.put(key, val);
                else general.put(key, val);
            }

            root.put("version", CURRENT_SAVE_VERSION);
            root.put("general", general);
            
            String secureStr = secure.toString();
            String encryptedSecure = Base64.encodeToString(xorEncryptDecrypt(secureStr.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);
            root.put("secure", encryptedSecure);

            File file = new File(folder, getFileName());
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(root.toString(4).getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception e) {
            Log.e("SaveManager", "Error saving hybrid data: " + e.getMessage());
        }
    }

    private static boolean isSecureKey(String key) {
        return key.contains("block") || key.contains("rank") || key.contains("ach_") || key.contains("unlocked") || key.contains("total_kills") || key.contains("bought_");
    }

    private static byte[] xorEncryptDecrypt(byte[] data) {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);
        }
        return result;
    }

    public static void loadData(Context context) {
        if (isLoaded) return;
        SharedPreferences shared = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        
        try {
            File file = new File(getFolderPath(context), getFileName());
            if (!file.exists()) { 
                // Fallback to legacy SharedPreferences if JSON doesn't exist
                processAllRewards(context);
                isLoaded = true; 
                return; 
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            int readSize = fis.read(data);
            fis.close();

            if (readSize <= 0) { isLoaded = true; return; }

            String jsonStr = new String(data, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(jsonStr);
            SharedPreferences.Editor editor = shared.edit();

            if (root.has("general")) {
                JSONObject general = root.getJSONObject("general");
                applyJsonToEditor(general, editor);
            }

            if (root.has("secure")) {
                try {
                    String encryptedStr = root.getString("secure");
                    byte[] decoded = Base64.decode(encryptedStr, Base64.DEFAULT);
                    String decrypted = new String(xorEncryptDecrypt(decoded), StandardCharsets.UTF_8);
                    JSONObject secure = new JSONObject(decrypted);
                    applyJsonToEditor(secure, editor);
                } catch (Exception e) {
                    Log.e("SaveManager", "Secure decrypt failed, skipping to avoid data loss.");
                }
            }

            editor.apply();
            processAllRewards(context);
            isLoaded = true;
        } catch (Exception e) {
            Log.e("SaveManager", "Critical error loading hybrid data: " + e.getMessage());
            isLoaded = true; // Still mark as loaded to prevent retry loops
        }
    }

    private static void applyJsonToEditor(JSONObject json, SharedPreferences.Editor editor) throws Exception {
        java.util.Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);
            if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) editor.putInt(key, (Integer) value);
            else if (value instanceof Long) editor.putLong(key, (Long) value);
            else if (value instanceof Double) editor.putFloat(key, ((Double) value).floatValue());
            else if (value instanceof String) editor.putString(key, (String) value);
        }
    }

    public static void processAllRewards(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        int blocksToAdd = 0;

        Map<String, Integer> achRewards = new java.util.HashMap<>();
        achRewards.put("ach_okay_unlocked", 50);
        achRewards.put("ach_fast_unlocked", 100);
        achRewards.put("ach_on_target_unlocked", 50);
        achRewards.put("ach_sync_unlocked", 150);
        achRewards.put("ach_it_support_unlocked", 200);
        achRewards.put("ach_secure_server_unlocked", 300);
        achRewards.put("ach_ctrl_unlocked", 100);
        achRewards.put("ach_pirate_unlocked", 150);
        achRewards.put("ach_master_unlocked", 300);
        achRewards.put("ach_ice_unlocked", 20);
        achRewards.put("ach_heal_master_unlocked", 100);
        achRewards.put("ach_die_hard_unlocked", 100);
        achRewards.put("ach_star_collector_unlocked", 60);
        achRewards.put("ach_double_shield_unlocked", 120);
        achRewards.put("ach_parry_master_unlocked", 50);
        achRewards.put("ach_boss_parry_unlocked", 100);
        achRewards.put("ach_bsod_unlocked", 100);
        achRewards.put("ach_speedrun_unlocked", 80);
        achRewards.put("ach_big_data_unlocked", 40);
        achRewards.put("ach_net_warrior_unlocked", 5000);
        achRewards.put("ach_imperial_glory_unlocked", 50000);
        achRewards.put("ach_untouchable_unlocked", 2000);
        achRewards.put("ach_data_devourer_unlocked", 10000);
        achRewards.put("ach_system_down_unlocked", 5000);
        achRewards.put("ach_time_master_unlocked", 15000);

        for (Map.Entry<String, Integer> entry : achRewards.entrySet()) {
            String key = entry.getKey();
            String grantKey = "reward_v12_" + key;
            if (prefs.getBoolean(key, false) && !prefs.getBoolean(grantKey, false)) {
                blocksToAdd += entry.getValue();
                editor.putBoolean(grantKey, true);
            }
        }

        int currentRank = prefs.getInt("player_rank", 0);
        int lastRewardedRank = prefs.getInt("last_rewarded_rank", 0);
        int[] rankPrizes = {0, 100, 250, 500, 1000, 2000, 5000, 10000, 25000};
        if (currentRank > lastRewardedRank) {
            for (int i = lastRewardedRank + 1; i <= currentRank && i < rankPrizes.length; i++) {
                blocksToAdd += rankPrizes[i];
            }
            editor.putInt("last_rewarded_rank", currentRank);
        }

        if (blocksToAdd > 0) {
            int total = prefs.getInt("total_blocks", 0) + blocksToAdd;
            editor.putInt("total_blocks", total);
        }
        editor.apply();
    }
}
