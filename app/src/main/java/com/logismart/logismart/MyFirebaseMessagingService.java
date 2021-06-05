package com.logismart.logismart;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;

public class MyFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    private final String SharedPrefFile = "com.logismart.android.SharedPreferences";
    private SharedPreferences mPreferences;
    String USER_NAME = null;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            if (/* Check if data needs to be processed by long running job */ true) {
                scheduleJob();
            } else {
                handleNow();
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getData() != null) {

            Log.d(TAG, "onMessageReceived: " + remoteMessage.getData().get("title"));
            Log.d(TAG, "onMessageReceived: " + remoteMessage.getData().get("body"));

            sendNotification(remoteMessage.getData().get("title"), remoteMessage.getData().get("body"));
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        mPreferences = getSharedPreferences(SharedPrefFile, MODE_PRIVATE);
        USER_NAME = mPreferences.getString("admin_name", null);
        sendRegistrationToServer(token);
    }

    /**
     * Schedule async work using WorkManager.
     */
    private void scheduleJob() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWorker.class)
                .build();
        WorkManager.getInstance().beginWith(work).enqueue();
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    /**
     * Persist token to third-party servers.
     * <p>
     * Modify this method to associate the user's FCM registration token with any
     * server-side account maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        Http http = new Http();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = http.Http(ServerURL.ADMIN_TOKEN_URL, USER_NAME, token);
                    if (result.contains("success")) {
                        Log.d(TAG, "run: Http success");
                    }
                    else {
                        Log.d(TAG, "run: Http fail");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(TAG, "run: Http exception");
                }
            }
        }).start();
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received. "경고! [??] 물품의 온도가 한계 온도에 근접했습니다."
     */
    private void sendNotification(String messageTitle, String messageBody) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = "fcm_default_channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setColor(ContextCompat.getColor(this, R.color.noti))
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(messageBody.substring(0, 11), 0 /* ID of notification */, notificationBuilder.build());
    }
}