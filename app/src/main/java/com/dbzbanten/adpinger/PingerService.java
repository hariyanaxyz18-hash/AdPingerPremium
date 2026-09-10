package com.dbzbanten.adpinger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PingerService extends Service {

    public static final String ACTION_SHOW_URL =
            "com.dbzbanten.adpinger.SHOW_URL";

    private static final String CHANNEL = "adpinger";
    private static final int NOTIFICATION_ID = 1001;

    private File logFile;
    private volatile boolean stopping = false;

    @Override
    public void onCreate() {
        super.onCreate();

        logFile = new File(getFilesDir(), "adpinger.log");

        createNotificationChannel();

        // Penting untuk Android modern:
        // AndroidManifest harus memiliki foregroundServiceType="dataSync"
        startForeground(
                NOTIFICATION_ID,
                createNotification("Service aktif")
        );

        log("SERVICE CREATED");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL,
                            "AdPinger",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription("Status service AdPingerMulti");

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String text) {

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("AdPingerMulti")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    private void updateNotification(String text) {

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        if (manager != null) {
            manager.notify(
                    NOTIFICATION_ID,
                    createNotification(text)
            );
        }
    }

    private void log(String message) {

        try {

            if (logFile == null) {
                logFile = new File(
                        getFilesDir(),
                        "adpinger.log"
                );
            }

            FileWriter writer =
                    new FileWriter(logFile, true);

            String time =
                    new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.US
                    ).format(new Date());

            writer.write(time + " " + message + "\n");
            writer.close();

        } catch (Exception ignored) {
        }
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        String action =
                intent != null
                        ? intent.getAction()
                        : null;

        if ("STOP".equals(action)) {

            stopping = true;

            log("SERVICE STOP");

            stopForeground(true);
            stopSelf();

            return START_NOT_STICKY;
        }

        stopping = false;

        log("SERVICE START");

        updateNotification("Service aktif — siap");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        stopping = true;

        log("SERVICE DESTROY");

        try {
            stopForeground(true);
        } catch (Exception ignored) {
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
            }
