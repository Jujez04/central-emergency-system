package it.ausl.emergency.utils;

public class AmbulanceKeywords {
    // ── Property Keys (WLDT State Core Namespaces) ───────────────────────────
    public static final String STATE_PROPERTY_KEY = "ambulance-state-property-key";
    public static final String LATITUDE_PROPERTY_KEY = "ambulance-latitude-property-key";
    public static final String LONGITUDE_PROPERTY_KEY = "ambulance-longitude-property-key";
    public static final String PATIENT_ID_PROPERTY_KEY = "ambulance-patient-id-property-key";
    public static final String HOSPITAL_ID_PROPERTY_KEY = "ambulance-hospital-id-property-key";
    public static final String FUEL_LEVEL_PROPERTY_KEY = "ambulance-fuel-level-property-key";
    public static final String MISSIONS_PROPERTY_KEY = "ambulance-missions-property-key";
    public static final String NEEDS_REFUELING_PROPERTY_KEY = "ambulance-needs-refueling-property-key";
    public static final String NEEDS_MAINTENANCE_PROPERTY_KEY = "ambulance-needs-maintenance-property-key";
    public static final String TIMESTAMP_PROPERTY_KEY = "ambulance-timestamp-property-key";
    public static final String TRIP_DISTANCE_PROPERTY_KEY = "ambulance-trip-distance-property-key";

    // ── Domain Event Keys ────────────────────────────────────────────────────
    public static final String MISSION_ASSIGNED_EVENT_KEY = "mission-assigned-event-key";
    public static final String PATIENT_ONBOARD_EVENT_KEY = "patient-onboard-event-key";
    public static final String HOSPITAL_HANDOVER_EVENT_KEY = "hospital-handover-event-key";
    public static final String CRITICAL_FUEL_EVENT_KEY = "critical-fuel-alert-event-key";
    public static final String MAINTENANCE_REQUIRED_EVENT_KEY = "maintenance-required-event-key";

    // ── Physical Action Keys (Inbound Closed-Loop Optimization) ──────────────
    public static final String REDIRECT_VEHICLE_ACTION_KEY = "redirect-vehicle-action-key";

    // ── AnyLogic Statechart Exact Nominal States ─────────────────────────────
    public static final String STATE_AT_REST = "atRest";
    public static final String STATE_REFUELING = "Refueling";
    public static final String STATE_UNDER_MAINTENANCE = "UnderMaintenance";
    public static final String STATE_MOVING_TO_PATIENT = "MovingToPatient";
    public static final String STATE_TAKING_PATIENT = "TakingPatient";
    public static final String STATE_SUPPORTING = "Supporting";
    public static final String STATE_MOVING_TO_HOSPITAL = "MovingToHospital";
    public static final String STATE_HANDOVER = "Handover";
    public static final String STATE_SANITIZING = "Sanitizing";
    public static final String STATE_RETURNING = "Returning";

    private AmbulanceKeywords() {
    }
}