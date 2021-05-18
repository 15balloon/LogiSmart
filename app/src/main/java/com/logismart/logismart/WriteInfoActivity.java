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

    private static final String TAG = "AuthActivity";

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
//        phone = "01066666666";

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
                Toast.makeText(WriteInfoActivity.this, "전송", Toast.LENGTH_SHORT).show();
                savetoSQL();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    private void savetoSQL() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://logismart.cafe24.com/CarrierDAO.jsp");
                    HttpURLConnection connect = (HttpURLConnection) url.openConnection();
//                    connect.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36 Edg/90.0.818.62");
                    connect.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connect.setRequestMethod("GET");
                    connect.setDoInput(true);
                    connect.setDoOutput(true);
                    connect.setDefaultUseCaches(false);
                    connect.setUseCaches(false);
                    connect.connect();

                    // 응답 헤더의 정보를 모두 출력
//                    for (Map.Entry<String, List<String>> header : connect.getHeaderFields().entrySet()) {
//                        for (String value : header.getValue()) {
//                            Log.d(TAG, header.getKey() + " : " + value);
//                        }
//                    }

                    String output = "name=" + name + "&birth=" + birth + "&phone=" + phone;

                    StringBuffer outputbf = new StringBuffer();
                    outputbf.append("name").append("=").append(name).append("&");
                    outputbf.append("birth").append("=").append(birth).append("&");
                    outputbf.append("phone").append("=").append(phone);

                    OutputStreamWriter osw = new OutputStreamWriter(connect.getOutputStream(), "EUC-KR");

                    osw.write(output);
                    Log.d(TAG, "run: sendMsg - " + output);
//                    osw.write(outputbf.toString());
//                    Log.d(TAG, "run: sendMsg - " + outputbf.toString());

                    osw.flush();

                    int responseCode = connect.getResponseCode();
                    Log.d(TAG, "run: responseCode - " + responseCode);
                    Log.d(TAG, "run: responseURL - " + connect.getURL());

                    if (responseCode == connect.HTTP_OK) {

                        Log.d(TAG, "run: HTTP_OK");

                        InputStreamReader tmp = new InputStreamReader(connect.getInputStream(), "EUC-KR");

                        BufferedReader reader = new BufferedReader(tmp);

                        StringBuilder builder = new StringBuilder();

                        String str;
                        while ((str = reader.readLine()) != null) {
                            builder.append(str + "\n");
                        }
//                        reader.close();
//                        tmp.close();

                        String receiveMsg = builder.toString();

                        Log.d(TAG, "run: " + receiveMsg);
                        connect.disconnect();
                        return;

                    } else {
                        Log.d(TAG, "run: HTTP_FAIL");
                        connect.disconnect();
                        return;
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

//    Volley
//    private void savetoSQL() {
//        // app -> SQL
//        String name = nameText.getText().toString();
//        String birth = birthText.getText().toString();
//        String phone = mPreferences.getString("phone", "null");
//
//        Response.Listener<String> responseListener = new Response.Listener<String>() {
//            @Override
//            public void onResponse(String response) {
//                try {
//                    Log.d(TAG, "onResponse: " + response);
//                    if (response.trim().length() != 0) {
//                        JSONObject jsonResponse = new JSONObject(response);
//                        int id = jsonResponse.getInt("id");
//                        if (id > 0) {
//                            Toast.makeText(WriteInfoActivity.this, "전송 성공", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                    Toast.makeText(WriteInfoActivity.this, "전송", Toast.LENGTH_SHORT).show();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        };
//
////        Toast.makeText(WriteInfoActivity.this, "전송 성공", Toast.LENGTH_SHORT).show();
//
//        try {
//            RegisterRequest registerRequest = new RegisterRequest(name, birth, phone, responseListener);
//            RequestQueue queue = Volley.newRequestQueue(WriteInfoActivity.this);
//            queue.add(registerRequest);
//            Log.d(TAG, "savetoSQL: queue add success");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    private void savetoSharedPrefName(String name) {
//        SharedPreferences.Editor preferencesEditor = mPreferences.edit();
//
//        // data
//        preferencesEditor.putString("name", name);
//
//        preferencesEditor.apply();
//    }

    public void updateUI(int check) {
//        if (check == 3) { // complete write personal info
//            nameText.setInputType(InputType.TYPE_NULL);
//            birthText.setInputType(InputType.TYPE_NULL);
//            completeBtn.setClickable(false);
////            pb.setVisibility(View.VISIBLE);
////            customProgressDialog.show();
//        }
//
//        else if (check == 4) { // fail to response personal info
//            nameText.setInputType(InputType.TYPE_CLASS_TEXT);
//            birthText.setInputType(InputType.TYPE_CLASS_PHONE);
//            completeBtn.setClickable(true);
////            pb.setVisibility(View.INVISIBLE);
////            customProgressDialog.dismiss();
//        }
    }

    private void moveActivity() {
        Intent intent = new Intent(WriteInfoActivity.this, WaitActivity.class);
        // From SQL
        intent.putExtra("ble", "LogiSmart"); // ble name
//        intent.putExtra("name", ); // user name
//        intent.putExtra("from", ); // starting point
//        intent.putExtra("to", ); // destination
        startActivity(intent);
    }
}
