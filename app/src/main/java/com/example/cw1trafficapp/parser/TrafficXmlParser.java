package com.example.cw1trafficapp.parser;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.example.cw1trafficapp.model.TrafficCoordinates;
import com.example.cw1trafficapp.model.TrafficLocation;
import com.example.cw1trafficapp.model.TrafficSituation;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class TrafficXmlParser {

    private static final String TAG = "TRAFFIC_API";

    public List<TrafficSituation> parseXml(
            String xmlData,
            boolean isUnplanned)
            throws XmlPullParserException, IOException {

        List<TrafficSituation> situations = new ArrayList<>();

        if (xmlData == null || xmlData.trim().isEmpty()) {
            Log.d(TAG, "XmlParser received empty or null XML input.");
            return situations;
        }

        Log.d(TAG,
                "XmlParser execution started. XML length: "
                        + xmlData.length());

        XmlPullParser parser;

        try {
            XmlPullParserFactory factory =
                    XmlPullParserFactory.newInstance();

            factory.setNamespaceAware(true);
            parser = factory.newPullParser();

        } catch (Throwable e) {

            try {
                parser = (XmlPullParser) Class.forName(
                        "org.kxml2.io.KXmlParser"
                ).getDeclaredConstructor().newInstance();

                parser.setFeature(
                        XmlPullParser.FEATURE_PROCESS_NAMESPACES,
                        true
                );

            } catch (Exception ex) {
                throw new XmlPullParserException(
                        "Could not instantiate XmlPullParser: "
                                + ex.getMessage()
                );
            }
        }

        parser.setInput(new StringReader(xmlData));

        TrafficSituation currentSituation = null;
        TrafficLocation currentLocation = null;

        String currentTag = "";
        String lastDescriptorType = "";

        boolean inGeneralPublicComment = false;
        boolean inToLocation = false;
        boolean inFromLocation = false;
        boolean inPointCoordinates = false;
        boolean inJunctionNameDescriptor = false;
        boolean inIlcDescriptor = false;
        boolean inOtherNameDescriptor = false;

        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {

            String tagName = parser.getName();

            switch (eventType) {

                case XmlPullParser.START_TAG:

                    currentTag = tagName != null ? tagName : "";

                    if ("situation".equalsIgnoreCase(tagName)) {

                        currentSituation = new TrafficSituation();
                        currentSituation.setUnplanned(isUnplanned);

                        String id = parser.getAttributeValue(
                                null, "id"
                        );

                        if (id != null) {
                            currentSituation.setId(id);
                        }

                    } else if ("situationRecord".equalsIgnoreCase(
                            tagName)
                            && currentSituation != null) {

                        String recordId = parser.getAttributeValue(
                                null, "id"
                        );

                        if (recordId != null) {
                            currentSituation.setSituationRecordId(
                                    recordId
                            );
                        }

                        String version = parser.getAttributeValue(
                                null, "version"
                        );

                        if (version != null) {
                            currentSituation.setVersion(version);
                        }

                        for (int i = 0;
                             i < parser.getAttributeCount();
                             i++) {

                            if ("type".equalsIgnoreCase(
                                    parser.getAttributeName(i))) {

                                currentSituation.setSituationRecordType(
                                        parser.getAttributeValue(i)
                                );

                                break;
                            }
                        }

                    } else if ("generalPublicComment"
                            .equalsIgnoreCase(tagName)) {

                        inGeneralPublicComment = true;

                    } else if ("to".equalsIgnoreCase(tagName)) {

                        inToLocation = true;
                        currentLocation = new TrafficLocation();
                        lastDescriptorType = "";

                    } else if ("from".equalsIgnoreCase(tagName)) {

                        inFromLocation = true;
                        currentLocation = new TrafficLocation();
                        lastDescriptorType = "";

                    } else if ("pointCoordinates"
                            .equalsIgnoreCase(tagName)) {

                        inPointCoordinates = true;

                    } else if ("name".equalsIgnoreCase(tagName)
                            && (inToLocation || inFromLocation)) {

                        inJunctionNameDescriptor = true;

                    } else if ("ilc".equalsIgnoreCase(tagName)
                            && (inToLocation || inFromLocation)) {

                        inIlcDescriptor = true;

                    } else if ("otherName".equalsIgnoreCase(tagName)
                            && (inToLocation || inFromLocation)) {

                        inOtherNameDescriptor = true;
                    }

                    break;

                case XmlPullParser.TEXT:

                    String text = parser.getText();

                    if (text != null) {
                        text = text.trim();
                    }

                    if (currentSituation != null
                            && text != null
                            && !text.isEmpty()) {

                        if (inGeneralPublicComment
                                && "value".equalsIgnoreCase(
                                currentTag)) {

                            if (currentSituation
                                    .getPublicComment().isEmpty()) {

                                currentSituation.setPublicComment(
                                        text
                                );
                            }

                        } else if ("overallStartTime"
                                .equalsIgnoreCase(currentTag)) {

                            currentSituation.setOverallStartTime(text);

                        } else if ("overallEndTime"
                                .equalsIgnoreCase(currentTag)) {

                            currentSituation.setOverallEndTime(text);

                        } else if ("validityStatus"
                                .equalsIgnoreCase(currentTag)) {

                            currentSituation.setValidityStatus(text);

                        } else if ("delaysType"
                                .equalsIgnoreCase(currentTag)) {

                            currentSituation.setDelaysType(text);

                        } else if ("numberOfOperationalLanes"
                                .equalsIgnoreCase(currentTag)) {

                            try {
                                currentSituation
                                        .setNumberOfOperationalLanes(
                                                Integer.parseInt(text)
                                        );
                            } catch (NumberFormatException ignored) {
                            }

                        } else if ("numberOfLanesRestricted"
                                .equalsIgnoreCase(currentTag)) {

                            try {
                                currentSituation
                                        .setNumberOfLanesRestricted(
                                                Integer.parseInt(text)
                                        );
                            } catch (NumberFormatException ignored) {
                            }

                        } else if ("tpegDirection"
                                .equalsIgnoreCase(currentTag)) {

                            currentSituation.setDirection(text);

                        } else if ("tpegOtherPointDescriptorType"
                                .equalsIgnoreCase(currentTag)) {

                            lastDescriptorType = text;

                        } else if (
                                "roadOrCarriagewayOrLaneManagementType"
                                        .equalsIgnoreCase(currentTag)
                                        || "roadMaintenanceType"
                                        .equalsIgnoreCase(currentTag)
                                        || "abnormalTrafficType"
                                        .equalsIgnoreCase(currentTag)) {

                            currentSituation.setSubtype(text);

                        } else if (inPointCoordinates
                                && currentLocation != null) {

                            TrafficCoordinates coords =
                                    currentLocation.getCoordinates();

                            if ("latitude".equalsIgnoreCase(
                                    currentTag)) {

                                try {
                                    coords.setLatitude(
                                            Double.parseDouble(text)
                                    );
                                } catch (NumberFormatException ignored) {
                                }

                            } else if ("longitude"
                                    .equalsIgnoreCase(currentTag)) {

                                try {
                                    coords.setLongitude(
                                            Double.parseDouble(text)
                                    );
                                } catch (NumberFormatException ignored) {
                                }
                            }

                        } else if (currentLocation != null
                                && "value".equalsIgnoreCase(
                                currentTag)) {

                            if (inIlcDescriptor) {

                                currentLocation
                                        .setPrimaryRoadName(text);

                                currentLocation
                                        .setIlcDescriptor(text);

                            } else if (inJunctionNameDescriptor) {

                                if ("linkName".equalsIgnoreCase(
                                        lastDescriptorType)
                                        || currentLocation
                                        .getPrimaryRoadName()
                                        .isEmpty()) {

                                    currentLocation
                                            .setPrimaryRoadName(text);

                                } else {
                                    currentLocation
                                            .setSecondaryLocationName(
                                                    text
                                            );
                                }

                                currentLocation.setJunctionName(text);

                            } else if (inOtherNameDescriptor) {

                                currentLocation.setOtherName(text);
                            }
                        }
                    }

                    break;

                case XmlPullParser.END_TAG:

                    if ("situation".equalsIgnoreCase(tagName)) {

                        if (currentSituation != null) {
                            situations.add(currentSituation);
                            currentSituation = null;
                        }

                    } else if ("generalPublicComment"
                            .equalsIgnoreCase(tagName)) {

                        inGeneralPublicComment = false;

                    } else if ("pointCoordinates"
                            .equalsIgnoreCase(tagName)) {

                        inPointCoordinates = false;

                    } else if ("name".equalsIgnoreCase(tagName)) {

                        inJunctionNameDescriptor = false;
                        lastDescriptorType = "";

                    } else if ("ilc".equalsIgnoreCase(tagName)) {

                        inIlcDescriptor = false;

                    } else if ("otherName"
                            .equalsIgnoreCase(tagName)) {

                        inOtherNameDescriptor = false;

                    } else if ("to".equalsIgnoreCase(tagName)) {

                        if (currentSituation != null
                                && currentLocation != null) {

                            currentSituation.setToLocation(
                                    currentLocation
                            );
                        }

                        inToLocation = false;
                        currentLocation = null;

                    } else if ("from".equalsIgnoreCase(tagName)) {

                        if (currentSituation != null
                                && currentLocation != null) {

                            currentSituation.setFromLocation(
                                    currentLocation
                            );
                        }

                        inFromLocation = false;
                        currentLocation = null;
                    }

                    currentTag = "";
                    break;
            }

            eventType = parser.next();
        }

        Log.d(TAG,
                "XmlParser finished. Produced "
                        + situations.size()
                        + " TrafficSituation objects.");

        return situations;
    }
}