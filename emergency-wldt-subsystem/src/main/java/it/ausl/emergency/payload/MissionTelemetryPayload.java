package it.ausl.emergency.payload;

/**
 * Immutable telemetry snapshot emitted by the AnyLogic simulation for a Mission agent.
 *
 * Each field maps to one of the properties declared in {@link it.ausl.emergency.utils.MissionKeywords}.
 * The record is deserialized from the JSON payload received on the MQTT topic:
 *   ces/mission/{agentId}/state
 *
 * Nullable / optional fields (hospitalId) carry "null" as their default string value
 * to match the convention already adopted in AmbulanceAdapterConfiguration.
 *
 * @param state                 Current mission lifecycle phase (see MissionKeywords.STATE_*)
 * @param severityCode          Triage severity assigned during the phone interview
 * @param confirmedSeverityCode Confirmed severity after on-scene clinical assessment
 * @param pathology             FHQ pathology detected on scene
 * @param patientId             Simulation agent ID of the associated patient
 * @param hospitalId            Target hospital agent ID (may be "null" before patient pick-up)
 * @param clinicalDeteriorated  Whether a deterioration event occurred during transport
 * @param timeCalled            Simulation clock (seconds) when the call was received
 * @param timeOnScene           Simulation clock when the vehicle arrived on scene (0 if not yet)
 * @param timeDeparted          Simulation clock when the vehicle left the scene (0 if not yet)
 * @param timeHandover          Simulation clock when the handover was completed (0 if not yet)
 */
public record MissionTelemetryPayload(
        String  state,
        String  severityCode,
        String  confirmedSeverityCode,
        String  pathology,
        String  patientId,
        String  hospitalId,
        boolean clinicalDeteriorated,
        double  timeCalled,
        double  timeOnScene,
        double  timeDeparted,
        double  timeHandover
) {
    /** Convenience factory for the initial dispatch event (no hospital or scene times yet). */
    public static MissionTelemetryPayload dispatched(
            String severityCode,
            String patientId,
            double timeCalled) {

        return new MissionTelemetryPayload(
                it.ausl.emergency.utils.MissionKeywords.STATE_DISPATCHED,
                severityCode,
                it.ausl.emergency.utils.PatientKeywords.SEVERITY_WHITE, // not yet confirmed
                it.ausl.emergency.utils.PatientKeywords.PATHOLOGY_NONE,
                patientId,
                "null",
                false,
                timeCalled,
                0.0,
                0.0,
                0.0
        );
    }
}