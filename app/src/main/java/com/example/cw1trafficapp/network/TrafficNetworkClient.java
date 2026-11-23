package com.example.cw1trafficapp.network;

import android.util.Base64;
import android.util.Log;

import com.example.cw1trafficapp.BuildConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Android HttpURLConnection network client for downloading official Traffic Scotland DATEX II feeds.
 * Validates credentials, rejects HTML/redirect responses, and includes safe TRAFFIC_API logging.
 */
public class TrafficNetworkClient {

    private static final String TAG = "TRAFFIC_API";

    public static final String UNPLANNED_FEED_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    public static final String ROADWORKS_FEED_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    private final String clientId;
    private final String clientKey;

    public TrafficNetworkClient() {
        this.clientId = BuildConfig.TRAFFIC_SCOTLAND_CLIENT_ID;
        this.clientKey = BuildConfig.TRAFFIC_SCOTLAND_CLIENT_KEY;
    }

    public boolean hasValidCredentials() {
        return clientId != null && !clientId.trim().isEmpty() 
                && !clientId.contains("DEFAULT") 
                && clientKey != null && !clientKey.trim().isEmpty() 
                && !clientKey.contains("DEFAULT");
    }

    public String fetchXmlData(String urlString, String feedName) throws IOException {
        Log.d(TAG, "Request started for publication: " + feedName + " | URL: " + urlString);
        boolean validCreds = hasValidCredentials();
        Log.d(TAG, "Credentials configured for " + feedName + ": " + validCreds);

        if (!validCreds) {
            String credsErr = "Traffic Scotland API credentials not configured for " + feedName + ". Aborting unauthenticated request.";
            Log.e(TAG, credsErr);
            throw new IOException(credsErr);
        }

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(18000);
            connection.setInstanceFollowRedirects(false); // Do not silently follow redirects to login HTML pages
            connection.setRequestProperty("User-Agent", "TrafficScotlandAndroidApp/1.0");
            connection.setRequestProperty("Accept", "application/xml, text/xml, */*");

            String id = clientId.trim();
            String key = clientKey.trim();

            connection.setRequestProperty("Client-ID", id);
            connection.setRequestProperty("Client-Key", key);
            connection.setRequestProperty("x-api-key", key);
            connection.setRequestProperty("ApiKey", key);

            // Official DATEX II developer guide format: Authorization = Base64(clientId + ":" + clientKey)
            String credentials = id + ":" + key;
            String rawBase64 = Base64.encodeToString(credentials.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            connection.setRequestProperty("Authorization", rawBase64);

            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();

            Log.d(TAG, feedName + " HTTP response code: " + responseCode);
            Log.d(TAG, feedName + " Response Content-Type: " + (contentType != null ? contentType : "unknown"));

            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM 
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP 
                    || responseCode == 307 
                    || responseCode == 308) {
                String location = connection.getHeaderField("Location");
                String redirectErr = feedName + " Redirected to: " + (location != null ? location : "login page") + " (HTTP " + responseCode + "). Authorization failed.";
                Log.e(TAG, redirectErr);
                throw new IOException(redirectErr);
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorMsg = feedName + " HTTP Error " + responseCode + ": " + connection.getResponseMessage();
                Log.e(TAG, errorMsg);
                throw new IOException(errorMsg);
            }

            if (contentType != null && contentType.toLowerCase().contains("text/html")) {
                String htmlErr = feedName + " Invalid response: Server returned HTML page instead of DATEX II XML (Content-Type: " + contentType + ")";
                Log.e(TAG, htmlErr);
                throw new IOException(htmlErr);
            }

            try (InputStream is = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                String xml = sb.toString();
                String trimmedXml = xml.trim();

                // Validate that response is not an HTML document
                if (trimmedXml.startsWith("<!DOCTYPE html") || trimmedXml.startsWith("<html") || trimmedXml.contains("</html>")) {
                    String htmlDocErr = feedName + " Invalid response content: Received HTML document instead of DATEX II XML.";
                    Log.e(TAG, htmlDocErr);
                    throw new IOException(htmlDocErr);
                }

                // Confirm expected DATEX II XML publication elements
                if (!trimmedXml.contains("<d2LogicalModel") && !trimmedXml.contains("<payloadPublication") && !trimmedXml.contains("<situation")) {
                    String xmlFormatErr = feedName + " Invalid XML content: Missing DATEX II publication elements.";
                    Log.e(TAG, xmlFormatErr);
                    throw new IOException(xmlFormatErr);
                }

                Log.d(TAG, feedName + " Valid XML response received successfully. Character count: " + xml.length());
                return xml;
            }
        } catch (IOException e) {
            Log.e(TAG, feedName + " Request Exception: " + e.getClass().getName() + " - " + e.getMessage());
            throw e;
        } finally {
            connection.disconnect();
        }
    }
}
