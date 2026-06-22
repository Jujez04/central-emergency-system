package it.ausl.emergency.utils;

/**
 * Ubiquitous Language constants for the Mission Digital Twin.
 *
 * The Mission aggregates the full lifecycle of a pre-hospital rescue operation:
 * from the moment the Centrale Operativa dispatches a vehicle until the Handover
 * is formally completed at the hospital.
 *
 * Relationships:
 *  - "has_patient"         → 1..1 PatientDigitalTwin   (created at dispatch)
 *  - "involves_vehicle"    → 1..N VehicleDigitalTwin   (ambulance, medcar, helicopter)
 *  - "targets_hospital"    → 0..1 HospitalDigitalTwin  (set when patient is picked up, may change on deterioration)
 */
public class MissionKeywords {

    private MissionKeywords() {}

    // ── Properties ────────────────────────────────────────────────────────────

    /** Current lifecycle phase of the mission (see STATE_* constants below). */
    public static final String STATE_PROPERTY_KEY              = "mission:state";

    /** Simulation clock time at which the call was received by the Centrale Operativa. */
    public static final String TIME_CALLED_PROPERTY_KEY        = "mission:timeCalled";

    /** Simulation clock time at which the first vehicle reached the scene. */
    public static final String TIME_ON_SCENE_PROPERTY_KEY      = "mission:timeOnScene";

    /** Simulation clock time at which the vehicle departed the scene toward the hospital. */
    public static final String TIME_DEPARTED_PROPERTY_KEY      = "mission:timeDeparted";

    /** Simulation clock time at which the handover was completed. */
    public static final String TIME_HANDOVER_PROPERTY_KEY      = "mission:timeHandover";

    /** KPI D09Z – Alarm-to-Target interval (seconds): timeCalled → timeOnScene. */
    public static final String KPI_D09Z_PROPERTY_KEY           = "mission:kpi:d09z";

    /** Derived KPI – total mission duration (seconds): timeCalled → timeHandover. */
    public static final String KPI_TOTAL_DURATION_PROPERTY_KEY = "mission:kpi:totalDuration";

    /** Triage severity code assigned by the Centrale Operativa during the phone interview. */
    public static final String SEVERITY_CODE_PROPERTY_KEY      = "mission:severityCode";

    /**
     * Confirmed severity code after on-scene clinical assessment.
     * May differ from severityCode (under-triage / over-triage).
     */
    public static final String CONFIRMED_SEVERITY_PROPERTY_KEY = "mission:confirmedSeverityCode";

    /** FHQ pathology identified on scene (SEVERE_TRAUMA, CARDIAC_ARREST, STROKE, …). */
    public static final String PATHOLOGY_PROPERTY_KEY          = "mission:pathology";

    /**
     * ID of the patient agent in the simulation (mirrors PatientKeywords identifiers).
     * Used to resolve the "has_patient" relationship instance.
     */
    public static final String PATIENT_ID_PROPERTY_KEY         = "mission:patientId";

    /**
     * ID of the target hospital (set when patient is picked up).
     * May be updated if ClinicalDeteriorationDetected forces a reroute.
     */
    public static final String HOSPITAL_ID_PROPERTY_KEY        = "mission:hospitalId";

    /**
     * Whether a clinical deterioration event has been detected during transport.
     * Triggers automatic reroute logic in the Centrale Operativa.
     */
    public static final String CLINICAL_DETERIORATED_PROPERTY_KEY = "mission:clinicalDeteriorated";

    // ── Mission lifecycle states ──────────────────────────────────────────────

    /** Initial state: call received, triage interview in progress. */
    public static final String STATE_TRIAGING             = "Triaging";

    /** Vehicle(s) dispatched, moving toward the scene. */
    public static final String STATE_DISPATCHED           = "Dispatched";

    /** Vehicle(s) arrived on scene, clinical assessment in progress. */
    public static final String STATE_ON_SCENE             = "OnScene";

    /** Patient loaded, vehicle moving toward the hospital. */
    public static final String STATE_TRANSPORTING         = "Transporting";

    /**
     * Handover completed; patient responsibility transferred to the Trauma Team.
     * Terminal state — the DT enters the "Done" lifecycle phase after this.
     */
    public static final String STATE_COMPLETED            = "Completed";

    // ── Domain Events ─────────────────────────────────────────────────────────

    /** Emitted when the Centrale Operativa completes triage and dispatches a vehicle. */
    public static final String VEHICLE_DISPATCHED_EVENT_KEY       = "mission:event:vehicleDispatched";

    /** Emitted when the first vehicle reaches the scene (triggers Emergency Context). */
    public static final String ARRIVED_ON_SCENE_EVENT_KEY         = "mission:event:arrivedOnScene";

    /** Emitted after the on-scene clinical assessment is complete. */
    public static final String CLINICAL_ASSESSMENT_EVENT_KEY      = "mission:event:clinicalAssessment";

    /** Emitted when clinical deterioration is detected during transport. */
    public static final String CLINICAL_DETERIORATION_EVENT_KEY   = "mission:event:clinicalDeterioration";

    /** Emitted when the hospital target is set or changed due to reroute. */
    public static final String HOSPITAL_ASSIGNED_EVENT_KEY        = "mission:event:hospitalAssigned";

    /** Emitted when the handover is formally completed (final domain event). */
    public static final String HANDOVER_COMPLETED_EVENT_KEY       = "mission:event:handoverCompleted";

    // ── Relationship names ────────────────────────────────────────────────────

    /** Semantic name for the 1..1 link to the Patient DT. */
    public static final String REL_HAS_PATIENT         = "has_patient";

    /** Semantic name for the 1..N link to the involved Vehicle DTs. */
    public static final String REL_INVOLVES_VEHICLE    = "involves_vehicle";

    /**
     * Semantic name for the 0..1 link to the Hospital DT.
     * Created when the hospital target is confirmed; may be replaced on reroute.
     */
    public static final String REL_TARGETS_HOSPITAL    = "targets_hospital";

    // ── Action keys ──────────────────────────────────────────────────────────

    /**
     * Digital action to override the target hospital (reroute).
     * Body: String hospitalId.
     * Triggered by external systems (e.g., Centrale Operativa DT) when the
     * patient deteriorates or a closer facility becomes available.
     */
    public static final String ACTION_REROUTE_HOSPITAL = "mission:action:rerouteHospital";

    // ── Payload / content types ───────────────────────────────────────────────

    public static final String CONTENT_TYPE_JSON  = "application/json";
    public static final String CONTENT_TYPE_TEXT  = "text/plain";
}