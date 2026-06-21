package it.ausl.emergency.adapter.physical;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ausl.emergency.model.payload.AmbulanceTelemetryPayload;
import it.ausl.emergency.utils.AmbulanceKeywords;
import it.wldt.adapter.physical.PhysicalAdapter;
import it.wldt.adapter.physical.PhysicalAssetDescription;
import it.wldt.adapter.physical.PhysicalAssetEvent;
import it.wldt.adapter.physical.PhysicalAssetProperty;
import it.wldt.adapter.physical.event.PhysicalAssetActionWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetEventWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.exception.EventBusException;

/**
 * Physical Adapter dell'Ambulanza.
 *
 * A differenza del paziente, le ambulanze esistono dall'avvio della simulazione
 * e rimangono attive per tutta la sua durata: la PAD viene pubblicata una volta
 * sola in onAdapterStart() con i valori iniziali di riposo, e i successivi
 * messaggi MQTT aggiornano le proprietà tramite onAmbulanceTelemetryReceived().
 *
 * Domain Events rilevati tramite fronti di transizione:
 *   MISSION_ASSIGNED     → atRest → MovingToPatient
 *   PATIENT_ONBOARD      → TakingPatient → Supporting | MovingToHospital
 *   HOSPITAL_HANDOVER    → qualsiasi → Handover
 *   REFUELING_NEEDED     → needsRefueling false → true
 *   MAINTENANCE_NEEDED   → needsMaintenance false → true
 */
public class AmbulancePhysicalAdapter extends PhysicalAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AmbulanceTelemetryPayload lastTelemetry = null;

    public AmbulancePhysicalAdapter(String id) {
        super(id);
    }

    @Override
    public void onIncomingPhysicalAction(PhysicalAssetActionWldtEvent<?> physicalActionEvent) {
        System.out.println("[AmbulancePhysicalAdapter] -> Azione non supportata: "
                + (physicalActionEvent != null ? physicalActionEvent.getActionKey() : "null"));
    }

    @Override
    public void onAdapterStart() {
        try {
            System.out.println("[AmbulancePhysicalAdapter] -> Pubblicazione PAD...");
            notifyPhysicalAdapterBound(buildPhysicalAssetDescription());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAdapterStop() {
        System.out.println("[AmbulancePhysicalAdapter] -> Adapter fermato.");
    }

    // ── PAD ───────────────────────────────────────────────────────────────────

    private PhysicalAssetDescription buildPhysicalAssetDescription() {
        PhysicalAssetDescription pad = new PhysicalAssetDescription();

        // Proprietà operative — valori iniziali allineati allo stato atRest
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.STATE_PROPERTY_KEY,
                AmbulanceKeywords.STATE_AT_REST));
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.LATITUDE_PROPERTY_KEY,
                0.0));
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.LONGITUDE_PROPERTY_KEY,
                0.0));
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.PATIENT_ID_PROPERTY_KEY,
                AmbulanceKeywords.NULL_REFERENCE));
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.HOSPITAL_ID_PROPERTY_KEY,
                AmbulanceKeywords.NULL_REFERENCE));
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.HOME_BASE_ID_PROPERTY_KEY,
                AmbulanceKeywords.NULL_REFERENCE));
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.FUEL_LEVEL_PROPERTY_KEY,
                1.0));
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.MISSIONS_SINCE_MAINTENANCE_PROPERTY_KEY,
                0));
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.NEEDS_REFUELING_PROPERTY_KEY,
                false));
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY,
                false));
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.TIMESTAMP_PROPERTY_KEY,
                0.0));
        pad.getProperties().add(new PhysicalAssetProperty<>(AmbulanceKeywords.TRIP_DISTANCE_PROPERTY_KEY,
                0.0));

        // Domain Events
        pad.getEvents().add(new PhysicalAssetEvent(AmbulanceKeywords.MISSION_ASSIGNED_EVENT_KEY,   "application/json"));
        pad.getEvents().add(new PhysicalAssetEvent(AmbulanceKeywords.PATIENT_ONBOARD_EVENT_KEY,    "application/json"));
        pad.getEvents().add(new PhysicalAssetEvent(AmbulanceKeywords.HOSPITAL_HANDOVER_EVENT_KEY,  "application/json"));
        pad.getEvents().add(new PhysicalAssetEvent(AmbulanceKeywords.REFUELING_NEEDED_EVENT_KEY,   "application/json"));
        pad.getEvents().add(new PhysicalAssetEvent(AmbulanceKeywords.MAINTENANCE_NEEDED_EVENT_KEY, "application/json"));

        return pad;
    }

    // ── Ingestion ─────────────────────────────────────────────────────────────

    /**
     * Entry point per la telemetria già deserializzata.
     * Chiamato da AmbulanceMqttIngestionAdapter (o direttamente nei test).
     */
    public void onAmbulanceTelemetryReceived(AmbulanceTelemetryPayload payload) {
        if (payload == null) return;

        try {
            // ── Aggiornamento proprietà ──────────────────────────────────────
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.STATE_PROPERTY_KEY, payload.state()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.LATITUDE_PROPERTY_KEY, payload.lat()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.LONGITUDE_PROPERTY_KEY, payload.lon()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.PATIENT_ID_PROPERTY_KEY,
                    payload.patientId() != null ? payload.patientId() : AmbulanceKeywords.NULL_REFERENCE));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.HOSPITAL_ID_PROPERTY_KEY,
                    payload.hospitalId() != null ? payload.hospitalId() : AmbulanceKeywords.NULL_REFERENCE));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.HOME_BASE_ID_PROPERTY_KEY,
                    payload.homeBaseId() != null ? payload.homeBaseId() : AmbulanceKeywords.NULL_REFERENCE));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.FUEL_LEVEL_PROPERTY_KEY, payload.fuelLevel()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.MISSIONS_SINCE_MAINTENANCE_PROPERTY_KEY, payload.missionsSinceMaintenance()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.NEEDS_REFUELING_PROPERTY_KEY, payload.needsRefueling()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY, payload.needsMaintenance()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.TIMESTAMP_PROPERTY_KEY, payload.timestamp()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    AmbulanceKeywords.TRIP_DISTANCE_PROPERTY_KEY, payload.tripDistanceSinceEmergency()));

            // ── Domain Event 1 — MISSION_ASSIGNED: atRest → MovingToPatient ──
            boolean isNowMovingToPatient = AmbulanceKeywords.STATE_MOVING_TO_PATIENT
                    .equalsIgnoreCase(payload.state());
            boolean wasAtRest = lastTelemetry == null
                    || AmbulanceKeywords.STATE_AT_REST.equalsIgnoreCase(lastTelemetry.state())
                    || AmbulanceKeywords.STATE_RETURNING.equalsIgnoreCase(lastTelemetry.state());
            if (isNowMovingToPatient && wasAtRest) {
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(
                        AmbulanceKeywords.MISSION_ASSIGNED_EVENT_KEY, payload));
            }

            // ── Domain Event 2 — PATIENT_ONBOARD: TakingPatient → Supporting/MovingToHospital ──
            boolean wasBeingTaken = lastTelemetry != null
                    && AmbulanceKeywords.STATE_TAKING_PATIENT.equalsIgnoreCase(lastTelemetry.state());
            boolean isNowTransporting =
                    AmbulanceKeywords.STATE_SUPPORTING.equalsIgnoreCase(payload.state())
                    || AmbulanceKeywords.STATE_MOVING_TO_HOSPITAL.equalsIgnoreCase(payload.state());
            if (wasBeingTaken && isNowTransporting) {
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(
                        AmbulanceKeywords.PATIENT_ONBOARD_EVENT_KEY, payload));
            }

            // ── Domain Event 3 — HOSPITAL_HANDOVER: → Handover ──────────────
            boolean isNowHandover = AmbulanceKeywords.STATE_HANDOVER.equalsIgnoreCase(payload.state());
            boolean wasHandover   = lastTelemetry != null
                    && AmbulanceKeywords.STATE_HANDOVER.equalsIgnoreCase(lastTelemetry.state());
            if (isNowHandover && !wasHandover) {
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(
                        AmbulanceKeywords.HOSPITAL_HANDOVER_EVENT_KEY, payload));
            }

            // ── Domain Event 4 — REFUELING_NEEDED: fronte false→true ─────────
            if (payload.needsRefueling()
                    && (lastTelemetry == null || !lastTelemetry.needsRefueling())) {
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(
                        AmbulanceKeywords.REFUELING_NEEDED_EVENT_KEY, payload));
            }

            // ── Domain Event 5 — MAINTENANCE_NEEDED: fronte false→true ───────
            if (payload.needsMaintenance()
                    && (lastTelemetry == null || !lastTelemetry.needsMaintenance())) {
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(
                        AmbulanceKeywords.MAINTENANCE_NEEDED_EVENT_KEY, payload));
            }

            lastTelemetry = payload;

        } catch (EventBusException e) {
            System.err.println("[AmbulancePhysicalAdapter] Event Bus error: " + e.getMessage());
        }
    }

    /**
     * Entry point per la telemetria grezza in formato JSON (usato nei test
     * e dall'ingestion adapter MQTT).
     */
    public void onAmbulanceJsonTelemetryReceived(String jsonPayload) {
        try {
            AmbulanceTelemetryPayload payload = objectMapper.readValue(
                    jsonPayload, AmbulanceTelemetryPayload.class);
            onAmbulanceTelemetryReceived(payload);
        } catch (Exception e) {
            System.err.println("[AmbulancePhysicalAdapter] Errore deserializzazione JSON: "
                    + e.getMessage());
        }
    }
}