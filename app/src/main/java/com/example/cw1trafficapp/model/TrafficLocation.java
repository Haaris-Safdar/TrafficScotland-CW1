package com.example.cw1trafficapp.model;

import java.io.Serializable;

/**
 * Model class representing a location point (junction or non-junction point).
 */
public class TrafficLocation implements Serializable {

    private String primaryRoadName = "";
    private String secondaryLocationName = "";
    private String junctionName = "";
    private String ilcDescriptor = "";
    private String otherName = "";
    private TrafficCoordinates coordinates = new TrafficCoordinates();

    public TrafficLocation() {
    }

    public String getPrimaryRoadName() {
        if (!primaryRoadName.isEmpty()) {
            return primaryRoadName;
        }
        if (!ilcDescriptor.isEmpty()) {
            return ilcDescriptor;
        }
        return "";
    }

    public void setPrimaryRoadName(String primaryRoadName) {
        if (primaryRoadName != null && !primaryRoadName.trim().isEmpty()) {
            this.primaryRoadName = primaryRoadName.trim();
        }
    }

    public String getSecondaryLocationName() {
        return secondaryLocationName;
    }

    public void setSecondaryLocationName(String secondaryLocationName) {
        if (secondaryLocationName != null && !secondaryLocationName.trim().isEmpty()) {
            this.secondaryLocationName = secondaryLocationName.trim();
        }
    }

    public String getJunctionName() {
        return junctionName;
    }

    public void setJunctionName(String junctionName) {
        this.junctionName = junctionName != null ? junctionName.trim() : "";
    }

    public String getIlcDescriptor() {
        return ilcDescriptor;
    }

    public void setIlcDescriptor(String ilcDescriptor) {
        this.ilcDescriptor = ilcDescriptor != null ? ilcDescriptor.trim() : "";
    }

    public String getOtherName() {
        return otherName;
    }

    public void setOtherName(String otherName) {
        this.otherName = otherName != null ? otherName.trim() : "";
    }

    public TrafficCoordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(TrafficCoordinates coordinates) {
        if (coordinates != null) {
            this.coordinates = coordinates;
        }
    }

    public String getDisplayName() {
        if (!otherName.isEmpty()) {
            return otherName;
        }
        if (!secondaryLocationName.isEmpty()) {
            return secondaryLocationName;
        }
        if (!junctionName.isEmpty() && !getPrimaryRoadName().isEmpty()) {
            return getPrimaryRoadName() + " " + junctionName;
        }
        if (!getPrimaryRoadName().isEmpty()) {
            return getPrimaryRoadName();
        }
        if (!junctionName.isEmpty()) {
            return junctionName;
        }
        return "Unknown Location";
    }
}
