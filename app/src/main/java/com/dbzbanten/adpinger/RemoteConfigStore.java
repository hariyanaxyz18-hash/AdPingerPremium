package com.dbzbanten.adpinger;

import android.content.Context;
import android.content.SharedPreferences;

public final class RemoteConfigStore {
    private static final String PREF = "adpinger_config";
    private static final String KEY_URL = "github_raw_url";
    private static final String DEFAULT_URL = "https://raw.githubusercontent.com/hariyanaxyz18-hash/AdPingerPremium/main/urls.txt";

    private RemoteConfigStore() {}

    public static void setUrl(Context context, String url) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(KEY_URL, url == null ? "" : url.trim()).apply();
    }

    public static String getUrl(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_URL, DEFAULT_URL);
    }
}
