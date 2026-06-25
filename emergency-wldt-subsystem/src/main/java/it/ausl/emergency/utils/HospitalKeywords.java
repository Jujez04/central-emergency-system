package it.ausl.emergency.utils;

/**
 * Utility class holding nominal state keys and property identifiers for the
 * Hospital Digital Twin.
 * Aligns strictly with AnyLogic simulated clinical points and tracking layers.
 */
public class HospitalKeywords {

    // Property Keys
    public static final String ASSISTANCE_LEVEL_PROPERTY_KEY = "hospital:assistance:level:property:key";
    public static final String PATIENT_ASSISTED_PROPERTY_KEY = "hospital:patient:assisted:property:key";
    public static final String TIMESTAMP_PROPERTY_KEY = "hospital:timestamp:property:key";

    private HospitalKeywords() {
    }
}