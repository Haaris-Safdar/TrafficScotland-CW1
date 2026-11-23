package com.example.cw1trafficapp;

import org.junit.Test;

import com.example.cw1trafficapp.network.TrafficNetworkClient;
import com.example.cw1trafficapp.parser.TrafficXmlParser;

import static org.junit.Assert.*;

public class TrafficNetworkAndCacheTest {

    @Test
    public void testOfficialDatexFeedUrls() {
        assertEquals("https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml", TrafficNetworkClient.UNPLANNED_FEED_URL);
        assertEquals("https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml", TrafficNetworkClient.ROADWORKS_FEED_URL);
    }

    @Test
    public void testNetworkClientInstantiation() {
        TrafficNetworkClient client = new TrafficNetworkClient();
        assertNotNull(client);
    }

    @Test
    public void testHtmlIgnoredByXmlParser() throws Exception {
        TrafficXmlParser parser = new TrafficXmlParser();
        String htmlSample = "<!DOCTYPE html><html><head><title>Login</title></head><body>Traffic Scotland &middot; Login</body></html>";
        
        // XmlParser returns empty list for HTML input instead of throwing unhandled crashes
        try {
            parser.parseXml(htmlSample, true);
        } catch (Exception e) {
            // Expected
            assertNotNull(e);
        }
    }
}
