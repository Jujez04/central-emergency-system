package it.ausl.emergency.utils;

public class CentralEmergencyKeywords {

    // ── Property Keys ────────────────────────────────────────────────────────
    public static final String PROPERTY_STATUS = "central:status";
    public static final String PROPERTY_ACTIVE_MISSIONS = "central:activeMissionsCount";

    // KPI
    public static final String KPI_AVG_D09Z = "central:kpi:avgD09zSeconds";
    public static final String KPI_MISSIONS_COMPLETED = "central:kpi:missionsCompleted";
    public static final String KPI_VEHICLES_NEEDING_MAINTENANCE = "central:kpi:vehiclesNeedingMaintenance";
    public static final String KPI_VEHICLES_LOW_FUEL = "central:kpi:vehiclesLowFuel";
    public static final String KPI_SATURATION_SCORE = "central:kpi:saturationScore";
    public static final String KPI_OVER_TRIAGE_COUNT = "central:kpi:overTriageCount";
    public static final String KPI_UNDER_TRIAGE_COUNT = "central:kpi:underTriageCount";
    public static final String KPI_TRIAGE_TOTAL_ASSESSED = "central:kpi:triageTotalAssessed";

    // ── Semantic Relationship Names ──────────────────────────────────────────
    public static final String REL_MONITORS_HOSPITAL = "central:relationship:monitorsHospital";
    public static final String REL_MANAGES_VEHICLE = "central:relationship:managesVehicle";
    public static final String REL_TRACKS_MISSION = "central:relationship:tracksMission";

    // ── Closed-Loop Actuation Actions ────────────────────────────────────────
    public static final String ACTION_TRIAGE = "central:action:triagePatient";
    public static final String ACTION_REDIRECT = "central:action:redirectMission";

    // ── Content Types ────────────────────────────────────────────────────────
    public static final String CONTENT_TYPE_JSON = "application/json";

    private CentralEmergencyKeywords() {
    }
}