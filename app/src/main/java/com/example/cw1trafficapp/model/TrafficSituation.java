package com.example.cw1trafficapp.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class TrafficSituation implements Serializable {

    public enum IconCategory {
        ROAD_CLOSED,
        LANE_RESTRICTION,
        QUEUING_TRAFFIC,
        ABNORMAL_TRAFFIC,
        MAINTENANCE_ROADWORKS,
        OTHER_INCIDENT
    }

    private String id = "";
    private String version = "";
    private String situationRecordId = "";
    private String situationRecordType = "";
    private String subtype = "";
    private String publicComment = "";
    private String overallStartTime = "";
    private String overallEndTime = "";
    private String validityStatus = "";
    private String delaysType = "";

    private int numberOfOperationalLanes = -1;
    private int numberOfLanesRestricted = -1;

    private String direction = "";

    private TrafficLocation fromLocation =
            new TrafficLocation();

    private TrafficLocation toLocation =
            new TrafficLocation();

    private boolean isUnplanned = false;

    public static final double GLASGOW_LAT = 55.8642;
    public static final double GLASGOW_LNG = -4.2518;

    public TrafficSituation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id.trim() : "";
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version != null ? version.trim() : "";
    }

    public String getSituationRecordId() {
        return situationRecordId;
    }

    public void setSituationRecordId(String value) {
        situationRecordId = value != null
                ? value.trim() : "";
    }

    public String getSituationRecordType() {
        return situationRecordType;
    }

    public void setSituationRecordType(String value) {
        situationRecordType = value != null
                ? value.trim() : "";
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String value) {
        subtype = value != null ? value.trim() : "";
    }

    public String getPublicComment() {
        return publicComment;
    }

    public void setPublicComment(String value) {
        publicComment = value != null
                ? value.trim() : "";
    }

    public String getOverallStartTime() {
        return overallStartTime;
    }

    public void setOverallStartTime(String value) {
        overallStartTime = value != null
                ? value.trim() : "";
    }

    public String getOverallEndTime() {
        return overallEndTime;
    }

    public void setOverallEndTime(String value) {
        overallEndTime = value != null
                ? value.trim() : "";
    }

    public String getValidityStatus() {
        return validityStatus;
    }

    public void setValidityStatus(String value) {
        validityStatus = value != null
                ? value.trim() : "";
    }

    public String getDelaysType() {
        return delaysType;
    }

    public void setDelaysType(String value) {
        delaysType = value != null
                ? value.trim() : "";
    }

    public int getNumberOfOperationalLanes() {
        return numberOfOperationalLanes;
    }

    public void setNumberOfOperationalLanes(int value) {
        numberOfOperationalLanes = value;
    }

    public int getNumberOfLanesRestricted() {
        return numberOfLanesRestricted;
    }

    public void setNumberOfLanesRestricted(int value) {
        numberOfLanesRestricted = value;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String value) {
        direction = value != null
                ? value.trim() : "";
    }

    public TrafficLocation getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(TrafficLocation value) {
        if (value != null) {
            fromLocation = value;
        }
    }

    public TrafficLocation getToLocation() {
        return toLocation;
    }

    public void setToLocation(TrafficLocation value) {
        if (value != null) {
            toLocation = value;
        }
    }

    public boolean isUnplanned() {
        return isUnplanned;
    }

    public void setUnplanned(boolean value) {
        isUnplanned = value;
    }

    public TrafficCoordinates getPrimaryCoordinates() {

        if (fromLocation != null
                && fromLocation.getCoordinates().isValid()) {

            return fromLocation.getCoordinates();
        }

        if (toLocation != null
                && toLocation.getCoordinates().isValid()) {

            return toLocation.getCoordinates();
        }

        return new TrafficCoordinates();
    }

    public double getDistanceFromReference(
            double refLat,
            double refLng) {

        double distFrom = Double.MAX_VALUE;
        double distTo = Double.MAX_VALUE;

        if (fromLocation != null
                && fromLocation.getCoordinates().isValid()) {

            distFrom = calculateHaversine(
                    refLat,
                    refLng,
                    fromLocation.getCoordinates().getLatitude(),
                    fromLocation.getCoordinates().getLongitude()
            );
        }

        if (toLocation != null
                && toLocation.getCoordinates().isValid()) {

            distTo = calculateHaversine(
                    refLat,
                    refLng,
                    toLocation.getCoordinates().getLatitude(),
                    toLocation.getCoordinates().getLongitude()
            );
        }

        if (distFrom == Double.MAX_VALUE
                && distTo == Double.MAX_VALUE) {
            return -1.0;
        }

        return Math.min(distFrom, distTo);
    }

    private double calculateHaversine(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        final int R = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(latDistance / 2)
                        * Math.sin(latDistance / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(
                Math.sqrt(a),
                Math.sqrt(1 - a)
        );

        return R * c;
    }

    public String getRoadName() {

        if (fromLocation != null
                && !fromLocation.getIlcDescriptor().isEmpty()) {

            return fromLocation.getIlcDescriptor();
        }

        if (toLocation != null
                && !toLocation.getIlcDescriptor().isEmpty()) {

            return toLocation.getIlcDescriptor();
        }

        if (!publicComment.isEmpty()) {

            String[] tokens = publicComment.split(
                    "[\\s,.-]+"
            );

            for (String token : tokens) {
                if (token.matches("^[AM]\\d+.*")) {
                    return token;
                }
            }
        }

        return "Unknown Road";
    }


    public IconCategory getIconCategory() {

        String type = situationRecordType.toLowerCase(Locale.UK);
        String sub = subtype.toLowerCase(Locale.UK);
        String comment = publicComment.toLowerCase(Locale.UK);

        // 1. Prioritise explicit DATEX II classifications.

        if (sub.contains("roadclosed")
                || sub.contains("carriagewayclosed")
                || sub.contains("alllanesclosed")
                || sub.contains("roadclosure")) {

            return IconCategory.ROAD_CLOSED;
        }

        if (sub.contains("laneclosed")
                || sub.contains("lanerestriction")
                || sub.contains("lanesrestricted")) {

            return IconCategory.LANE_RESTRICTION;
        }

        if (sub.contains("queuingtraffic")
                || sub.contains("slowtraffic")) {

            return IconCategory.QUEUING_TRAFFIC;
        }

        if (type.contains("abnormaltraffic")) {
            return IconCategory.ABNORMAL_TRAFFIC;
        }

        // 2. Recognise lane and hard shoulder restrictions.
        // These must be checked before general closures.

        boolean laneRestriction = comment.matches(
                "(?s).*\\b(?:lane(?:s)?(?:\\s+\\d+)?|hard\\s+shoulder)"
                        + "\\b.{0,30}\\b(?:closure|closures|closed|restriction|restricted)\\b.*"
        )
                || comment.contains("contraflow")
                || numberOfLanesRestricted > 0;

        if (laneRestriction) {
            return IconCategory.LANE_RESTRICTION;
        }

        // 3. Recognise closure wording in roadworks comments.

        boolean roadClosure =
                comment.contains("road closed")
                        || comment.contains("road closure")
                        || comment.contains("carriageway closed")
                        || comment.contains("carriageway closure")
                        || comment.contains("full closure")
                        || comment.contains("total closure")
                        || comment.matches(
                        "(?s).*\\b(?:on|off)?\\s*slip\\s+closure\\b.*"
                )
                        || comment.matches(
                        "(?s).*\\s[-–]\\s*(?:emergency\\s+)?closure\\b.*"
                );

        if (roadClosure) {
            return IconCategory.ROAD_CLOSED;
        }

        // 4. Ordinary maintenance and construction works.

        if (type.contains("maintenanceworks")
                || type.contains("constructionworks")
                || sub.contains("roadworks")
                || sub.contains("maintenance")) {

            return IconCategory.MAINTENANCE_ROADWORKS;
        }

        // 5. Queuing or congestion reported in descriptions.

        if (comment.contains("queuing")
                || comment.contains("congestion")) {

            return IconCategory.QUEUING_TRAFFIC;
        }

        // 6. Other unplanned situations.

        return IconCategory.OTHER_INCIDENT;
    }


    public String getFormattedStartTime() {
        return formatIsoDate(overallStartTime);
    }

    public String getFormattedEndTime() {
        if (overallEndTime.isEmpty()) {
            return "Ongoing / Unspecified";
        }

        return formatIsoDate(overallEndTime);
    }

    private Instant parseTrafficInstant(String input) {

        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(
                    input.trim()
            ).toInstant();

        } catch (DateTimeParseException ignored) {

            try {
                return LocalDateTime.parse(input.trim())
                        .atZone(
                                ZoneId.of("Europe/London")
                        )
                        .toInstant();

            } catch (DateTimeParseException invalidDate) {
                return null;
            }
        }
    }

    private String formatIsoDate(String isoString) {

        if (isoString == null
                || isoString.trim().isEmpty()) {
            return "N/A";
        }

        Instant timestamp =
                parseTrafficInstant(isoString);

        if (timestamp == null) {
            return isoString;
        }

        return DateTimeFormatter.ofPattern(
                "dd MMM yyyy, HH:mm",
                Locale.UK
        ).withZone(
                ZoneId.of("Europe/London")
        ).format(timestamp);
    }

    public long getDurationHours() {

        Instant start =
                parseTrafficInstant(overallStartTime);

        Instant end =
                parseTrafficInstant(overallEndTime);

        if (start == null
                || end == null
                || !end.isAfter(start)) {
            return -1L;
        }

        return Duration.between(
                start, end
        ).toHours();
    }

    public String getCalculatedDuration() {

        if (overallStartTime.isEmpty()
                || overallEndTime.isEmpty()) {
            return "Ongoing / Unspecified";
        }

        Instant start =
                parseTrafficInstant(overallStartTime);

        Instant end =
                parseTrafficInstant(overallEndTime);

        if (start == null || end == null) {
            return "Duration N/A";
        }

        if (!end.isAfter(start)) {
            return "Ended";
        }

        long hours = Duration.between(
                start, end
        ).toHours();

        long days = hours / 24;

        if (days > 30) {
            DateTimeFormatter month =
                    DateTimeFormatter.ofPattern(
                            "MMM yyyy",
                            Locale.UK
                    ).withZone(
                            ZoneId.of("Europe/London")
                    );

            return month.format(start)
                    + " – "
                    + month.format(end)
                    + " (Long term)";
        }

        if (days >= 1) {
            return days + (
                    days == 1 ? " day" : " days"
            );
        }

        if (hours >= 1) {
            return hours + (
                    hours == 1 ? " hour" : " hours"
            );
        }

        return "Under 1 hour";
    }
}