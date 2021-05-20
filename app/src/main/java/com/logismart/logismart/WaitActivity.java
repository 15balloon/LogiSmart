package com.logismart.logismart;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WaitActivity extends AppCompatActivity {

    private WaitActivity.BackPressCloseHandler backPressCloseHandler;

    public void onBackPressed() {
        this.backPressCloseHandler.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wait);

        backPressCloseHandler = new WaitActivity.BackPressCloseHandler(WaitActivity.this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // TODO : admin confirm check
//        if (confirm) {
//            moveActivity();
//        }
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
                // TODO : app <-> DB
            }
        });
    }

    public void moveActivity() {
        Intent intent = new Intent(WaitActivity.this, MainDriverActivity.class);
        intent.putExtra("ble", "LogiSmart");
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
