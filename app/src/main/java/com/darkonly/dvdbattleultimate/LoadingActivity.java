package com.darkonly.dvdbattleultimate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable nextActivityRunnable;

    @Override
    protected void onResume() {
        super.onResume();
        GameUtils.applyFullscreenIfEnabled(this);
        GameUtils.applyLanguage(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        
        View root = findViewById(R.id.loading_root);
        GameUtils.startStarAnimation(this, (ViewGroup) root, handler);
        GameUtils.startBackgroundFlicker(root, handler);

        nextActivityRunnable = () -> {
            startActivity(new Intent(LoadingActivity.this, MainActivity.class));
            finish();
        };
        handler.postDelayed(nextActivityRunnable, 4000); 
    }
}