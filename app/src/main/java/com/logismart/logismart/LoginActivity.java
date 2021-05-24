package com.logismart.logismart;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private final String SharedPrefFile = "com.logismart.android.SharedPreferences";
    private SharedPreferences mPreferences;

    private Button backBtn;
    private EditText idText;
    private EditText pwText;
    private Button loginBtn;

    private ProgressDialog pd;

    private Http http;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mPreferences = getSharedPreferences(SharedPrefFile, MODE_PRIVATE);


        backBtn = findViewById(R.id.login_back_btn);
        idText = (EditText) findViewById(R.id.id_input);
        pwText = (EditText) findViewById(R.id.pw_input);
        loginBtn = findViewById(R.id.login_btn);

        pd = ProgressDialog.show(LoginActivity.this, "", "로딩중");
        pd.dismiss();

        http = new Http();

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pd.show();
                String id = idText.getText().toString().replace(" ", "");
                String pw = pwText.getText().toString().replace(" ", "");

                if (id.isEmpty() || pw.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "아이디, 패스워드를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                    return;
                }
                httpSQL(id, pw);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mPreferences.getString("admin_id", "null").equals("null")) {
            Log.d(TAG, "onStart: Admin Auto-Login");
            moveActivity();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    private synchronized void httpSQL(String id, String pw) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onClick: " + id + pw);
                try {
                    String result = http.Http(ServerURL.ADMIN_LOGIN_URL, id, pw);
                    JSONObject jsonObject = null;
                    jsonObject = new JSONObject(result);
                    if (jsonObject.getString("result").equals("success")) {
                        savetoSharedPrefId(id);
                        moveActivity();
                    }
                    else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.dismiss();
                                Toast.makeText(LoginActivity.this, "아이디, 패스워드가 틀립니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void savetoSharedPrefId(String id) {
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();

        // data
        preferencesEditor.putString("admin_id", id);

        preferencesEditor.apply();
    }

    private void moveActivity() {
        Intent intent = new Intent(LoginActivity.this, MainAdminActivity.class);
        intent.putExtra("id", mPreferences.getString("admin_id", "null"));
        startActivity(intent);
    }
}
