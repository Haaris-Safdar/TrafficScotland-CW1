package com.example.cw1trafficapp.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.cw1trafficapp.cache.TrafficCacheManager;
import com.example.cw1trafficapp.model.TrafficSituation;
import com.example.cw1trafficapp.network.TrafficNetworkClient;
import com.example.cw1trafficapp.parser.TrafficXmlParser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

public class TrafficRepository {

    private static final String TAG = "TRAFFIC_API";

    public enum FeedStatus {
        LIVE_FRESH,
        CACHED_OFFLINE,
        UNAVAILABLE
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String errorMessage);
    }

    public static class TrafficResult {

        private final List<TrafficSituation> incidents;
        private final List<TrafficSituation> roadworks;

        private final FeedStatus unplannedStatus;
        private final FeedStatus roadworksStatus;

        private final long unplannedTimeMs;
        private final long roadworksTimeMs;

        public TrafficResult(
                List<TrafficSituation> incidents,
                List<TrafficSituation> roadworks,
                FeedStatus unplannedStatus,
                FeedStatus roadworksStatus,
                long unplannedTimeMs,
                long roadworksTimeMs) {

            this.incidents = incidents != null
                    ? incidents : new ArrayList<>();

            this.roadworks = roadworks != null
                    ? roadworks : new ArrayList<>();

            this.unplannedStatus = unplannedStatus;
            this.roadworksStatus = roadworksStatus;
            this.unplannedTimeMs = unplannedTimeMs;
            this.roadworksTimeMs = roadworksTimeMs;
        }

        public List<TrafficSituation> getIncidents() {
            return incidents;
        }

        public List<TrafficSituation> getRoadworks() {
            return roadworks;
        }

        public FeedStatus getUnplannedStatus() {
            return unplannedStatus;
        }

        public FeedStatus getRoadworksStatus() {
            return roadworksStatus;
        }

        public boolean hasData() {
            return !incidents.isEmpty() || !roadworks.isEmpty();
        }

        public boolean isOverallLive() {
            return unplannedStatus == FeedStatus.LIVE_FRESH
                    && roadworksStatus == FeedStatus.LIVE_FRESH;
        }

        public boolean isOverallOffline() {
            return unplannedStatus == FeedStatus.CACHED_OFFLINE
                    || roadworksStatus == FeedStatus.CACHED_OFFLINE;
        }

        public String getOverallStatusLabel() {

            if (isOverallLive()) {
                return "Live Traffic Scotland Feed";
            }

            if (unplannedStatus == FeedStatus.LIVE_FRESH
                    && roadworksStatus == FeedStatus.CACHED_OFFLINE) {
                return "Incidents: Live | Roadworks: Cached Offline";
            }

            if (unplannedStatus == FeedStatus.CACHED_OFFLINE
                    && roadworksStatus == FeedStatus.LIVE_FRESH) {
                return "Incidents: Cached Offline | Roadworks: Live";
            }

            if (unplannedStatus == FeedStatus.CACHED_OFFLINE
                    && roadworksStatus == FeedStatus.CACHED_OFFLINE) {
                return "Offline — Showing cached traffic data";
            }

            if (unplannedStatus == FeedStatus.LIVE_FRESH) {
                return "Incidents: Live | Roadworks: Unavailable";
            }

            if (roadworksStatus == FeedStatus.LIVE_FRESH) {
                return "Incidents: Unavailable | Roadworks: Live";
            }

            if (unplannedStatus == FeedStatus.CACHED_OFFLINE) {
                return "Incidents: Cached | Roadworks: Unavailable";
            }

            if (roadworksStatus == FeedStatus.CACHED_OFFLINE) {
                return "Incidents: Unavailable | Roadworks: Cached";
            }

            return "Traffic information unavailable";
        }

        public String getFormattedLastUpdatedTime() {
            long newestTime = Math.max(
                    unplannedTimeMs,
                    roadworksTimeMs
            );

            return TrafficCacheManager.getFormattedTime(newestTime);
        }
    }

    private final ExecutorService executorService;

    private ScheduledExecutorService scheduledExecutorService;

    private final Handler mainThreadHandler;
    private final TrafficXmlParser xmlParser;
    private final TrafficNetworkClient networkClient;

    private final Object fetchLock = new Object();

    private final List<DataCallback<TrafficResult>>
            waitingCallbacks = new ArrayList<>();

    private boolean fetching = false;

    public TrafficRepository() {
        executorService = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());
        xmlParser = new TrafficXmlParser();
        networkClient = new TrafficNetworkClient();
    }

    public void loadTrafficData(
            Context context,
            DataCallback<TrafficResult> callback) {

        synchronized (fetchLock) {
            waitingCallbacks.add(callback);

            if (fetching) {
                Log.d(TAG,
                        "Additional refresh joined the current network request.");
                return;
            }

            fetching = true;
        }

        Log.d(TAG,
                "=== TrafficRepository loadTrafficData execution started ===");

        try {
            executorService.execute(() -> {

                TrafficResult result = null;
                String failure = null;

                try {
                    List<TrafficSituation> incidents = new ArrayList<>();

                    FeedStatus unplannedStatus =
                            FeedStatus.UNAVAILABLE;

                    long unplannedTime = 0L;

                    // Unplanned Events feed
                    try {
                        Log.d(TAG,
                                "Fetching Unplanned Events feed from network...");

                        String xml = networkClient.fetchXmlData(
                                TrafficNetworkClient.UNPLANNED_FEED_URL,
                                "Unplanned Events"
                        );

                        List<TrafficSituation> parsed =
                                xmlParser.parseXml(xml, true);

                        if (parsed != null) {
                            incidents = parsed;
                            unplannedStatus = FeedStatus.LIVE_FRESH;

                            TrafficCacheManager.saveUnplannedCache(
                                    context, xml
                            );

                            unplannedTime = System.currentTimeMillis();

                            Log.d(TAG,
                                    "Unplanned Events live success: "
                                            + incidents.size());
                        }

                    } catch (Exception liveEx) {

                        Log.e(TAG,
                                "Unplanned Events live fetch failed. "
                                        + "Trying cache.",
                                liveEx);

                        if (TrafficCacheManager.hasUnplannedCache(context)) {

                            try {
                                String cachedXml =
                                        TrafficCacheManager
                                                .getCachedUnplannedXml(context);

                                List<TrafficSituation> parsedCache =
                                        xmlParser.parseXml(cachedXml, true);

                                if (parsedCache != null) {
                                    incidents = parsedCache;

                                    unplannedStatus =
                                            FeedStatus.CACHED_OFFLINE;

                                    unplannedTime =
                                            TrafficCacheManager
                                                    .getUnplannedLastSuccessTimeMs(context);
                                }

                            } catch (Exception cacheEx) {
                                Log.e(TAG,
                                        "Unplanned Events cache parsing error",
                                        cacheEx);
                            }
                        }
                    }

                    List<TrafficSituation> roadworks = new ArrayList<>();

                    FeedStatus roadworksStatus =
                            FeedStatus.UNAVAILABLE;

                    long roadworksTime = 0L;

                    // Current Roadworks feed
                    try {
                        Log.d(TAG,
                                "Fetching Current Roadworks feed from network...");

                        String xml = networkClient.fetchXmlData(
                                TrafficNetworkClient.ROADWORKS_FEED_URL,
                                "Current Roadworks"
                        );

                        List<TrafficSituation> parsed =
                                xmlParser.parseXml(xml, false);

                        if (parsed != null) {
                            roadworks = parsed;

                            roadworksStatus = FeedStatus.LIVE_FRESH;

                            TrafficCacheManager.saveRoadworksCache(
                                    context, xml
                            );

                            roadworksTime = System.currentTimeMillis();

                            Log.d(TAG,
                                    "Current Roadworks live success: "
                                            + roadworks.size());
                        }

                    } catch (Exception liveEx) {

                        Log.e(TAG,
                                "Current Roadworks live fetch failed. "
                                        + "Trying cache.",
                                liveEx);

                        if (TrafficCacheManager.hasRoadworksCache(context)) {

                            try {
                                String cachedXml =
                                        TrafficCacheManager
                                                .getCachedRoadworksXml(context);

                                List<TrafficSituation> parsedCache =
                                        xmlParser.parseXml(cachedXml, false);

                                if (parsedCache != null) {
                                    roadworks = parsedCache;

                                    roadworksStatus =
                                            FeedStatus.CACHED_OFFLINE;

                                    roadworksTime =
                                            TrafficCacheManager
                                                    .getRoadworksLastSuccessTimeMs(context);
                                }

                            } catch (Exception cacheEx) {
                                Log.e(TAG,
                                        "Roadworks cache parsing error",
                                        cacheEx);
                            }
                        }
                    }

                    result = new TrafficResult(
                            incidents,
                            roadworks,
                            unplannedStatus,
                            roadworksStatus,
                            unplannedTime,
                            roadworksTime
                    );

                    Log.d(TAG,
                            "Traffic result: "
                                    + incidents.size() + " incidents, "
                                    + roadworks.size() + " roadworks");

                } catch (Exception unexpected) {
                    failure =
                            "Unable to process traffic feeds: "
                                    + unexpected.getMessage();

                    Log.e(TAG, failure, unexpected);

                } finally {

                    final TrafficResult completed = result;
                    final String error = failure;

                    final List<DataCallback<TrafficResult>> listeners;

                    synchronized (fetchLock) {
                        listeners = new ArrayList<>(waitingCallbacks);
                        waitingCallbacks.clear();
                        fetching = false;
                    }

                    mainThreadHandler.post(() -> {
                        for (DataCallback<TrafficResult> listener : listeners) {

                            try {
                                if (error == null) {
                                    listener.onSuccess(completed);
                                } else {
                                    listener.onError(error);
                                }

                            } catch (RuntimeException callbackError) {
                                Log.e(TAG,
                                        "Traffic callback failed",
                                        callbackError);
                            }
                        }
                    });
                }
            });

        } catch (RejectedExecutionException rejected) {

            final List<DataCallback<TrafficResult>> listeners;

            synchronized (fetchLock) {
                listeners = new ArrayList<>(waitingCallbacks);
                waitingCallbacks.clear();
                fetching = false;
            }

            mainThreadHandler.post(() -> {
                for (DataCallback<TrafficResult> listener : listeners) {
                    listener.onError(
                            "Traffic refresh is unavailable: repository stopped."
                    );
                }
            });
        }
    }

    public void startHourlyAutoRefresh(
            Context context,
            DataCallback<TrafficResult> callback) {

        stopAutoRefresh();

        scheduledExecutorService =
                Executors.newSingleThreadScheduledExecutor();

        scheduledExecutorService.scheduleWithFixedDelay(
                () -> loadTrafficData(context, callback),
                1,
                1,
                TimeUnit.HOURS
        );

        Log.d(TAG, "Hourly auto-refresh scheduled.");
    }

    public void stopAutoRefresh() {
        if (scheduledExecutorService != null
                && !scheduledExecutorService.isShutdown()) {

            scheduledExecutorService.shutdownNow();
            scheduledExecutorService = null;

            Log.d(TAG, "Auto-refresh scheduler stopped.");
        }
    }

    public void shutdown() {
        stopAutoRefresh();
        executorService.shutdownNow();
    }
}