package it.ausl.emergency.utils;

public class AmbulanceKeywords {

    // ── Property Keys ─────────────────────────────────────────────────────────
    public static final String STATE_PROPERTY_KEY = "ambulance-state-property-key";
    public static final String LATITUDE_PROPERTY_KEY = "ambulance-latitude-property-key";
    public static final String LONGITUDE_PROPERTY_KEY = "ambulance-longitude-property-key";
    public static final String PATIENT_ID_PROPERTY_KEY = "ambulance-patient-id-property-key";
    public static final String HOSPITAL_ID_PROPERTY_KEY = "ambulance-hospital-id-property-key";
    public static final String HOME_BASE_ID_PROPERTY_KEY = "ambulance-home-base-id-property-key";
    public static final String FUEL_LEVEL_PROPERTY_KEY = "ambulance-fuel-level-property-key";
    public static final String MISSIONS_SINCE_MAINTENANCE_PROPERTY_KEY = "ambulance-missions-since-maintenance-property-key";
    public static final String NEEDS_REFUELING_PROPERTY_KEY = "ambulance-needs-refueling-property-key";
    public static final String NEEDS_MAINTENANCE_PROPERTY_KEY = "ambulance-needs-maintenance-property-key";
    public static final String TIMESTAMP_PROPERTY_KEY = "ambulance-timestamp-property-key";
    public static final String TRIP_DISTANCE_PROPERTY_KEY = "ambulance-trip-distance-property-key";

    // ── Event Keys (Domain Events DDD) ────────────────────────────────────────
    /** atRest → MovingToPatient: missione assegnata dalla Centrale Operativa */
    public static final String MISSION_ASSIGNED_EVENT_KEY = "mission-assegnied-event-key";
    /**
     * TakingPatient → Supporting o MovingToHospital: ambulanza arrivata, paziente
     * preso in carico
     */
    public static final String PATIENT_ONBOARD_EVENT_KEY = "patient-taken-event-key";
    /** → Handover: consegna del paziente all'ospedale */
    public static final String HOSPITAL_HANDOVER_EVENT_KEY = "handover-event-key";
    /** fronte false→true su needsRefueling */
    public static final String REFUELING_NEEDED_EVENT_KEY = "refueling-event-key";
    /** fronte false→true su needsMaintenance */
    public static final String MAINTENANCE_NEEDED_EVENT_KEY = "maintenance-event-key";

    // ── Valori degli stati dello statechart AnyLogic ──────────────────────────
    public static final String STATE_AT_REST = "atRest";
    public static final String STATE_MOVING_TO_PATIENT = "MovingToPatient";
    public static final String STATE_TAKING_PATIENT = "TakingPatient";
    public static final String STATE_SUPPORTING = "Supporting";
    public static final String STATE_MOVING_TO_HOSPITAL = "MovingToHospital";
    public static final String STATE_HANDOVER = "Handover";
    public static final String STATE_SANITIZING = "Sanitizing";
    public static final String STATE_RETURNING = "Returning";
    public static final String STATE_REFUELING = "Refueling";
    public static final String STATE_UNDER_MAINTENANCE = "UnderMaintenance";

    // ── Valore sentinella per campi opzionali ─────────────────────────────────
    /**
     * Valore pubblicato da AnyLogic quando targetPatient o targetHospital è null
     */
    public static final String NULL_REFERENCE = "null";

    private AmbulanceKeywords() {
    }
}