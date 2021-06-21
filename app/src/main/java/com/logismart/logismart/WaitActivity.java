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

public class WaitActivity extends AppCompatActivity {

    private static final String TAG = "WaitActivity";

    private WaitActivity.BackPressCloseHandler backPressCloseHandler;

    private final String SharedPrefFile = "com.logismart.android.SharedPreferences";
    private SharedPreferences mPreferences;


    private String USER_ID;

    private Http http;

    private String ble;
    private String manager;
    private String phone;
    private String ship;
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

        mPreferences = getSharedPreferences(SharedPrefFile, MODE_PRIVATE);
        USER_ID = String.valueOf(mPreferences.getInt("id", 0));

        http = new Http();

        backPressCloseHandler = new WaitActivity.BackPressCloseHandler(WaitActivity.this);

        checkConfirm();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    private synchronized void checkConfirm() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = http.Http(ServerURL.CARRIER_ACCEPT_URL, USER_ID);
                    getreceiveMsg(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void getreceiveMsg(String receiveMsg) throws JSONException {
        Log.d(TAG, "getreceiveMsg: " + receiveMsg);
        JSONObject jsonObject = new JSONObject(receiveMsg);

        if (!receiveMsg.isEmpty() && jsonObject.getString("result").equals("success")) { // success

            Log.d(TAG, "getreceiveMsg: Insert Success");

            ble = jsonObject.getString("ble");
            manager = jsonObject.getString("manager");
            phone = jsonObject.getString("phone");
            ship = jsonObject.getString("ship");
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
        intent.putExtra("ship", ship);
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
