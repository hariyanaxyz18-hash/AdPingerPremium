package com.dbzbanten.adpinger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Optional Magisk/root integration. The app remains functional without root. */
public final class RootUtils {
    private RootUtils() {}

    /**
     * Checks real root access, not just the presence of a su binary.
     * On Magisk this may display the normal Superuser approval dialog the first time.
     */
    public static boolean requestRootAccess() {
        Process p = null;
        try {
            p = new ProcessBuilder("su", "-c", "id")
                    .redirectErrorStream(true)
                    .start();
            String out = read(p.getInputStream());
            int code = p.waitFor();
            return code == 0 && out.contains("uid=0");
        } catch (Exception ignored) {
            return false;
        } finally {
            if (p != null) p.destroy();
        }
    }

    public static boolean isRootAvailable() {
        return requestRootAccess();
    }

    /** Runs a fixed, app-owned root command. Never pass remote URL text as a shell command. */
    public static boolean runRootCommand(String... command) {
        if (command == null || command.length == 0) return false;
        Process p = null;
        try {
            StringBuilder cmd = new StringBuilder();
            for (String part : command) {
                if (part == null) return false;
                if (cmd.length() > 0) cmd.append(' ');
                cmd.append(shellQuote(part));
            }
            p = new ProcessBuilder("su", "-c", cmd.toString())
                    .redirectErrorStream(true)
                    .start();
            p.getInputStream().close();
            return p.waitFor() == 0;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (p != null) p.destroy();
        }
    }

    public static String modeLabel() {
        return isRootAvailable()
                ? "ROOT DEVICE • MAGISK ROOT ACCESS OK"
                : "NON-ROOT DEVICE • STANDARD MODE";
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[256];
        int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
        return new String(out.toByteArray(), "UTF-8");
    }
}
