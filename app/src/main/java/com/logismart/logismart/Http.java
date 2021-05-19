package com.logismart.logismart;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class Http {

    private static final String TAG = "Http";
    URL url;
    String receiveMsg;

    public String Http(String url, String ... strings) throws IOException {
        this.url = new URL(url);
        HttpURLConnection connect = (HttpURLConnection) this.url.openConnection();
        connect.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connect.setRequestMethod("POST");
        connect.setDoInput(true);
        connect.setDoOutput(true);
        connect.setDefaultUseCaches(false);
        connect.setUseCaches(false);
        connect.connect();

        String output = "";
        receiveMsg = "";

        if (strings.length > 0) {
            int i = 0;
            while (strings.length > i) {
                output = output + "strings" + (i + 1) + "=" + strings[i] + "&";
                i++;
            }
            output = output.substring(0, output.length() - 1);
        }

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

            String str = "";

            while ((str = reader.readLine()) != null) {
                builder.append(str + "\n");
            }
            reader.close();
            tmp.close();

            receiveMsg = builder.toString();
            Log.d(TAG, "run: " + receiveMsg);

            connect.disconnect();

        } else {
            Log.d(TAG, "run: HTTP_FAIL");
            connect.disconnect();
        }

        return receiveMsg;
    }
}
