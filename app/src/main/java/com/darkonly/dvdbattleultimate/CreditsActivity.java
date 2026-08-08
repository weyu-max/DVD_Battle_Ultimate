package com.darkonly.dvdbattleultimate;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class CreditsActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        GameUtils.applyFullscreenIfEnabled(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);
        findViewById(R.id.btn_close_credits).setOnClickListener(v -> finish());
        findViewById(R.id.btn_legal_credits).setOnClickListener(v -> showLegalDialog());
    }

    private void showLegalDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.legal_title)
            .setMessage(R.string.legal_text)
            .setPositiveButton("OK", null)
            .show();
    }
}