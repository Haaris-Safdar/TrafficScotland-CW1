package com.example.cw1trafficapp.model;

import java.io.Serializable;
import java.util.Locale;

/**
 * Model class representing geographical latitude and longitude coordinates.
 */
public class TrafficCoordinates implements Serializable {

    private double latitude;
    private double longitude;

    public TrafficCoordinates() {

    }

    public TrafficCoordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isValid() {
        return (latitude != 0.0 || longitude != 0.0) && latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0;
    }

    @Override
    public String toString() {
        if (!isValid()) {
            return "N/A";
        }
        return String.format(Locale.UK, "%.5f, %.5f", latitude, longitude);
    }
}
