package it.ausl.emergency.adapter.physical;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ausl.emergency.adapter.configuration.MedHelicopterAdapterConfiguration;
import it.ausl.emergency.payload.MedHelicopterTelemetryPayload;
import it.ausl.emergency.utils.MedHelicopterKeywords;
import it.wldt.adapter.physical.ConfigurablePhysicalAdapter;
import it.wldt.adapter.physical.PhysicalAssetDescription;
import it.wldt.adapter.physical.PhysicalAssetEvent;
import it.wldt.adapter.physical.PhysicalAssetProperty;
import it.wldt.adapter.physical.event.PhysicalAssetActionWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetEventWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.exception.EventBusException;

public class MedHelicopterPhysicalAdapter
        extends ConfigurablePhysicalAdapter<MedHelicopterAdapterConfiguration> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MedHelicopterTelemetryPayload lastTelemetry = null;

    public MedHelicopterPhysicalAdapter(String id, MedHelicopterAdapterConfiguration configuration) {
        super(id, configuration);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onAdapterStart() {
        try {
            notifyPhysicalAdapterBound(buildPhysicalAssetDescription());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAdapterStop() {
    }

    // ── PAD Builder ───────────────────────────────────────────────────────────

    private PhysicalAssetDescription buildPhysicalAssetDescription() {
        PhysicalAssetDescription pad = new PhysicalAssetDescription();
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.STATE_PROPERTY_KEY,
                getConfiguration().getDefaultState()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.LATITUDE_PROPERTY_KEY,
                getConfiguration().getDefaultLatitude()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.LONGITUDE_PROPERTY_KEY,
                getConfiguration().getDefaultLongitude()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.PATIENT_ID_PROPERTY_KEY,
                getConfiguration().getDefaultPatientId()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.HOSPITAL_ID_PROPERTY_KEY,
                getConfiguration().getDefaultHospitalId()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.HOME_BASE_PROPERTY_KEY,
                getConfiguration().getDefaultHomeBase()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.FUEL_LEVEL_PROPERTY_KEY,
                getConfiguration().getDefaultFuelLevel()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.MISSIONS_PROPERTY_KEY,
                getConfiguration().getDefaultMissionsSinceMaintenance()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.NEEDS_REFUELING_PROPERTY_KEY,
                getConfiguration().isDefaultNeedsRefueling()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY,
                getConfiguration().isDefaultNeedsMaintenance()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.TIMESTAMP_PROPERTY_KEY,
                getConfiguration().getDefaultTimestamp()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedHelicopterKeywords.TRIP_DISTANCE_PROPERTY_KEY,
                getConfiguration().getDefaultTripDistanceSinceEmergency()));
        pad.getEvents().add(new PhysicalAssetEvent(
                MedHelicopterKeywords.CRITICAL_FUEL_EVENT_KEY,        "application/json"));
        pad.getEvents().add(new PhysicalAssetEvent(
                MedHelicopterKeywords.MAINTENANCE_REQUIRED_EVENT_KEY, "application/json"));
        return pad;
    }

    /**
     * Entry point per payload JSON grezzi (es. da un client MQTT reale).
     */
    public void onMedHelicopterJsonTelemetryReceived(String jsonPayload) {
        try {
            MedHelicopterTelemetryPayload payload =
                    objectMapper.readValue(jsonPayload, MedHelicopterTelemetryPayload.class);
            onMedHelicopterTelemetryReceived(payload);
        } catch (Exception e) {
            System.err.println("[MedHelicopterPhysicalAdapter] JSON parsing error: " + e.getMessage());
        }
    }

    public void onMedHelicopterTelemetryReceived(MedHelicopterTelemetryPayload payload) {
        if (payload == null) return;

        try {
            // ── 1. Aggiornamento delle proprietà ──────────────────────────────
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedHelicopterKeywords.STATE_PROPERTY_KEY,
                    payload.state()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedHelicopterKeywords.LATITUDE_PROPERTY_KEY,
                    payload.lat()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedHelicopterKeywords.LONGITUDE_PROPERTY_KEY,
                    payload.lon()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedHelicopterKeywords.PATIENT_ID_PROPERTY_KEY,
                    payload.patientId() != null ? payload.patientId() : "null"));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedHelicopterKeywords.HOSPITAL_ID_PROPERTY_KEY,
                    payload.hospitalId() != null ? payload.hospitalId() : "null"));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedHelicopterKeywords.FUEL_LEVEL_PROPERTY_KEY,
                    payload.fuelLevel()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedHelicopterKeywords.MISSIONS_PROPERTY_KEY,
                    payload.missionsSinceMaintenance()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedHelicopterKeywords.NEEDS_REFUELING_PROPERTY_KEY,
                    payload.needsRefueling()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedHelicopterKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY,
                    payload.needsMaintenance()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedHelicopterKeywords.TIMESTAMP_PROPERTY_KEY,
                    payload.timestamp()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedHelicopterKeywords.TRIP_DISTANCE_PROPERTY_KEY,
                    payload.tripDistanceSinceEmergency()));
            if (payload.fuelLevel() < MedHelicopterKeywords.CRITICAL_FUEL_THRESHOLD
                    && (lastTelemetry == null
                        || lastTelemetry.fuelLevel() >= MedHelicopterKeywords.CRITICAL_FUEL_THRESHOLD)) {
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(
                        MedHelicopterKeywords.CRITICAL_FUEL_EVENT_KEY, payload));
            }

            if (payload.needsMaintenance()
                    && (lastTelemetry == null || !lastTelemetry.needsMaintenance())) {
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(
                        MedHelicopterKeywords.MAINTENANCE_REQUIRED_EVENT_KEY, payload));
            }

            lastTelemetry = payload;

        } catch (EventBusException e) {
            System.err.println("[MedHelicopterPhysicalAdapter] EventBus error: " + e.getMessage());
        }
    }

    @Override
    public void onIncomingPhysicalAction(PhysicalAssetActionWldtEvent<?> event) {
        // Il MedHelicopter non espone azioni
        System.out.println("[MedHelicopterPhysicalAdapter] -> Unsupported action received: "
                + (event != null ? event.getActionKey() : "null"));
    }
}
