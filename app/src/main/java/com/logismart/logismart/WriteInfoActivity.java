package com.logismart.logismart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class WriteInfoActivity extends AppCompatActivity {

    private static final String TAG = "WriteInfoActivity";

    private SharedPreferences mPreferences;
    private final String SharedPrefFile = "com.logismart.android.SharedPreferences";

    private Http http;

    private TextView backBtn;
    private EditText nameText;
    private EditText birthText;
    private Button completeBtn;

    private String name;
    private String birth;
    private String phone;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_writeinfo);

        mPreferences = getSharedPreferences(SharedPrefFile, MODE_PRIVATE);

        http = new Http();

        phone = mPreferences.getString("phone", "null");

        backBtn = findViewById(R.id.back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        nameText = findViewById(R.id.name_input);
        birthText = findViewById(R.id.birth_input);
        completeBtn = findViewById(R.id.complete_btn);
        completeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = nameText.getText().toString();
                birth = birthText.getText().toString();
                if (name.isEmpty() || birth.isEmpty()) {
                    Toast.makeText(WriteInfoActivity.this, "성명, 생년월일을 써주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                savetoSQL();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
        if (mPreferences.getInt("id", 0) != 0) {
            Log.d(TAG, "onStart: mAuth exist");
            moveActivity();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    private synchronized void savetoSQL() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    String result = http.Http(ServerURL.CARRIER_INFO_URL, name, birth, phone);
                    getreceiveMsg(result);
                    
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void getreceiveMsg(String receiveMsg) throws JSONException {
        JSONObject jsonObject = new JSONObject(receiveMsg);

        if (!receiveMsg.isEmpty() && jsonObject.getString("result").equals("success")) { // success

            int id = jsonObject.getInt("id");

            Log.d(TAG, "getreceiveMsg: Insert Success. ID - " + id);

            savetoSharedPrefId(id);
            moveActivity();
        }
        else {
            Log.d(TAG, "getreceiveMsg: Insert Fail");
        }
    }

    private void savetoSharedPrefId(int id) {
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();

        // data
        preferencesEditor.putInt("id", id);

        preferencesEditor.apply();
    }

    private void moveActivity() {
        Intent intent = new Intent(WriteInfoActivity.this, WaitActivity.class);
        startActivity(intent);
    }
}
