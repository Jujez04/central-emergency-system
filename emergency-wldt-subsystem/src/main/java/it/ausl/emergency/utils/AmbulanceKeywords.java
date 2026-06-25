package it.ausl.emergency.utils;

/**
 * Domain constants and WLDT state boundary property keys for the Ambulance entity.
 * Defines the Ubiquitous Language terms and event identifiers used during telemetry ingestion.
 */
public class AmbulanceKeywords {

    // -- State Properties
    public static final String STATE_PROPERTY_KEY = "ambulance:state";
    public static final String LATITUDE_PROPERTY_KEY = "ambulance:latitude";
    public static final String LONGITUDE_PROPERTY_KEY = "ambulance:longitude";
    public static final String PATIENT_ID_PROPERTY_KEY = "ambulance:patientId";
    public static final String HOSPITAL_ID_PROPERTY_KEY = "ambulance:hospitalId";
    public static final String FUEL_LEVEL_PROPERTY_KEY = "ambulance:fuelLevel";
    public static final String MISSIONS_PROPERTY_KEY = "ambulance:missionsSinceMaintenance";
    public static final String NEEDS_REFUELING_PROPERTY_KEY = "ambulance:needsRefueling";
    public static final String NEEDS_MAINTENANCE_PROPERTY_KEY = "ambulance:needsMaintenance";
    public static final String TIMESTAMP_PROPERTY_KEY = "ambulance:timestamp";
    public static final String TRIP_DISTANCE_PROPERTY_KEY = "ambulance:tripDistanceSinceEmergency";

    // -- Nominal Operational States
    public static final String STATE_AT_REST = "atRest";
    public static final String STATE_MOVING_TO_PATIENT = "MovingToPatient";
    public static final String STATE_TAKING_PATIENT = "TakingPatient";
    public static final String STATE_MOVING_TO_HOSPITAL = "MovingToHospital";
    public static final String STATE_HANDOVER = "Handover";

    // -- Core Domain Events
    public static final String CRITICAL_FUEL_EVENT_KEY = "ambulance:event:critical_fuel";
    public static final String MAINTENANCE_REQUIRED_EVENT_KEY = "ambulance:event:maintenance_required";

    // -- Actuation Actions
    public static final String REDIRECT_VEHICLE_ACTION_KEY = "ambulance:action:redirect";
}