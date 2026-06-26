package it.ausl.emergency.adapter.configuration;

import it.ausl.emergency.utils.MedHelicopterKeywords;

/**
 * Configurazione del Digital Twin del MedHelicopter.
 * Contiene i valori iniziali pubblicati nella PAD durante la fase di binding.
 */
public class MedHelicopterAdapterConfiguration {

    private String defaultState = MedHelicopterKeywords.STATE_AT_REST;
    private double defaultLatitude = 0.0;
    private double defaultLongitude = 0.0;
    private String defaultPatientId = "null";
    private String defaultHospitalId = "null";
    private String defaultHomeBase = "null";
    private double defaultFuelLevel = 1.0;
    private int defaultMissionsSinceMaintenance = 0;
    private boolean defaultNeedsRefueling = false;
    private boolean defaultNeedsMaintenance = false;
    private double defaultTimestamp = 0.0;
    private double defaultTripDistanceSinceEmergency = 0.0;

    public MedHelicopterAdapterConfiguration() {
    }

    public MedHelicopterAdapterConfiguration(
            String defaultState,
            double defaultLatitude,
            double defaultLongitude,
            String defaultPatientId,
            String defaultHospitalId,
            String defaultHomeBase,
            double defaultFuelLevel,
            int defaultMissionsSinceMaintenance,
            boolean defaultNeedsRefueling,
            boolean defaultNeedsMaintenance,
            double defaultTimestamp,
            double defaultTripDistanceSinceEmergency) {
        this.defaultState = defaultState;
        this.defaultLatitude = defaultLatitude;
        this.defaultLongitude = defaultLongitude;
        this.defaultPatientId = defaultPatientId;
        this.defaultHospitalId = defaultHospitalId;
        this.defaultHomeBase = defaultHomeBase;
        this.defaultFuelLevel = defaultFuelLevel;
        this.defaultMissionsSinceMaintenance = defaultMissionsSinceMaintenance;
        this.defaultNeedsRefueling = defaultNeedsRefueling;
        this.defaultNeedsMaintenance = defaultNeedsMaintenance;
        this.defaultTimestamp = defaultTimestamp;
        this.defaultTripDistanceSinceEmergency = defaultTripDistanceSinceEmergency;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getDefaultState() {
        return defaultState;
    }

    public void setDefaultState(String v) {
        this.defaultState = v;
    }

    public double getDefaultLatitude() {
        return defaultLatitude;
    }

    public void setDefaultLatitude(double v) {
        this.defaultLatitude = v;
    }

    public double getDefaultLongitude() {
        return defaultLongitude;
    }

    public void setDefaultLongitude(double v) {
        this.defaultLongitude = v;
    }

    public String getDefaultPatientId() {
        return defaultPatientId;
    }

    public void setDefaultPatientId(String v) {
        this.defaultPatientId = v;
    }

    public String getDefaultHospitalId() {
        return defaultHospitalId;
    }

    public void setDefaultHospitalId(String v) {
        this.defaultHospitalId = v;
    }

    public String getDefaultHomeBase() {
        return defaultHomeBase;
    }

    public void setDefaultHomeBase(String v) {
        this.defaultHomeBase = v;
    }

    public double getDefaultFuelLevel() {
        return defaultFuelLevel;
    }

    public void setDefaultFuelLevel(double v) {
        this.defaultFuelLevel = v;
    }

    public int getDefaultMissionsSinceMaintenance() {
        return defaultMissionsSinceMaintenance;
    }

    public void setDefaultMissionsSinceMaintenance(int v) {
        this.defaultMissionsSinceMaintenance = v;
    }

    public boolean isDefaultNeedsRefueling() {
        return defaultNeedsRefueling;
    }

    public void setDefaultNeedsRefueling(boolean v) {
        this.defaultNeedsRefueling = v;
    }

    public boolean isDefaultNeedsMaintenance() {
        return defaultNeedsMaintenance;
    }

    public void setDefaultNeedsMaintenance(boolean v) {
        this.defaultNeedsMaintenance = v;
    }

    public double getDefaultTimestamp() {
        return defaultTimestamp;
    }

    public void setDefaultTimestamp(double v) {
        this.defaultTimestamp = v;
    }

    public double getDefaultTripDistanceSinceEmergency() {
        return defaultTripDistanceSinceEmergency;
    }

    public void setDefaultTripDistanceSinceEmergency(double v) {
        this.defaultTripDistanceSinceEmergency = v;
    }
}
