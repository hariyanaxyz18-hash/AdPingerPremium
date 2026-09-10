package com.dbzbanten.adpinger;

import android.content.Context;
import java.io.*;

/** Root detection only. The app remains fully functional without root. */
public final class RootUtils {
    private RootUtils() {}

    public static boolean isRootAvailable() {
        String[] paths = {
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/adb/magisk", "/data/adb/ksu", "/data/adb/ap/bin/su"
        };
        for (String p : paths) if (new File(p).exists()) return true;
        try {
            Process p = new ProcessBuilder("sh", "-c", "command -v su").redirectErrorStream(true).start();
            String out = read(p.getInputStream()).trim();
            p.waitFor();
            return !out.isEmpty();
        } catch (Exception ignored) { return false; }
    }

    public static String modeLabel() {
        return isRootAvailable() ? "ROOT DEVICE • ROOT ACCESS DETECTED" : "NON-ROOT DEVICE • STANDARD MODE";
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[256]; int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
        return new String(out.toByteArray(), "UTF-8");
    }
}
