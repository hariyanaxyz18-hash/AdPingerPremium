package com.dbzbanten.adpinger;

import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class PingerService extends Service {
    public static final String ACTION_SHOW_URL = "com.dbzbanten.adpinger.SHOW_URL";
    private Handler handler;
    private Runnable task;
    private static final String CHANNEL="adpinger";
    private File logFile;
    private volatile boolean stopping = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        logFile = new File(getFilesDir(),"adpinger.log");
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL,"AdPinger",NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
        startForeground(1001, notification("Auto random URL siap"));
    }

    private Notification notification(String text) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this,CHANNEL) : new Notification.Builder(this);
        return b.setContentTitle("AdPingerMulti")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(true).build();
    }

    private void log(String s) {
        try (FileWriter w = new FileWriter(logFile,true)) {
            w.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US).format(new Date())+" "+s+"\n");
        } catch(Exception ignored) {}
    }

    @Override public int onStartCommand(Intent in,int flags,int id) {
        if ("STOP".equals(in != null ? in.getAction() : null)) {
            stopping = true;
            if(task!=null) handler.removeCallbacks(task);
            executor.shutdownNow();
            stopForeground(true); stopSelf();
            return START_NOT_STICKY;
        }
        stopping = false;
        if(task==null) schedule();
        return START_STICKY;
    }

    private void schedule() {
        task = new Runnable() {
            @Override public void run() {
                if (stopping) return;
                executor.execute(PingerService.this::selectRandomUrl);
                int mins=getSharedPreferences("adpinger",0).getInt("interval",5);
                handler.postDelayed(this, mins * 60_000L);
            }
        };
        // first selection happens immediately after START
        handler.post(task);
    }

    private void selectRandomUrl() {
        try {
            List<String> urls = RemoteUrlConfig.fetch(RemoteConfigStore.getUrl(this));
            if (urls.isEmpty()) throw new IOException("Daftar URL kosong");
            String u = urls.get(new Random().nextInt(urls.size()));
            log("AUTO RANDOM SELECT: " + u);

            // Kirim URL ke Activity. Jika Activity sedang terbuka, MainActivity
            // akan langsung memuat halaman tersebut ke WebView. Android
            // membatasi WebView yang berjalan di background; service tidak
            // mencoba memaksa Activity tampil di depan.
            Intent i = new Intent(ACTION_SHOW_URL);
            i.setPackage(getPackageName());
            i.putExtra("url", u);
            sendBroadcast(i);

            updateNotification("URL random terpilih — buka aplikasi untuk melihat");
        } catch(Exception e) {
            log("AUTO RANDOM ERROR: " + e.getMessage());
            updateNotification("Gagal memilih URL: " + e.getMessage());
        }
    }

    private void updateNotification(String text) {
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1001, notification(text));
    }

    @Override public void onDestroy() {
        if (handler != null && task != null) handler.removeCallbacks(task);
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i){return null;}
}
