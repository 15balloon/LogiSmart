package com.logismart.logismart;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private Button backBtn;
    private EditText idText;
    private EditText pwText;
    private Button loginBtn;

    private ProgressDialog pd;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        backBtn = findViewById(R.id.login_back_btn);
        idText = (EditText) findViewById(R.id.id_input);
        pwText = (EditText) findViewById(R.id.pw_input);
        loginBtn = findViewById(R.id.login_btn);

        pd = ProgressDialog.show(LoginActivity.this, "", "로딩중");

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
                // TODO : server -> app
                moveActivity();
            }
        });
    }

    private void moveActivity() {
        Intent intent = new Intent(LoginActivity.this, MainAdminActivity.class);
//        intent.putExtra("name", ); // admin name
        startActivity(intent);
    }
}
