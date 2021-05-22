package com.logismart.logismart;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class WaitActivity extends AppCompatActivity {

    private static final String TAG = "WaitActivity";

    private WaitActivity.BackPressCloseHandler backPressCloseHandler;

    private final String SharedPrefFile = "com.logismart.android.SharedPreferences";
    private SharedPreferences mPreferences = getSharedPreferences(SharedPrefFile, MODE_PRIVATE);


    private final String USER_ID = String.valueOf(mPreferences.getInt("id", 0));

    private Http http;

    private String ble;
    private String manager;
    private String phone;
    private String from;
    private String to;
    private int upper;
    private int lower;

    public void onBackPressed() {
        this.backPressCloseHandler.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wait);

        http = new Http();

        backPressCloseHandler = new WaitActivity.BackPressCloseHandler(WaitActivity.this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            String result = http.Http(ServerURL.CARRIER_ACCEPT_URL, USER_ID);
            getreceiveMsg(result);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    private void getreceiveMsg(String receiveMsg) throws JSONException {
        JSONObject jsonObject = new JSONObject(receiveMsg);

        if (!receiveMsg.isEmpty() && jsonObject.getString("result").equals("success")) { // success

            Log.d(TAG, "getreceiveMsg: Insert Success");

            ble = jsonObject.getString("ble");
            manager = jsonObject.getString("manager");
            phone = jsonObject.getString("phone");
            from = jsonObject.getString("from");
            to = jsonObject.getString("to");
            upper = jsonObject.getInt("upper");
            lower = jsonObject.getInt("lower");

            moveActivity();
        }
        else {
            Log.d(TAG, "getreceiveMsg: Insert Fail");
        }
    }

    public void moveActivity() {
        Intent intent = new Intent(WaitActivity.this, MainDriverActivity.class);
        intent.putExtra("ble", ble);
        intent.putExtra("manager", manager);
        intent.putExtra("manager_phone", phone);
        intent.putExtra("from", from);
        intent.putExtra("to", to);
        intent.putExtra("upper", upper);
        intent.putExtra("lower", lower);
        startActivity(intent);
    }

    public class BackPressCloseHandler {
        private Activity activity;
        private long backKeyPressedTime;
        private Toast toast;

        public BackPressCloseHandler(Activity context) {
            this.backKeyPressedTime = 0;
            this.activity = context;
        }

        public void onBackPressed() {
            this.toast = Toast.makeText(this.activity, "뒤로가기 버튼을 한 번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT);

            if (System.currentTimeMillis() > this.backKeyPressedTime + 2000) {
                this.backKeyPressedTime = System.currentTimeMillis();
                this.toast.show();
            } else if (System.currentTimeMillis() <= this.backKeyPressedTime + 2000) {
                this.activity.finishAffinity();
                this.toast.cancel();
                System.runFinalization();
                System.exit(0);
            }
        }
    }
}
