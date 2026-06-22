package it.ausl.emergency.utils;

/**
 * Costanti per il Digital Twin della MedCar (Automedica).
 *
 * La MedCar si distingue dall'Ambulanza su tre punti strutturali:
 *  1. Ha homeBaseId invece di hospitalId (torna alla postazione fissa, non all'ospedale)
 *  2. Lo statechart è più semplice: atRest → MovingToPatient → TreatingPatient → Returning
 *     (non trasporta il paziente, quindi niente TakingPatient/Supporting/MovingToHospital/
 *      Handover/Sanitizing)
 *  3. Non espone azioni di redirect (non decide la destinazione ospedaliera)
 *
 * Domain Events esposti:
 *  - Mission Assigned      (atRest → MovingToPatient, fronte di salita)
 *  - On Scene Treating     (MovingToPatient → TreatingPatient, fronte di salita)
 *  - Mission Completed     (TreatingPatient → Returning, fronte di salita)
 *  - Critical Fuel Alert   (fuelLevel scende sotto soglia 0.20)
 *  - Maintenance Required  (fronte di salita su needsMaintenance)
 */
public class MedCarKeywords {

    // ── Property Keys ────────────────────────────────────────────────────────
    public static final String STATE_PROPERTY_KEY              = "medcar-state-property-key";
    public static final String LATITUDE_PROPERTY_KEY           = "medcar-latitude-property-key";
    public static final String LONGITUDE_PROPERTY_KEY          = "medcar-longitude-property-key";
    public static final String PATIENT_ID_PROPERTY_KEY         = "medcar-patient-id-property-key";
    public static final String HOME_BASE_ID_PROPERTY_KEY       = "medcar-home-base-id-property-key";
    public static final String FUEL_LEVEL_PROPERTY_KEY         = "medcar-fuel-level-property-key";
    public static final String MISSIONS_PROPERTY_KEY           = "medcar-missions-property-key";
    public static final String NEEDS_REFUELING_PROPERTY_KEY    = "medcar-needs-refueling-property-key";
    public static final String NEEDS_MAINTENANCE_PROPERTY_KEY  = "medcar-needs-maintenance-property-key";
    public static final String TIMESTAMP_PROPERTY_KEY          = "medcar-timestamp-property-key";
    public static final String TRIP_DISTANCE_PROPERTY_KEY      = "medcar-trip-distance-property-key";

    // ── Domain Event Keys ────────────────────────────────────────────────────
    public static final String MISSION_ASSIGNED_EVENT_KEY      = "medcar-mission-assigned-event-key";
    public static final String ON_SCENE_TREATING_EVENT_KEY     = "medcar-on-scene-treating-event-key";
    public static final String MISSION_COMPLETED_EVENT_KEY     = "medcar-mission-completed-event-key";
    public static final String CRITICAL_FUEL_EVENT_KEY         = "medcar-critical-fuel-alert-event-key";
    public static final String MAINTENANCE_REQUIRED_EVENT_KEY  = "medcar-maintenance-required-event-key";

    public static final double CRITICAL_FUEL_THRESHOLD = 0.20;

    public static final String STATE_AT_REST            = "atRest";
    public static final String STATE_REFUELING          = "Refueling";
    public static final String STATE_UNDER_MAINTENANCE  = "UnderMaintenance";
    public static final String STATE_MOVING_TO_PATIENT  = "MovingToPatient";
    public static final String STATE_TREATING_PATIENT   = "TreatingPatient";
    public static final String STATE_RETURNING          = "Returning";

    private MedCarKeywords() {}
}