package it.ausl.emergency.utils;

/**
 * Costanti per il Digital Twin del MedHelicopter (Elisoccorso).
 *
 * Il MedHelicopter si distingue da MedCar e Ambulanza:
 *  1. Ha sia homeBase (postazione di partenza) sia hospitalId (destinazione paziente)
 *  2. Statechart completo con trasporto paziente:
 *       atRest → MovingToPatient → TakingPatient → MovingToHospital
 *               → Handover → Sanitizing → Returning → atRest
 *     con rami laterali: atRest → Refueling, atRest → UnderMaintenance
 *  3. NON ha lo stato Supporting (a differenza dell'Ambulanza)
 *  4. Non espone azioni di redirect
 *
 * Domain Events:
 *  - Mission Assigned     (atRest → MovingToPatient)
 *  - Patient Onboard      (TakingPatient → MovingToHospital)
 *  - Hospital Handover    (→ Handover)
 *  - Critical Fuel Alert  (fuelLevel scende sotto soglia 0.20)
 *  - Maintenance Required (fronte di salita su needsMaintenance)
 */
public class MedHelicopterKeywords {

    // ── Property Keys ────────────────────────────────────────────────────────
    public static final String STATE_PROPERTY_KEY              = "medhelicopter-state-property-key";
    public static final String LATITUDE_PROPERTY_KEY           = "medhelicopter-latitude-property-key";
    public static final String LONGITUDE_PROPERTY_KEY          = "medhelicopter-longitude-property-key";
    public static final String PATIENT_ID_PROPERTY_KEY         = "medhelicopter-patient-id-property-key";
    public static final String HOSPITAL_ID_PROPERTY_KEY        = "medhelicopter-hospital-id-property-key";
    public static final String HOME_BASE_PROPERTY_KEY          = "medhelicopter-home-base-property-key";
    public static final String FUEL_LEVEL_PROPERTY_KEY         = "medhelicopter-fuel-level-property-key";
    public static final String MISSIONS_PROPERTY_KEY           = "medhelicopter-missions-property-key";
    public static final String NEEDS_REFUELING_PROPERTY_KEY    = "medhelicopter-needs-refueling-property-key";
    public static final String NEEDS_MAINTENANCE_PROPERTY_KEY  = "medhelicopter-needs-maintenance-property-key";
    public static final String TIMESTAMP_PROPERTY_KEY          = "medhelicopter-timestamp-property-key";
    public static final String TRIP_DISTANCE_PROPERTY_KEY      = "medhelicopter-trip-distance-property-key";

    // ── Domain Event Keys ────────────────────────────────────────────────────
    public static final String MISSION_ASSIGNED_EVENT_KEY      = "medhelicopter-mission-assigned-event-key";
    public static final String PATIENT_ONBOARD_EVENT_KEY       = "medhelicopter-patient-onboard-event-key";
    public static final String HOSPITAL_HANDOVER_EVENT_KEY     = "medhelicopter-hospital-handover-event-key";
    public static final String CRITICAL_FUEL_EVENT_KEY         = "medhelicopter-critical-fuel-alert-event-key";
    public static final String MAINTENANCE_REQUIRED_EVENT_KEY  = "medhelicopter-maintenance-required-event-key";

    // ── Soglie ───────────────────────────────────────────────────────────────
    public static final double CRITICAL_FUEL_THRESHOLD = 0.20;

    // ── AnyLogic Statechart States ───────────────────────────────────────────
    public static final String STATE_AT_REST             = "atRest";
    public static final String STATE_REFUELING           = "Refueling";
    public static final String STATE_UNDER_MAINTENANCE   = "UnderMaintenance";
    public static final String STATE_MOVING_TO_PATIENT   = "MovingToPatient";
    public static final String STATE_TAKING_PATIENT      = "TakingPatient";
    public static final String STATE_MOVING_TO_HOSPITAL  = "MovingToHospital";
    public static final String STATE_HANDOVER            = "Handover";
    public static final String STATE_SANITIZING          = "Sanitizing";
    public static final String STATE_RETURNING           = "Returning";

    private MedHelicopterKeywords() {}
}
