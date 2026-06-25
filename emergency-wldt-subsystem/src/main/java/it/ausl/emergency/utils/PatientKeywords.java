package it.ausl.emergency.utils;

/**
 * Utility class for Domain constants and state boundary property keys for the
 * Patient entity.
 */
public class PatientKeywords {

    // State Properties
    public static final String STATE_PROPERTY_KEY = "patient:state";
    public static final String SEVERITY_CODE_PROPERTY_KEY = "patient:severityCode";
    public static final String CONFIRMED_SEVERITY_CODE_PROPERTY_KEY = "patient:confirmedSeverityCode";
    public static final String PATHOLOGY_PROPERTY_KEY = "patient:pathology";
    public static final String GCS_SCORE_PROPERTY_KEY = "patient:gcsScore";
    public static final String AIRWAY_OBSTRUCTED_PROPERTY_KEY = "patient:isAirwayObstructed";
    public static final String EXTERNAL_HEMORRHAGE_PROPERTY_KEY = "patient:hasExternalHemorrhage";
    public static final String CLINICAL_DETERIORATED_PROPERTY_KEY = "patient:isClinicalDeteriorated";
    public static final String LATITUDE_PROPERTY_KEY = "patient:latitude";
    public static final String LONGITUDE_PROPERTY_KEY = "patient:longitude";
    public static final String TIME_CALLED_PROPERTY_KEY = "patient:timeCalled";

    // Nominal Patient States
    public static final String STATE_SIGNALED = "Signaled";
    public static final String STATE_WAITING_SUPPORT = "WaitingSupport";
    public static final String STATE_BEING_TREATED = "BeingTreated";
    public static final String STATE_MOVING_TO_HOSPITAL = "MovingToHospital";
    public static final String STATE_AT_HOSPITAL = "AtHospital";
    public static final String HANDOVER_STATE_VALUE = "Handover";

    // Triage Severity Codes
    public static final String SEVERITY_WHITE = "WHITE";
    public static final String SEVERITY_GREEN = "GREEN";
    public static final String SEVERITY_YELLOW = "YELLOW";
    public static final String SEVERITY_RED = "RED";

    // Pathology Nominal Values
    public static final String PATHOLOGY_NONE = "NONE";

    // Core Domain Events
    public static final String CLINICAL_ASSESSMENT_EVENT_KEY = "patient:event:clinical_assessment";
    public static final String CLINICAL_DETERIORATION_EVENT_KEY = "patient:event:clinical_deterioration";
    public static final String HANDOVER_COMPLETED_EVENT_KEY = "patient:event:handover_completed";
}