package it.ausl.emergency.adapter.configuration;

import it.ausl.emergency.utils.MissionKeywords;

/**
 * Configuration for the Mission Digital Twin adapter.
 * Carries the initial default values that the Physical Adapter publishes
 * inside the PhysicalAssetDescription (PAD) at binding time.
 *
 * The default hospital ID is intentionally "null" because the hospital target
 * is unknown at dispatch time and will be set (and later possibly changed) as
 * the mission evolves.
 */
public class MissionAdapterConfiguration {

    private String defaultState = MissionKeywords.STATE_TRIAGING;
    private String defaultSeverityCode = "null";
    private String defaultConfirmedSeverity = "null";
    private String defaultPathology = "null";
    private String defaultPatientId = "null";
    private String defaultHospitalId = "null";
    private boolean defaultClinicalDeteriorated = false;
    private double defaultTimeCalled = 0.0;
    private double defaultTimeOnScene = 0.0;
    private double defaultTimeDeparted = 0.0;
    private double defaultTimeHandover = 0.0;

    // ── Constructors ─────────────────────────────────────────────────────────

    public MissionAdapterConfiguration() {
    }

    public MissionAdapterConfiguration(
            String defaultState,
            String defaultSeverityCode,
            String defaultConfirmedSeverity,
            String defaultPathology,
            String defaultPatientId,
            String defaultHospitalId,
            boolean defaultClinicalDeteriorated,
            double defaultTimeCalled,
            double defaultTimeOnScene,
            double defaultTimeDeparted,
            double defaultTimeHandover) {

        this.defaultState = defaultState;
        this.defaultSeverityCode = defaultSeverityCode;
        this.defaultConfirmedSeverity = defaultConfirmedSeverity;
        this.defaultPathology = defaultPathology;
        this.defaultPatientId = defaultPatientId;
        this.defaultHospitalId = defaultHospitalId;
        this.defaultClinicalDeteriorated = defaultClinicalDeteriorated;
        this.defaultTimeCalled = defaultTimeCalled;
        this.defaultTimeOnScene = defaultTimeOnScene;
        this.defaultTimeDeparted = defaultTimeDeparted;
        this.defaultTimeHandover = defaultTimeHandover;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getDefaultState() {
        return defaultState;
    }

    public void setDefaultState(String v) {
        this.defaultState = v;
    }

    public String getDefaultSeverityCode() {
        return defaultSeverityCode;
    }

    public void setDefaultSeverityCode(String v) {
        this.defaultSeverityCode = v;
    }

    public String getDefaultConfirmedSeverity() {
        return defaultConfirmedSeverity;
    }

    public void setDefaultConfirmedSeverity(String v) {
        this.defaultConfirmedSeverity = v;
    }

    public String getDefaultPathology() {
        return defaultPathology;
    }

    public void setDefaultPathology(String v) {
        this.defaultPathology = v;
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

    public boolean isDefaultClinicalDeteriorated() {
        return defaultClinicalDeteriorated;
    }

    public void setDefaultClinicalDeteriorated(boolean v) {
        this.defaultClinicalDeteriorated = v;
    }

    public double getDefaultTimeCalled() {
        return defaultTimeCalled;
    }

    public void setDefaultTimeCalled(double v) {
        this.defaultTimeCalled = v;
    }

    public double getDefaultTimeOnScene() {
        return defaultTimeOnScene;
    }

    public void setDefaultTimeOnScene(double v) {
        this.defaultTimeOnScene = v;
    }

    public double getDefaultTimeDeparted() {
        return defaultTimeDeparted;
    }

    public void setDefaultTimeDeparted(double v) {
        this.defaultTimeDeparted = v;
    }

    public double getDefaultTimeHandover() {
        return defaultTimeHandover;
    }

    public void setDefaultTimeHandover(double v) {
        this.defaultTimeHandover = v;
    }

    @Override
    public String toString() {
        return "MissionAdapterConfiguration{"
                + "state=" + defaultState
                + ", severityCode=" + defaultSeverityCode
                + ", patientId=" + defaultPatientId
                + ", hospitalId=" + defaultHospitalId
                + ", timeCalled=" + defaultTimeCalled
                + '}';
    }
}