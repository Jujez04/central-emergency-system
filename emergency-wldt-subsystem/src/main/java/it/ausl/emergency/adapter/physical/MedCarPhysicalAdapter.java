package it.ausl.emergency.adapter.physical;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ausl.emergency.adapter.configuration.MedCarAdapterConfiguration;
import it.ausl.emergency.payload.MedCarTelemetryPayload;
import it.ausl.emergency.utils.MedCarKeywords;
import it.wldt.adapter.physical.ConfigurablePhysicalAdapter;
import it.wldt.adapter.physical.PhysicalAssetDescription;
import it.wldt.adapter.physical.PhysicalAssetEvent;
import it.wldt.adapter.physical.PhysicalAssetProperty;
import it.wldt.adapter.physical.event.PhysicalAssetActionWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetEventWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.exception.EventBusException;

public class MedCarPhysicalAdapter extends ConfigurablePhysicalAdapter<MedCarAdapterConfiguration> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MedCarTelemetryPayload lastTelemetry = null;

    public MedCarPhysicalAdapter(String id, MedCarAdapterConfiguration configuration) {
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

        // Properties (specchio 1:1 dei campi di MedCarTelemetryPayload)
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedCarKeywords.STATE_PROPERTY_KEY,            getConfiguration().getDefaultState()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedCarKeywords.LATITUDE_PROPERTY_KEY,         getConfiguration().getDefaultLatitude()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedCarKeywords.LONGITUDE_PROPERTY_KEY,        getConfiguration().getDefaultLongitude()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedCarKeywords.PATIENT_ID_PROPERTY_KEY,       getConfiguration().getDefaultPatientId()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedCarKeywords.HOME_BASE_ID_PROPERTY_KEY,     getConfiguration().getDefaultHomeBaseId()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedCarKeywords.FUEL_LEVEL_PROPERTY_KEY,       getConfiguration().getDefaultFuelLevel()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedCarKeywords.MISSIONS_PROPERTY_KEY,         getConfiguration().getDefaultMissionsSinceMaintenance()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedCarKeywords.NEEDS_REFUELING_PROPERTY_KEY,  getConfiguration().isDefaultNeedsRefueling()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedCarKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY, getConfiguration().isDefaultNeedsMaintenance()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedCarKeywords.TIMESTAMP_PROPERTY_KEY,        getConfiguration().getDefaultTimestamp()));
        pad.getProperties().add(new PhysicalAssetProperty<>(
                MedCarKeywords.TRIP_DISTANCE_PROPERTY_KEY,    getConfiguration().getDefaultTripDistanceSinceEmergency()));
        pad.getEvents().add(new PhysicalAssetEvent(
                MedCarKeywords.CRITICAL_FUEL_EVENT_KEY,        "application/json"));
        pad.getEvents().add(new PhysicalAssetEvent(
                MedCarKeywords.MAINTENANCE_REQUIRED_EVENT_KEY, "application/json"));
        return pad;
    }

    // ── Telemetry Ingestion ───────────────────────────────────────────────────

    public void onMedCarJsonTelemetryReceived(String jsonPayload) {
        try {
            MedCarTelemetryPayload payload = objectMapper.readValue(jsonPayload, MedCarTelemetryPayload.class);
            onMedCarTelemetryReceived(payload);
        } catch (Exception e) {
            System.err.println("JSON parsing error: " + e.getMessage());
        }
    }

    public void onMedCarTelemetryReceived(MedCarTelemetryPayload payload) {
        if (payload == null) return;

        try {
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedCarKeywords.STATE_PROPERTY_KEY,            payload.state()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedCarKeywords.LATITUDE_PROPERTY_KEY,         payload.lat()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedCarKeywords.LONGITUDE_PROPERTY_KEY,        payload.lon()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedCarKeywords.PATIENT_ID_PROPERTY_KEY,
                    payload.patientId() != null ? payload.patientId() : "null"));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedCarKeywords.HOME_BASE_ID_PROPERTY_KEY,
                    payload.homeBaseId() != null ? payload.homeBaseId() : "null"));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedCarKeywords.FUEL_LEVEL_PROPERTY_KEY,       payload.fuelLevel()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedCarKeywords.MISSIONS_PROPERTY_KEY,         payload.missionsSinceMaintenance()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedCarKeywords.NEEDS_REFUELING_PROPERTY_KEY,  payload.needsRefueling()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedCarKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY, payload.needsMaintenance()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedCarKeywords.TIMESTAMP_PROPERTY_KEY,        payload.timestamp()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    MedCarKeywords.TRIP_DISTANCE_PROPERTY_KEY,    payload.tripDistanceSinceEmergency()));

            if (payload.fuelLevel() < MedCarKeywords.CRITICAL_FUEL_THRESHOLD
                    && (lastTelemetry == null
                        || lastTelemetry.fuelLevel() >= MedCarKeywords.CRITICAL_FUEL_THRESHOLD)) {
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(
                        MedCarKeywords.CRITICAL_FUEL_EVENT_KEY, payload));
            }

            if (payload.needsMaintenance()
                    && (lastTelemetry == null || !lastTelemetry.needsMaintenance())) {
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(
                        MedCarKeywords.MAINTENANCE_REQUIRED_EVENT_KEY, payload));
            }

            lastTelemetry = payload;

        } catch (EventBusException e) {
            System.err.println("[MedCarPhysicalAdapter] EventBus error: " + e.getMessage());
        }
    }

    // ── Action (no-op) ────────────────────────────────────────────────────────

    @Override
    public void onIncomingPhysicalAction(PhysicalAssetActionWldtEvent<?> event) {
    }
}