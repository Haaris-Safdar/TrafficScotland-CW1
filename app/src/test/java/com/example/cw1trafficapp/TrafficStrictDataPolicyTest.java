package com.example.cw1trafficapp;

import org.junit.Test;

import com.example.cw1trafficapp.cache.TrafficCacheManager;
import com.example.cw1trafficapp.model.TrafficSituation;
import com.example.cw1trafficapp.repository.TrafficRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TrafficStrictDataPolicyTest {

    @Test
    public void testStrictDataResultStatusLabels() {
        List<TrafficSituation> incidents = new ArrayList<>();
        incidents.add(new TrafficSituation());

        List<TrafficSituation> roadworks = new ArrayList<>();
        roadworks.add(new TrafficSituation());

        // Case 1: Both feeds Live Fresh
        TrafficRepository.TrafficResult liveResult = new TrafficRepository.TrafficResult(
                incidents, roadworks,
                TrafficRepository.FeedStatus.LIVE_FRESH, TrafficRepository.FeedStatus.LIVE_FRESH,
                System.currentTimeMillis(), System.currentTimeMillis()
        );
        assertTrue(liveResult.hasData());
        assertTrue(liveResult.isOverallLive());
        assertEquals("Live Traffic Scotland Feed", liveResult.getOverallStatusLabel());

        // Case 2: Both feeds Cached Offline
        TrafficRepository.TrafficResult cachedResult = new TrafficRepository.TrafficResult(
                incidents, roadworks,
                TrafficRepository.FeedStatus.CACHED_OFFLINE, TrafficRepository.FeedStatus.CACHED_OFFLINE,
                System.currentTimeMillis(), System.currentTimeMillis()
        );
        assertTrue(cachedResult.hasData());
        assertFalse(cachedResult.isOverallLive());
        assertTrue(cachedResult.isOverallOffline());
        assertEquals("Offline — Showing cached traffic data", cachedResult.getOverallStatusLabel());

        // Case 3: Mixed feeds (Incidents Live, Roadworks Cached)
        TrafficRepository.TrafficResult mixedResult = new TrafficRepository.TrafficResult(
                incidents, roadworks,
                TrafficRepository.FeedStatus.LIVE_FRESH, TrafficRepository.FeedStatus.CACHED_OFFLINE,
                System.currentTimeMillis(), System.currentTimeMillis()
        );
        assertTrue(mixedResult.hasData());
        assertEquals("Incidents: Live | Roadworks: Cached Offline", mixedResult.getOverallStatusLabel());

        // Case 4: First launch with no internet & no cache (UNAVAILABLE)
        TrafficRepository.TrafficResult emptyResult = new TrafficRepository.TrafficResult(
                new ArrayList<>(), new ArrayList<>(),
                TrafficRepository.FeedStatus.UNAVAILABLE, TrafficRepository.FeedStatus.UNAVAILABLE,
                0L, 0L
        );
        assertFalse(emptyResult.hasData());
        assertEquals("Traffic information unavailable", emptyResult.getOverallStatusLabel());
    }

    @Test
    public void testTimeFormatting() {
        String formatted = TrafficCacheManager.getFormattedTime(0L);
        assertEquals("No previous update", formatted);

        long testMs = 1763217300000L; // Fixed timestamp
        String timeStr = TrafficCacheManager.getFormattedTime(testMs);
        assertNotNull(timeStr);
        assertFalse(timeStr.isEmpty());
    }
}
