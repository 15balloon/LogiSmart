package push;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.HttpServlet;

public class SendNoti extends HttpServlet {
	
    boolean SHOW_ON_IDLE = false;
    int LIVE_TIME = 300;
    int RETRY = 2;
    
    String simpleApiKey = Keys.FMS;
    String gcmURL = "https://android.googleapis.com/fcm/send";
    
    public void sendMsg(String title, String msg, String token) throws IOException {
    	
    	System.out.println("sendMsg");
    	
    	URL url = new URL("https://fcm.googleapis.com/fcm/send");
    	HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    	conn.setDoOutput(true);
    	conn.setRequestMethod("POST");
    	conn.setRequestProperty("Content-Type", "application/json");
    	conn.setRequestProperty("Authorization", "key=" + simpleApiKey);
    	
    	conn.setDoOutput(true);
    	conn.setDoInput(true);
    	
    	String input = "{\"data\" : {\"title\" : \"" + title + " \", \"body\" : \"" + msg + "\"}, \"to\":\"" + token + "\"}";
    	
    	OutputStream os = conn.getOutputStream();
    	os.write(input.getBytes("UTF-8"));
    	os.flush();
    	os.close();
    	
    	int responseCode = conn.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Post parameters : " + input);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        System.out.println(response.toString());
    }
}
