package com.example.cw1trafficapp.cache;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Manager class handling local offline caching of Traffic Scotland XML data feeds.
 * Supports independent caching and timestamping for Unplanned Events and Current Roadworks.
 */
public class TrafficCacheManager {

    private static final String TAG = "TRAFFIC_API";

    private static final String PREF_NAME = "traffic_scotland_cache";
    private static final String KEY_UNPLANNED_XML = "key_unplanned_xml";
    private static final String KEY_UNPLANNED_TIME = "key_unplanned_time";
    private static final String KEY_ROADWORKS_XML = "key_roadworks_xml";
    private static final String KEY_ROADWORKS_TIME = "key_roadworks_time";

    public static void saveUnplannedCache(Context context, String xmlData) {
        if (context == null || xmlData == null || xmlData.trim().isEmpty()) {
            Log.e(TAG, "Cache write failed for Unplanned Events: Context or XML is null/empty");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_UNPLANNED_XML, xmlData.trim())
                .putLong(KEY_UNPLANNED_TIME, System.currentTimeMillis())
                .apply();
        Log.d(TAG, "Cache write success for Unplanned Events. Character count: " + xmlData.length());
    }

    public static void saveRoadworksCache(Context context, String xmlData) {
        if (context == null || xmlData == null || xmlData.trim().isEmpty()) {
            Log.e(TAG, "Cache write failed for Current Roadworks: Context or XML is null/empty");
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_ROADWORKS_XML, xmlData.trim())
                .putLong(KEY_ROADWORKS_TIME, System.currentTimeMillis())
                .apply();
        Log.d(TAG, "Cache write success for Current Roadworks. Character count: " + xmlData.length());
    }

    public static String getCachedUnplannedXml(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String xml = prefs.getString(KEY_UNPLANNED_XML, null);
        if (xml != null && !xml.trim().isEmpty()) {
            Log.d(TAG, "Cache read success for Unplanned Events. Character count: " + xml.length());
        } else {
            Log.d(TAG, "Cache read for Unplanned Events: No cache present");
        }
        return xml;
    }

    public static String getCachedRoadworksXml(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String xml = prefs.getString(KEY_ROADWORKS_XML, null);
        if (xml != null && !xml.trim().isEmpty()) {
            Log.d(TAG, "Cache read success for Current Roadworks. Character count: " + xml.length());
        } else {
            Log.d(TAG, "Cache read for Current Roadworks: No cache present");
        }
        return xml;
    }

    public static long getUnplannedLastSuccessTimeMs(Context context) {
        if (context == null) return 0L;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_UNPLANNED_TIME, 0L);
    }

    public static long getRoadworksLastSuccessTimeMs(Context context) {
        if (context == null) return 0L;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_ROADWORKS_TIME, 0L);
    }

    public static boolean hasUnplannedCache(Context context) {
        String xml = getCachedUnplannedXml(context);
        return xml != null && !xml.trim().isEmpty();
    }

    public static boolean hasRoadworksCache(Context context) {
        String xml = getCachedRoadworksXml(context);
        return xml != null && !xml.trim().isEmpty();
    }

    public static boolean hasAnyCache(Context context) {
        return hasUnplannedCache(context) || hasRoadworksCache(context);
    }

    public static String getFormattedTime(long timeMs) {
        if (timeMs <= 0L) {
            return "No previous update";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.UK);
        return sdf.format(new Date(timeMs));
    }
}
