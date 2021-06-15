package com.logismart.logismart;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class Http {

    private static final String TAG = "Http";
    URL url;
    String receiveMsg;

    public String Http(String url, String ... strings){
        HttpURLConnection connect = null;
        OutputStreamWriter osw = null;
        InputStreamReader tmp = null;
        try {
            this.url = new URL(url);
            connect = (HttpURLConnection) this.url.openConnection();
            connect.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connect.setRequestMethod("POST");
            connect.setDoInput(true);
            connect.setDoOutput(true);
//        connect.setDefaultUseCaches(false);
//        connect.setUseCaches(false);
            connect.setConnectTimeout(1000);
            connect.setReadTimeout(1000);
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

            osw = new OutputStreamWriter(connect.getOutputStream(), "UTF-8");

            osw.write(output);
            Log.d(TAG, "run: sendMsg - " + output);

            osw.flush();
            osw.close();

            int responseCode = connect.getResponseCode();
            Log.d(TAG, "run: responseCode - " + responseCode);
            Log.d(TAG, "run: responseURL - " + connect.getURL());

            if (responseCode == connect.HTTP_OK) {

                Log.d(TAG, "run: HTTP_OK");

                tmp = new InputStreamReader(connect.getInputStream(), "UTF-8");

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

            } else {
                Log.d(TAG, "run: HTTP_FAIL");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connect.disconnect();
        }

        return receiveMsg;
    }
}
