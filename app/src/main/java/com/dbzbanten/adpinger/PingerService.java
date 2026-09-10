package com.dbzbanten.adpinger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PingerService extends Service {
    public static final String ACTION_SHOW_URL = "com.dbzbanten.adpinger.SHOW_URL";
    public static final String ACTION_STOP = "com.dbzbanten.adpinger.STOP";
    public static final String EXTRA_URL = "url";
    private static final String CHANNEL_ID = "adpinger";
    private static final int NOTIFICATION_ID = 1001;
    private static final String PREF = "adpinger";
    private static final String PENDING_URL = "pending_url";
    private final Random random = new Random();
    private File logFile;
    private ScheduledExecutorService scheduler;
    private volatile boolean running;

    @Override public void onCreate() {
        super.onCreate();
        logFile = new File(getFilesDir(), "adpinger.log");
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification("Service aktif"));
        running = true;
        writeLog("SERVICE CREATED");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "AdPingerMulti", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Status service AdPingerMulti");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String text) {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder.setContentTitle("AdPingerMulti")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(NOTIFICATION_ID, createNotification(text));
    }

    private void writeLog(String message) {
        try {
            if (logFile == null) logFile = new File(getFilesDir(), "adpinger.log");
            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(time + " " + message + "\n");
            }
        } catch (Exception ignored) {}
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if ("STOP".equals(action)) {
            running = false;
            stopScheduler();
            getSharedPreferences(PREF, MODE_PRIVATE)
                    .edit()
                    .putBoolean("enabled", false)
                    .remove(PENDING_URL)
                    .apply();

            Intent stopBroadcast = new Intent(ACTION_STOP)
                    .setPackage(getPackageName());
            sendBroadcast(stopBroadcast);

            writeLog("SERVICE STOP");
            updateNotification("Service dihentikan");
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        int minutes = getSharedPreferences(PREF, MODE_PRIVATE).getInt("interval", 5);
        if (minutes < 1) minutes = 1;
        running = true;
        getSharedPreferences(PREF, MODE_PRIVATE).edit().putBoolean("enabled", true).apply();
        startScheduler(minutes);
        writeLog("SERVICE START interval=" + minutes + "m");
        updateNotification("Auto WebView — setiap " + minutes + " menit");
        return START_STICKY;
    }

    private synchronized void startScheduler(int minutes) {
        stopScheduler();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // Open one URL immediately, then repeat at the selected interval.
        scheduler.execute(this::pickAndOpenUrl);
        scheduler.scheduleAtFixedRate(this::pickAndOpenUrl, minutes, minutes, TimeUnit.MINUTES);
    }

    private synchronized void stopScheduler() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void pickAndOpenUrl() {
        if (!running) return;
        try {
            String raw = RemoteConfigStore.getUrl(this);
            List<String> urls = RemoteUrlConfig.fetch(raw);
            if (urls.isEmpty()) throw new IllegalStateException("Daftar URL kosong");
            String url = urls.get(random.nextInt(urls.size()));
            getSharedPreferences(PREF, MODE_PRIVATE).edit().putString(PENDING_URL, url).apply();
            writeLog("AUTO URL=" + url);
            updateNotification("Membuka URL otomatis");
            showUrl(url);
        } catch (Exception e) {
            writeLog("AUTO ERROR=" + e.getMessage());
            updateNotification("Gagal memilih URL: " + e.getMessage());
        }
    }

    private void showUrl(String url) {
        // Works while MainActivity is alive/foreground.
        Intent broadcast = new Intent(ACTION_SHOW_URL).setPackage(getPackageName());
        broadcast.putExtra(EXTRA_URL, url);
        sendBroadcast(broadcast);

        // Android 10+ blocks ordinary background activity launches. On a rooted
        // device, Magisk can grant this app permission to run the fixed `am start`
        // command so the WebView Activity can be brought forward automatically.
        if (RootUtils.requestRootAccess()) {
            boolean started = RootUtils.runRootCommand(
                    "am", "start", "-n", getPackageName() + "/.MainActivity");
            writeLog(started ? "ROOT ACTIVITY STARTED" : "ROOT ACTIVITY START FAILED");
        } else {
            writeLog("NO ROOT: broadcast sent; Activity must be open for direct WebView update");
        }
    }

    @Override public void onDestroy() {
        running = false;
        stopScheduler();
        writeLog("SERVICE DESTROY");
        try { stopForeground(true); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
