package com.logismart.logismart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class InitActivity extends AppCompatActivity {

    Button admin_btn;
    Button driver_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        admin_btn = findViewById(R.id.admin);
        driver_btn = findViewById(R.id.driver);

        admin_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InitActivity.this, MainActivity.class);
                intent.putExtra("type", "admin");
                startActivity(intent);
            }
        });

        driver_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InitActivity.this, MainActivity.class);
                intent.putExtra("type", "driver");
                intent.putExtra("ble", "BLE's name~~~~~"); // 블루투스 이름
                startActivity(intent);
            }
        });
    }

}
