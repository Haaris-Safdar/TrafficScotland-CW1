package com.example.cw1trafficapp;

import org.junit.Before;
import org.junit.Test;

import com.example.cw1trafficapp.model.TrafficSituation;
import com.example.cw1trafficapp.parser.TrafficXmlParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

public class TrafficXmlParserTest {

    private TrafficXmlParser parser;

    @Before
    public void setUp() {
        parser = new TrafficXmlParser();
    }

    private String readAssetFile(
            String filePath) throws Exception {

        File file = new File(filePath);

        assertTrue(
                "Asset file should exist at "
                        + file.getAbsolutePath(),
                file.exists()
        );

        StringBuilder result = new StringBuilder();

        try (
                InputStream stream =
                        new FileInputStream(file);

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        stream,
                                        StandardCharsets.UTF_8
                                )
                        )
        ) {
            String line;

            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    @Test
    public void testUnplannedIncidentsParsingCount()
            throws Exception {

        String xml = readAssetFile(
                "src/test/resources/unplannedContentExample.xml"
        );

        List<TrafficSituation> incidents =
                parser.parseXml(xml, true);

        assertNotNull(incidents);
        assertEquals(4, incidents.size());

        TrafficSituation first = incidents.get(0);

        assertEquals(
                "I_20260810_248_S",
                first.getId()
        );

        assertEquals(
                "I_20260810_248",
                first.getSituationRecordId()
        );

        assertEquals(
                "10",
                first.getVersion()
        );

        assertEquals(
                "RoadOrCarriagewayOrLaneManagement",
                first.getSituationRecordType()
        );

        assertTrue(
                first.getPublicComment()
                        .contains("A74M(S) junction 19")
        );

        assertEquals(
                "2026-08-10T11:37:42+01:00",
                first.getOverallStartTime()
        );

        assertEquals(
                "roadClosed",
                first.getSubtype()
        );

        assertTrue(first.isUnplanned());
        assertTrue(first.getPrimaryCoordinates().isValid());

        assertEquals(
                55.103477,
                first.getFromLocation()
                        .getCoordinates().getLatitude(),
                0.0001
        );

        assertEquals(
                -3.349595,
                first.getFromLocation()
                        .getCoordinates().getLongitude(),
                0.0001
        );
    }

    @Test
    public void testRoadworksParsingCount()
            throws Exception {

        String xml = readAssetFile(
                "src/test/resources/roadworksContentExample.xml"
        );

        List<TrafficSituation> roadworks =
                parser.parseXml(xml, false);

        assertNotNull(roadworks);
        assertEquals(405, roadworks.size());

        TrafficSituation first = roadworks.get(0);

        assertEquals("D_159301_S", first.getId());
        assertEquals(
                "D_159301",
                first.getSituationRecordId()
        );

        assertEquals("1", first.getVersion());

        assertEquals(
                "MaintenanceWorks",
                first.getSituationRecordType()
        );

        assertEquals(
                "longDelays",
                first.getDelaysType()
        );

        assertTrue(
                first.getPublicComment()
                        .contains("M8 WB Woodside")
        );

        assertFalse(first.isUnplanned());

        assertEquals(
                "2023-07-22T00:00:00+01:00",
                first.getOverallStartTime()
        );

        assertEquals(
                "2027-12-31T23:59:00+00:00",
                first.getOverallEndTime()
        );
    }

    @Test
    public void testPrimaryRoadDescriptorExtraction()
            throws Exception {

        String testXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<d2LogicalModel xmlns=\"http://datex2.eu/schema/2/2_0\">\n"
                        + "  <payloadPublication>\n"
                        + "    <situation id=\"S_TEST_A90\">\n"
                        + "      <situationRecord id=\"R_TEST_A90\">\n"
                        + "        <generalPublicComment><comment><values><value>A90 Incident</value></values></comment></generalPublicComment>\n"
                        + "        <groupOfLocations>\n"
                        + "          <tpegLinearLocation>\n"
                        + "            <from>\n"
                        + "              <name><descriptor><values><value>A90</value></values></descriptor><tpegOtherPointDescriptorType>linkName</tpegOtherPointDescriptorType></name>\n"
                        + "              <name><descriptor><values><value>A933 (A90 Jct)</value></values></descriptor><tpegOtherPointDescriptorType>nonLinkedPointName</tpegOtherPointDescriptorType></name>\n"
                        + "            </from>\n"
                        + "          </tpegLinearLocation>\n"
                        + "        </groupOfLocations>\n"
                        + "      </situationRecord>\n"
                        + "    </situation>\n"
                        + "  </payloadPublication>\n"
                        + "</d2LogicalModel>";

        List<TrafficSituation> situations =
                parser.parseXml(testXml, true);

        assertEquals(1, situations.size());

        assertEquals(
                "A90",
                situations.get(0).getRoadName()
        );
    }
}