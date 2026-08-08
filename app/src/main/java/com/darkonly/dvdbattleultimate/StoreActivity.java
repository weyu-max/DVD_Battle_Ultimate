package com.darkonly.dvdbattleultimate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class StoreActivity extends AppCompatActivity {

    private TextView textTotalBlocks;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GameUtils.applyLanguage(this);
        setContentView(R.layout.activity_store);

        prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        textTotalBlocks = findViewById(R.id.text_store_blocks);
        updateBlocks();

        findViewById(R.id.btn_back_store).setOnClickListener(v -> finish());
        setupStoreItems();
    }

    private void updateBlocks() {
        int total = prefs.getInt("total_blocks", 0);
        textTotalBlocks.setText("BLOCKS: " + total);
    }

    private void setupStoreItems() {
        android.widget.LinearLayout container = findViewById(R.id.store_items_container);
        if (container == null) return;
        container.removeAllViews();

        addStoreHeader(container, "TEMAS DE ARENA (10.000 B)");
        addStoreItem(container, "TEMA MATRIX", "Fundo verde digital", 10000, "theme_matrix");
        addStoreItem(container, "TEMA CYBERPUNK", "Neon e vibrante", 10000, "theme_cyber");

        addStoreHeader(container, "MELHORIAS (BLOCKS)");
        addStoreItem(container, "ESCUDO EXTRA", "Inicia com escudo", 5000, "booster_shield");
        addStoreItem(container, "ÍMÃ TURBO", "Atração mais forte", 8000, "booster_magnet");
    }

    private void addStoreHeader(android.widget.LinearLayout parent, String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(android.graphics.Color.YELLOW);
        tv.setPadding(0, 20, 0, 10);
        parent.addView(tv);
    }

    private void addStoreItem(android.widget.LinearLayout parent, String name, String desc, int price, final String id) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 10, 0, 10);
        row.setBackgroundResource(R.drawable.edittext_retro);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        textLayout.setPadding(16, 0, 0, 0);

        TextView t1 = new TextView(this);
        t1.setText(name);
        t1.setTextColor(Color.WHITE);
        t1.setTypeface(null, android.graphics.Typeface.BOLD);
        
        TextView t2 = new TextView(this);
        t2.setText(desc);
        t2.setTextColor(Color.LTGRAY);
        t2.setTextSize(10);

        textLayout.addView(t1);
        textLayout.addView(t2);

        final android.widget.Button buyBtn = new android.widget.Button(this);
        boolean owned = prefs.getBoolean("owned_" + id, false);
        buyBtn.setText(owned ? "OK" : price + " B");
        buyBtn.setEnabled(!owned);
        buyBtn.setBackgroundResource(R.drawable.btn_retro_classic);
        buyBtn.setLayoutParams(new LinearLayout.LayoutParams(120, 80));

        buyBtn.setOnClickListener(v -> {
            int total = prefs.getInt("total_blocks", 0);
            if (total >= price) {
                prefs.edit().putInt("total_blocks", total - price)
                           .putBoolean("owned_" + id, true).apply();
                SaveManager.saveData(this);
                updateBlocks();
                buyBtn.setText("OK");
                buyBtn.setEnabled(false);
                Toast.makeText(this, "Adquirido!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Blocks insuficientes!", Toast.LENGTH_SHORT).show();
            }
        });

        row.addView(textLayout);
        row.addView(buyBtn);
        parent.addView(row);
    }
}
