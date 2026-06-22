package it.ausl.emergency.utils;

public class CentraleOperativaKeywords {

    // ── Property Keys ────────────────────────────────────────────────────────
    public static final String PROPERTY_STATUS = "centrale:status";
    public static final String PROPERTY_ACTIVE_MISSIONS = "centrale:activeMissionsCount";

    // ── Semantic Relationship Names ──────────────────────────────────────────
    public static final String REL_MONITORS_HOSPITAL = "centrale:relationship:monitorsHospital";
    public static final String REL_MANAGES_VEHICLE   = "centrale:relationship:managesVehicle";
    public static final String REL_TRACKS_MISSION    = "centrale:relationship:tracksMission";

    // ── Closed-Loop Actuation Actions ────────────────────────────────────────
    public static final String ACTION_TRIAGE  = "centrale:action:triagePatient";
    public static final String ACTION_REDIRECT = "centrale:action:redirectMission";

    // ── Content Types ────────────────────────────────────────────────────────
    public static final String CONTENT_TYPE_JSON = "application/json";

    private CentraleOperativaKeywords() {}
}