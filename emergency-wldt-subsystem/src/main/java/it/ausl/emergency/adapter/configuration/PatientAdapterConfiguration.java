package it.ausl.emergency.adapter.configuration;

import it.ausl.emergency.utils.PatientKeywords;

public class PatientAdapterConfiguration {

    // Allineamento rigido ai valori nominali del modello DDD ed AnyLogic
    private String defaultState = PatientKeywords.STATE_SIGNALED;
    private String defaultSeverityCode = PatientKeywords.SEVERITY_WHITE;
    private String defaultConfirmedSeverityCode = PatientKeywords.SEVERITY_WHITE;
    private String defaultPathology = PatientKeywords.PATHOLOGY_NONE;
    private int defaultGcsScore = 15;
    private boolean defaultAirwayObstructed = false;
    private boolean defaultExternalHemorrhage = false;
    private boolean defaultClinicalDeteriorated = false;
    private double defaultLatitude = 0.0;
    private double defaultLongitude = 0.0;
    private double defaultTimeCalled = 0.0;

    public PatientAdapterConfiguration() {
    }

    public PatientAdapterConfiguration(String defaultState, String defaultSeverityCode,
            String defaultConfirmedSeverityCode, String defaultPathology,
            int defaultGcsScore, boolean defaultAirwayObstructed,
            boolean defaultExternalHemorrhage, boolean defaultClinicalDeteriorated,
            double defaultLatitude, double defaultLongitude, double defaultTimeCalled) {
        this.defaultState = defaultState;
        this.defaultSeverityCode = defaultSeverityCode;
        this.defaultConfirmedSeverityCode = defaultConfirmedSeverityCode;
        this.defaultPathology = defaultPathology;
        this.defaultGcsScore = defaultGcsScore;
        this.defaultAirwayObstructed = defaultAirwayObstructed;
        this.defaultExternalHemorrhage = defaultExternalHemorrhage;
        this.defaultClinicalDeteriorated = defaultClinicalDeteriorated;
        this.defaultLatitude = defaultLatitude;
        this.defaultLongitude = defaultLongitude;
        this.defaultTimeCalled = defaultTimeCalled;
    }

    // ── Getters e Setters ─────────────────────────────────────────────────────

    public String getDefaultState() {
        return defaultState;
    }

    public void setDefaultState(String defaultState) {
        this.defaultState = defaultState;
    }

    public String getDefaultSeverityCode() {
        return defaultSeverityCode;
    }

    public void setDefaultSeverityCode(String defaultSeverityCode) {
        this.defaultSeverityCode = defaultSeverityCode;
    }

    public String getDefaultConfirmedSeverityCode() {
        return defaultConfirmedSeverityCode;
    }

    public void setDefaultConfirmedSeverityCode(String defaultConfirmedSeverityCode) {
        this.defaultConfirmedSeverityCode = defaultConfirmedSeverityCode;
    }

    public String getDefaultPathology() {
        return defaultPathology;
    }

    public void setDefaultPathology(String defaultPathology) {
        this.defaultPathology = defaultPathology;
    }

    public int getDefaultGcsScore() {
        return defaultGcsScore;
    }

    public void setDefaultGcsScore(int defaultGcsScore) {
        this.defaultGcsScore = defaultGcsScore;
    }

    public boolean isDefaultAirwayObstructed() {
        return defaultAirwayObstructed;
    }

    public void setDefaultAirwayObstructed(boolean defaultAirwayObstructed) {
        this.defaultAirwayObstructed = defaultAirwayObstructed;
    }

    public boolean isDefaultExternalHemorrhage() {
        return defaultExternalHemorrhage;
    }

    public void setDefaultExternalHemorrhage(boolean defaultExternalHemorrhage) {
        this.defaultExternalHemorrhage = defaultExternalHemorrhage;
    }

    public boolean isDefaultClinicalDeteriorated() {
        return defaultClinicalDeteriorated;
    }

    public void setDefaultClinicalDeteriorated(boolean defaultClinicalDeteriorated) {
        this.defaultClinicalDeteriorated = defaultClinicalDeteriorated;
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

    public double getDefaultTimeCalled() {
        return defaultTimeCalled;
    }

    public void setDefaultTimeCalled(double defaultTimeCalled) {
        this.defaultTimeCalled = defaultTimeCalled;
    }

    @Override
    public String toString() {
        return "PatientAdapterConfiguration{" +
                "defaultState='" + defaultState + '\'' +
                ", defaultSeverityCode='" + defaultSeverityCode + '\'' +
                ", defaultConfirmedSeverityCode='" + defaultConfirmedSeverityCode + '\'' +
                ", defaultPathology='" + defaultPathology + '\'' +
                ", defaultGcsScore=" + defaultGcsScore +
                ", defaultAirwayObstructed=" + defaultAirwayObstructed +
                ", defaultExternalHemorrhage=" + defaultExternalHemorrhage +
                ", defaultClinicalDeteriorated=" + defaultClinicalDeteriorated +
                ", defaultLatitude=" + defaultLatitude +
                ", defaultLongitude=" + defaultLongitude +
                ", defaultTimeCalled=" + defaultTimeCalled +
                '}';
    }
}