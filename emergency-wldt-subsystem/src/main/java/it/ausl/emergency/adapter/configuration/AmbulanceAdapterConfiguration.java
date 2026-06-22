package it.ausl.emergency.adapter.configuration;

import it.ausl.emergency.utils.AmbulanceKeywords;

/**
 * Configuration class for the Ambulance Digital Twin components.
 * Holds initial default values for properties used during the binding phase.
 */
public class AmbulanceAdapterConfiguration {

    private String defaultState = AmbulanceKeywords.STATE_AT_REST;
    private double defaultLatitude = 0.0;
    private double defaultLongitude = 0.0;
    private String defaultPatientId = "null";
    private String defaultHospitalId = "null";
    private double defaultFuelLevel = 1.0;
    private int defaultMissionsSinceMaintenance = 0;
    private boolean defaultNeedsRefueling = false;
    private boolean defaultNeedsMaintenance = false;
    private double defaultTimestamp = 0.0;
    private double defaultTripDistanceSinceEmergency = 0.0;

    public AmbulanceAdapterConfiguration() {
    }

    public AmbulanceAdapterConfiguration(String defaultState, double defaultLatitude, double defaultLongitude,
            String defaultPatientId, String defaultHospitalId, double defaultFuelLevel,
            int defaultMissionsSinceMaintenance, boolean defaultNeedsRefueling,
            boolean defaultNeedsMaintenance, double defaultTimestamp,
            double defaultTripDistanceSinceEmergency) {
        this.defaultState = defaultState;
        this.defaultLatitude = defaultLatitude;
        this.defaultLongitude = defaultLongitude;
        this.defaultPatientId = defaultPatientId;
        this.defaultHospitalId = defaultHospitalId;
        this.defaultFuelLevel = defaultFuelLevel;
        this.defaultMissionsSinceMaintenance = defaultMissionsSinceMaintenance;
        this.defaultNeedsRefueling = defaultNeedsRefueling;
        this.defaultNeedsMaintenance = defaultNeedsMaintenance;
        this.defaultTimestamp = defaultTimestamp;
        this.defaultTripDistanceSinceEmergency = defaultTripDistanceSinceEmergency;
    }

    // ── Getters and Setters ──────────────────────────────────────────────────

    public String getDefaultState() {
        return defaultState;
    }

    public void setDefaultState(String defaultState) {
        this.defaultState = defaultState;
    }

    public double getDefaultLatitude() {
        return defaultLatitude;
    }

    public void setDefaultLatitude(double defaultLatitude) {
        this.defaultLatitude = defaultLatitude;
    }

    public double getDefaultLongitude() {
        return defaultLongitude;
    }

    public void setDefaultLongitude(double defaultLongitude) {
        this.defaultLongitude = defaultLongitude;
    }

    public String getDefaultPatientId() {
        return defaultPatientId;
    }

    public void setDefaultPatientId(String defaultPatientId) {
        this.defaultPatientId = defaultPatientId;
    }

    public String getDefaultHospitalId() {
        return defaultHospitalId;
    }

    public void setDefaultHospitalId(String defaultHospitalId) {
        this.defaultHospitalId = defaultHospitalId;
    }

    public double getDefaultFuelLevel() {
        return defaultFuelLevel;
    }

    public void setDefaultFuelLevel(double defaultFuelLevel) {
        this.defaultFuelLevel = defaultFuelLevel;
    }

    public int getDefaultMissionsSinceMaintenance() {
        return defaultMissionsSinceMaintenance;
    }

    public void setDefaultMissionsSinceMaintenance(int defaultMissionsSinceMaintenance) {
        this.defaultMissionsSinceMaintenance = defaultMissionsSinceMaintenance;
    }

    public boolean isDefaultNeedsRefueling() {
        return defaultNeedsRefueling;
    }

    public void setDefaultNeedsRefueling(boolean defaultNeedsRefueling) {
        this.defaultNeedsRefueling = defaultNeedsRefueling;
    }

    public boolean isDefaultNeedsMaintenance() {
        return defaultNeedsMaintenance;
    }

    public void setDefaultNeedsMaintenance(boolean defaultNeedsMaintenance) {
        this.defaultNeedsMaintenance = defaultNeedsMaintenance;
    }

    public double getDefaultTimestamp() {
        return defaultTimestamp;
    }

    public void setDefaultTimestamp(double defaultTimestamp) {
        this.defaultTimestamp = defaultTimestamp;
    }

    public double getDefaultTripDistanceSinceEmergency() {
        return defaultTripDistanceSinceEmergency;
    }

    public void setDefaultTripDistanceSinceEmergency(double defaultTripDistanceSinceEmergency) {
        this.defaultTripDistanceSinceEmergency = defaultTripDistanceSinceEmergency;
    }
}