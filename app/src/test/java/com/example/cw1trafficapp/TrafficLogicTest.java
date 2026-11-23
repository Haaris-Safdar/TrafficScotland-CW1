package com.example.cw1trafficapp;

import org.junit.Test;

import com.example.cw1trafficapp.model.TrafficCoordinates;
import com.example.cw1trafficapp.model.TrafficLocation;
import com.example.cw1trafficapp.model.TrafficSituation;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TrafficLogicTest {


    @Test
    public void testRoadworksIconClassification() {

        TrafficSituation roadworks = new TrafficSituation();

        roadworks.setSituationRecordType("MaintenanceWorks");
        roadworks.setSubtype("roadworks");

        // Full road closure
        roadworks.setPublicComment(
                "M8 WB Woodside - A804 Phoenix Rd - Closure"
        );

        assertEquals(
                TrafficSituation.IconCategory.ROAD_CLOSED,
                roadworks.getIconCategory()
        );

        // Lane restriction
        roadworks.setPublicComment(
                "M8 EB JCT 17 to JCT 16 - Lane Closure"
        );

        assertEquals(
                TrafficSituation.IconCategory.LANE_RESTRICTION,
                roadworks.getIconCategory()
        );

        // Contraflow
        roadworks.setPublicComment(
                "A90 Brechin - Contraflow S/B"
        );

        assertEquals(
                TrafficSituation.IconCategory.LANE_RESTRICTION,
                roadworks.getIconCategory()
        );

        // Standard maintenance
        roadworks.setPublicComment(
                "Scheduled maintenance on the M8"
        );

        assertEquals(
                TrafficSituation.IconCategory.MAINTENANCE_ROADWORKS,
                roadworks.getIconCategory()
        );
    }


    @Test
    public void testHaversineDistanceCalculation() {

        TrafficSituation situation = new TrafficSituation();
        TrafficLocation location = new TrafficLocation();

        location.setCoordinates(
                new TrafficCoordinates(55.8642, -4.2518)
        );

        situation.setFromLocation(location);

        double distance = situation.getDistanceFromReference(
                TrafficSituation.GLASGOW_LAT,
                TrafficSituation.GLASGOW_LNG
        );

        assertEquals(0.0, distance, 0.1);

        location.setCoordinates(
                new TrafficCoordinates(55.9533, -3.1883)
        );

        distance = situation.getDistanceFromReference(
                TrafficSituation.GLASGOW_LAT,
                TrafficSituation.GLASGOW_LNG
        );

        assertTrue(
                "Distance to Edinburgh should be approximately 66km",
                distance > 60 && distance < 75
        );
    }

    @Test
    public void testDateFormattingAndDuration() {

        TrafficSituation situation = new TrafficSituation();

        situation.setOverallStartTime(
                "2026-08-10T11:37:42+01:00"
        );

        situation.setOverallEndTime(
                "2026-08-20T11:37:42+01:00"
        );

        String formattedStart =
                situation.getFormattedStartTime();

        assertNotNull(formattedStart);

        assertTrue(
                formattedStart.contains("Aug 2026")
                        || formattedStart.contains("10")
        );

        assertEquals(
                "10 days",
                situation.getCalculatedDuration()
        );
    }

    @Test
    public void testRoadNameExtraction() {

        TrafficSituation situation = new TrafficSituation();
        TrafficLocation location = new TrafficLocation();

        location.setIlcDescriptor("A74(M)");
        situation.setFromLocation(location);

        assertEquals(
                "A74(M)",
                situation.getRoadName()
        );

        TrafficSituation second = new TrafficSituation();

        second.setPublicComment(
                "Essential roadworks taking place on the M8 near Junction 18."
        );

        assertEquals("M8", second.getRoadName());
    }

    @Test
    public void testIconCategoryClassification() {

        TrafficSituation situation = new TrafficSituation();

        situation.setSubtype("roadClosed");

        assertEquals(
                TrafficSituation.IconCategory.ROAD_CLOSED,
                situation.getIconCategory()
        );

        situation.setSubtype("queuingTraffic");

        assertEquals(
                TrafficSituation.IconCategory.QUEUING_TRAFFIC,
                situation.getIconCategory()
        );

        situation.setSubtype("roadworks");

        situation.setSituationRecordType(
                "MaintenanceWorks"
        );

        assertEquals(
                TrafficSituation.IconCategory.MAINTENANCE_ROADWORKS,
                situation.getIconCategory()
        );
    }

    @Test
    public void testDynamicDashboardMetricsCalculation() {

        List<TrafficSituation> roadworks = new ArrayList<>();

        TrafficSituation first = new TrafficSituation();
        first.setDelaysType("longDelays");
        first.setSubtype("roadClosed");
        roadworks.add(first);

        TrafficSituation second = new TrafficSituation();
        second.setDelaysType("veryLongDelays");
        second.setPublicComment(
                "M8 Junction 18 - Closure in place"
        );
        roadworks.add(second);

        TrafficSituation third = new TrafficSituation();
        third.setDelaysType("delays");
        roadworks.add(third);

        int longDelays = 0;
        int closures = 0;

        for (TrafficSituation situation : roadworks) {

            if ("longDelays".equalsIgnoreCase(
                    situation.getDelaysType())
                    || "veryLongDelays".equalsIgnoreCase(
                    situation.getDelaysType())) {
                longDelays++;
            }

            if (situation.getIconCategory()
                    == TrafficSituation.IconCategory.ROAD_CLOSED
                    || situation.getPublicComment()
                    .toLowerCase().contains("closure")) {

                closures++;
            }
        }

        assertEquals(2, longDelays);
        assertEquals(2, closures);
    }

    @Test
    public void testIsoOffsetsAcrossDstAndDurationBands() {

        TrafficSituation situation = new TrafficSituation();

        situation.setOverallStartTime(
                "2026-10-24T23:00:00+01:00"
        );

        situation.setOverallEndTime(
                "2026-10-25T03:00:00+00:00"
        );

        assertEquals(
                5L,
                situation.getDurationHours()
        );

        assertEquals(
                "5 hours",
                situation.getCalculatedDuration()
        );

        situation.setOverallEndTime("");

        assertEquals(
                -1L,
                situation.getDurationHours()
        );
    }

    @Test
    public void testStructuredIconTypeTakesPriorityOverDescription() {

        TrafficSituation situation = new TrafficSituation();

        situation.setSituationRecordType(
                "AbnormalTraffic"
        );

        situation.setPublicComment(
                "Road closed to traffic following an abnormal event"
        );

        assertEquals(
                TrafficSituation.IconCategory.ABNORMAL_TRAFFIC,
                situation.getIconCategory()
        );
    }
}