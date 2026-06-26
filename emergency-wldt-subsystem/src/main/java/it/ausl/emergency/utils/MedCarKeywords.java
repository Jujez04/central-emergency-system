package it.ausl.emergency.utils;

/**
 * Utility class for defining fundamental keywords used for defining MedCar
 * properties and behavior.
 */
public class MedCarKeywords {

    // Property Keys
    public static final String STATE_PROPERTY_KEY = "medcar:state:property:key";
    public static final String LATITUDE_PROPERTY_KEY = "medcar:latitude:property:key";
    public static final String LONGITUDE_PROPERTY_KEY = "medcar:longitude:property:key";
    public static final String PATIENT_ID_PROPERTY_KEY = "medcar:patient:id:property:key";
    public static final String HOME_BASE_ID_PROPERTY_KEY = "medcar:home:base:id:property:key";
    public static final String FUEL_LEVEL_PROPERTY_KEY = "medcar:fuel:level:property:key";
    public static final String MISSIONS_PROPERTY_KEY = "medcar:missions:property:key";
    public static final String NEEDS_REFUELING_PROPERTY_KEY = "medcar:needs:refueling:property:key";
    public static final String NEEDS_MAINTENANCE_PROPERTY_KEY = "medcar:needs:maintenance:property:key";
    public static final String TIMESTAMP_PROPERTY_KEY = "medcar:timestamp:property:key";
    public static final String TRIP_DISTANCE_PROPERTY_KEY = "medcar:trip:distance:property:key";

    // Domain Event Keys
    public static final String CRITICAL_FUEL_EVENT_KEY = "medcar:critical:fuel:alert:event:key";
    public static final String MAINTENANCE_REQUIRED_EVENT_KEY = "medcar:maintenance:required:event:key";

    public static final double CRITICAL_FUEL_THRESHOLD = 0.20;

    // States
    public static final String STATE_AT_REST = "atRest";
    public static final String STATE_REFUELING = "Refueling";
    public static final String STATE_UNDER_MAINTENANCE = "UnderMaintenance";
    public static final String STATE_MOVING_TO_PATIENT = "MovingToPatient";
    public static final String STATE_TREATING_PATIENT = "TreatingPatient";
    public static final String STATE_RETURNING = "Returning";

    private MedCarKeywords() {
    }
}