package com.dbzbanten.adpinger;

import android.app.Activity;
import android.content.*;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private EditText githubUrl;
    private Spinner interval;
    private TextView status, logView, currentUrl, deviceMode;
    private WebView webView;
    private final String[] intervals = {"1 menit (auto pilih)","5 menit (auto pilih)","15 menit","30 menit","60 menit"};
    private BroadcastReceiver urlReceiver;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        githubUrl = findViewById(R.id.githubUrl);
        interval = findViewById(R.id.interval);
        status = findViewById(R.id.status);
        logView = findViewById(R.id.logView);
        currentUrl = findViewById(R.id.currentUrl);
        deviceMode = findViewById(R.id.deviceMode);
        deviceMode.setText(RootUtils.modeLabel());
        webView = findViewById(R.id.webView);

        interval.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, intervals));
        int savedMin = getSharedPreferences("adpinger",0).getInt("interval",5);
        interval.setSelection(savedMin == 5 ? 1 : savedMin == 15 ? 2 : savedMin == 30 ? 3 : savedMin == 60 ? 4 : 0);

        githubUrl.setText(RemoteConfigStore.getUrl(this));
        // Default remote URL list is hosted on GitHub; users can still change it.
        githubUrl.setHint("Raw GitHub URL daftar URL");
        setupWebView();

        findViewById(R.id.saveRefresh).setOnClickListener(v -> refreshRemote());
        findViewById(R.id.start).setOnClickListener(v -> startPinger());
        findViewById(R.id.stop).setOnClickListener(v -> stopPinger());
        findViewById(R.id.showLog).setOnClickListener(v -> loadLog());
        findViewById(R.id.randomUrl).setOnClickListener(v -> showRandomUrl());

        urlReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                String u = intent.getStringExtra(PingerService.EXTRA_URL);
                if (u != null && !u.isEmpty()) {
                    getSharedPreferences("adpinger",0).edit().remove("pending_url").apply();
                    currentUrl.setText("AUTO RANDOM: " + u);
                    status.setText("URL dipilih otomatis. Membuka di WebView...");
                    loadAdUrl(u);
                }
            }
        };
        registerReceiver(urlReceiver, new IntentFilter(PingerService.ACTION_SHOW_URL),
                Context.RECEIVER_NOT_EXPORTED);
        loadPendingUrl(getIntent());
        loadLog();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadPendingUrl(intent);
    }

    private void loadPendingUrl(Intent intent) {
        String u = intent == null ? null : intent.getStringExtra(PingerService.EXTRA_URL);
        if (u == null || u.isEmpty()) {
            u = getSharedPreferences("adpinger",0).getString("pending_url", "");
        }
        if (u != null && !u.isEmpty()) {
            getSharedPreferences("adpinger",0).edit().remove("pending_url").apply();
            currentUrl.setText("AUTO RANDOM: " + u);
            status.setText("URL otomatis. Membuka di WebView...");
            loadAdUrl(u);
        }
    }

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        CookieManager.getInstance().setAcceptCookie(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                status.setText("Memuat: " + url);
            }
            @Override public void onPageFinished(WebView view, String url) {
                status.setText("WebView aktif: HTTP/halaman selesai dimuat");
                loadLog();
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
    }

    private void loadAdUrl(String u) {
        runOnUiThread(() -> {
            currentUrl.setText(u);
            webView.loadUrl(u);
        });
    }

    private void refreshRemote() {
        final String u = githubUrl.getText().toString().trim();
        if (u.isEmpty()) { status.setText("Masukkan URL Raw GitHub"); return; }
        RemoteConfigStore.setUrl(this, u);
        status.setText("Mengambil konfigurasi...");
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int n = RemoteUrlConfig.fetch(u).size();
                runOnUiThread(() -> status.setText("Konfigurasi OK: " + n + " URL"));
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Gagal: " + e.getMessage()));
            }
        });
    }

    private void startPinger() {
        int[] mins = {1,5,15,30,60};
        int selected = mins[interval.getSelectedItemPosition()];
        getSharedPreferences("adpinger",0).edit().putInt("interval", selected).apply();
        Intent i = new Intent(this, PingerService.class).setAction("START");
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        status.setText("Auto WebView START — setiap " + selected + " menit");
    }

    private void stopPinger() {
        Intent i = new Intent(this, PingerService.class).setAction("STOP");
        startService(i);
        status.setText("Auto WebView STOP");
    }

    private void showRandomUrl() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<String> urls = RemoteUrlConfig.fetch(RemoteConfigStore.getUrl(this));
                if (urls.isEmpty()) throw new IOException("Daftar URL kosong");
                String u = urls.get(new Random().nextInt(urls.size()));
                runOnUiThread(() -> loadAdUrl(u));
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Random URL gagal: " + e.getMessage()));
            }
        });
    }

    private void loadLog() {
        try {
            File f = new File(getFilesDir(), "adpinger.log");
            if (!f.exists()) { logView.setText("Belum ada log."); return; }
            FileInputStream in = new FileInputStream(f);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            in.close();
            logView.setText(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch(Exception e) { logView.setText("Log error: " + e.getMessage()); }
    }

    @Override protected void onDestroy() {
        if (urlReceiver != null) unregisterReceiver(urlReceiver);
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
