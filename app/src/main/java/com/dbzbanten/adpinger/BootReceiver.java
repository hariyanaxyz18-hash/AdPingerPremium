package com.dbzbanten.adpinger;

import android.content.*;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) return;
        if (!c.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE).getBoolean(MainActivity.ENABLED,false)) return;
        Intent s=new Intent(c,PingerService.class);
        try { if(Build.VERSION.SDK_INT>=26)c.startForegroundService(s); else c.startService(s); } catch(Exception ignored) {}
    }
}
