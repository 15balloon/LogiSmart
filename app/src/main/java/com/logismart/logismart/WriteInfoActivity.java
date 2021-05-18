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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class WriteInfoActivity extends AppCompatActivity {

    private static final String TAG = "WriteInfoActivity";

    private SharedPreferences mPreferences;
    final private String SharedPrefFile = "com.logismart.android.SharedPreferences";

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
        if (!mPreferences.getString("name", "nothing").equals("nothing")) {
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
                    URL url = new URL("http://logismart.cafe24.com:80/CarrierDAO.jsp");
                    HttpURLConnection connect = (HttpURLConnection) url.openConnection();
                    connect.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connect.setRequestMethod("POST");
                    connect.setDoInput(true);
                    connect.setDoOutput(true);
                    connect.setDefaultUseCaches(false);
                    connect.setUseCaches(false);
                    connect.connect();

                    String output = "name=" + name + "&birth=" + birth + "&phone=" + phone;

                    OutputStreamWriter osw = new OutputStreamWriter(connect.getOutputStream(), "UTF-8");

                    osw.write(output);
                    Log.d(TAG, "run: sendMsg - " + output);

                    osw.flush();
                    osw.close();

                    int responseCode = connect.getResponseCode();
                    Log.d(TAG, "run: responseCode - " + responseCode);
                    Log.d(TAG, "run: responseURL - " + connect.getURL());

                    if (responseCode == connect.HTTP_OK) {

                        Log.d(TAG, "run: HTTP_OK");

                        InputStreamReader tmp = new InputStreamReader(connect.getInputStream(), "UTF-8");

                        BufferedReader reader = new BufferedReader(tmp);

                        StringBuilder builder = new StringBuilder();

                        String str;
                        while ((str = reader.readLine()) != null) {
                            builder.append(str + "\n");
                        }
                        reader.close();
                        tmp.close();

                        String receiveMsg = builder.toString();
                        Log.d(TAG, "run: " + receiveMsg);

                        connect.disconnect();
                        
                        if (receiveMsg.contains("name")) {
                            Log.d(TAG, "run: Insert Success");
                        }
                        else {
                            Log.d(TAG, "run: Insert Fail");
                        }

                        getreceiveMsg(receiveMsg);

                    } else {
                        Log.d(TAG, "run: HTTP_FAIL");
                        connect.disconnect();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void getreceiveMsg(String receiveMsg) {
        if (!receiveMsg.isEmpty() && receiveMsg.contains("name")) { // success
            savetoSharedPrefName(name);
            moveActivity();
        }
    }

    private void savetoSharedPrefName(String name) {
        SharedPreferences.Editor preferencesEditor = mPreferences.edit();

        // data
        preferencesEditor.putString("name", name);

        preferencesEditor.apply();
    }

    private void moveActivity() {
        Intent intent = new Intent(WriteInfoActivity.this, WaitActivity.class);
        startActivity(intent);
    }
}
