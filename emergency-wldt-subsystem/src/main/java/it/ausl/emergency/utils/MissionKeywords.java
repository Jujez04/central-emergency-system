package it.ausl.emergency.utils;

/**
 * Domain constants and WLDT state boundary property keys for the aggregated
 * Mission entity.
 * Orchestrates cross-entity tracking KPIs and triggers complex closed-loop
 * optimization actions.
 */
public class MissionKeywords {

    // State Properties
    public static final String STATE_PROPERTY_KEY = "mission:state";
    public static final String SEVERITY_CODE_PROPERTY_KEY = "mission:severityCode";
    public static final String CONFIRMED_SEVERITY_PROPERTY_KEY = "mission:confirmedSeverityCode";
    public static final String PATHOLOGY_PROPERTY_KEY = "mission:pathology";
    public static final String PATIENT_ID_PROPERTY_KEY = "mission:patientId";
    public static final String HOSPITAL_ID_PROPERTY_KEY = "mission:hospitalId";
    public static final String CLINICAL_DETERIORATED_PROPERTY_KEY = "mission:clinicalDeteriorated";
    public static final String TIME_CALLED_PROPERTY_KEY = "mission:timeCalled";
    public static final String TIME_ON_SCENE_PROPERTY_KEY = "mission:timeOnScene";
    public static final String TIME_DEPARTED_PROPERTY_KEY = "mission:timeDeparted";
    public static final String TIME_HANDOVER_PROPERTY_KEY = "mission:timeHandover";

    // Augmented Core KPIs (Calculated by the Shadowing Function)
    public static final String KPI_D09Z_PROPERTY_KEY = "mission:kpi:d09zSeconds";
    public static final String KPI_TOTAL_DURATION_PROPERTY_KEY = "mission:kpi:totalDurationSeconds";

    // Nominal Mission Lifecycles
    public static final String STATE_TRIAGING = "Triaging";
    public static final String STATE_DISPATCHED = "Dispatched";
    public static final String STATE_ON_SCENE = "OnScene";
    public static final String STATE_TRANSPORTING = "Transporting";
    public static final String STATE_COMPLETED = "Completed";

    // Core Domain Events
    public static final String VEHICLE_DISPATCHED_EVENT_KEY = "mission:event:vehicle_dispatched";
    public static final String ARRIVED_ON_SCENE_EVENT_KEY = "mission:event:arrived_on_scene";
    public static final String CLINICAL_ASSESSMENT_EVENT_KEY = "mission:event:clinical_assessment";
    public static final String CLINICAL_DETERIORATION_EVENT_KEY = "mission:event:clinical_deterioration";
    public static final String HOSPITAL_ASSIGNED_EVENT_KEY = "mission:event:hospital_assigned";
    public static final String HANDOVER_COMPLETED_EVENT_KEY = "mission:event:handover_completed";

    // Semantic Domain Relationships
    public static final String REL_HAS_PATIENT = "mission:rel:has_patient";
    public static final String REL_INVOLVES_VEHICLE = "mission:rel:involves_vehicle";
    public static final String REL_TARGETS_HOSPITAL = "mission:rel:targets_hospital";

    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_TEXT = "text/plain";
}