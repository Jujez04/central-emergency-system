package it.ausl.emergency.utils;

public class CentralEmergencyKeywords {

    // ── Property Keys ────────────────────────────────────────────────────────
    public static final String PROPERTY_STATUS = "central:status";
    public static final String PROPERTY_ACTIVE_MISSIONS = "central:activeMissionsCount";

    // ── Semantic Relationship Names ──────────────────────────────────────────
    public static final String REL_MONITORS_HOSPITAL = "central:relationship:monitorsHospital";
    public static final String REL_MANAGES_VEHICLE   = "central:relationship:managesVehicle";
    public static final String REL_TRACKS_MISSION    = "central:relationship:tracksMission";

    // ── Closed-Loop Actuation Actions ────────────────────────────────────────
    public static final String ACTION_TRIAGE  = "central:action:triagePatient";
    public static final String ACTION_REDIRECT = "central:action:redirectMission";

    // ── Content Types ────────────────────────────────────────────────────────
    public static final String CONTENT_TYPE_JSON = "application/json";

    private CentralEmergencyKeywords() {}
}