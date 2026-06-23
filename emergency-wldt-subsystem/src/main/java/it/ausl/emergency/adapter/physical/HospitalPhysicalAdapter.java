package it.ausl.emergency.adapter.physical;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ausl.emergency.adapter.configuration.HospitalAdapterConfiguration;
import it.ausl.emergency.payload.HospitalTelemetryPayload;
import it.ausl.emergency.utils.HospitalKeywords;
import it.wldt.adapter.physical.ConfigurablePhysicalAdapter;
import it.wldt.adapter.physical.PhysicalAssetDescription;
import it.wldt.adapter.physical.PhysicalAssetProperty;
import it.wldt.adapter.physical.event.PhysicalAssetActionWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.exception.EventBusException;

/**
 * Hospital Configurable Physical Adapter.
 * Ingests external real-time structural data from simulated clinical points
 * and translates telemetry metrics to the transactional digital core state
 * layers.
 */
public class HospitalPhysicalAdapter extends ConfigurablePhysicalAdapter<HospitalAdapterConfiguration> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public HospitalPhysicalAdapter(String id, HospitalAdapterConfiguration configuration) {
        super(id, configuration);
    }

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

    private PhysicalAssetDescription buildPhysicalAssetDescription() {
        PhysicalAssetDescription pad = new PhysicalAssetDescription();

        // 1. Map Configuration Properties to Physical Asset Description (PAD)
        pad.getProperties().add(new PhysicalAssetProperty<>(HospitalKeywords.ASSISTANCE_LEVEL_PROPERTY_KEY,
                getConfiguration().getDefaultAssistanceLevel()));
        pad.getProperties().add(new PhysicalAssetProperty<>(HospitalKeywords.PATIENT_ASSISTED_PROPERTY_KEY,
                getConfiguration().getDefaultPatientAssisted()));
        pad.getProperties().add(new PhysicalAssetProperty<>(HospitalKeywords.TIMESTAMP_PROPERTY_KEY,
                getConfiguration().getDefaultTimestamp()));
        return pad;
    }

    public void onHospitalJsonTelemetryReceived(String jsonPayload) {
        try {
            HospitalTelemetryPayload payload = objectMapper.readValue(jsonPayload, HospitalTelemetryPayload.class);
            this.onHospitalTelemetryReceived(payload);
        } catch (Exception e) {
            System.err.println("[HospitalPhysicalAdapter] JSON structural deserialization parsing error: "
                    + e.getMessage());
        }
    }

    public void onHospitalTelemetryReceived(HospitalTelemetryPayload payload) {
        if (payload == null) {
            return;
        }

        try {
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    HospitalKeywords.ASSISTANCE_LEVEL_PROPERTY_KEY, payload.assistanceLevel()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    HospitalKeywords.PATIENT_ASSISTED_PROPERTY_KEY, payload.patientAssisted()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                    HospitalKeywords.TIMESTAMP_PROPERTY_KEY, payload.timestamp()));

        } catch (EventBusException e) {
            System.err.println("Processing failure on framework internal event bus context: " + e.getMessage());
        }
    }

    @Override
    public void onIncomingPhysicalAction(PhysicalAssetActionWldtEvent<?> physicalActionEvent) {
    }
}