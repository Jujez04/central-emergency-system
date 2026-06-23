package it.ausl.emergency.adapter.digital;

import it.ausl.emergency.adapter.configuration.PatientAdapterConfiguration;
import it.ausl.emergency.payload.PatientTelemetryPayload;
import it.ausl.emergency.utils.PatientKeywords;
import it.wldt.adapter.digital.DigitalAdapter;
import it.wldt.core.state.*;
import it.wldt.exception.EventBusException;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Patient Digital Adapter.
 * Exposes the Digital Twin internal state boundaries to external monitoring systems
 * by asynchronously processing transactional state updates and core Domain Events.
 */
public class PatientDigitalAdapter extends DigitalAdapter<PatientAdapterConfiguration> {

    public PatientDigitalAdapter(String id, PatientAdapterConfiguration configuration) {
        super(id, configuration);
    }

    // ── Lifecycle Callbacks ──────────────────────────────────────────────────

    @Override
    public void onAdapterStart() {
    }

    @Override
    public void onAdapterStop() {
    }

    // ── Digital Twin Engine Lifecycle Callbacks ──────────────────────────────

    @Override
    public void onDigitalTwinCreate() {
    }

    @Override
    public void onDigitalTwinStart() {
    }

    @Override
    public void onDigitalTwinSync(DigitalTwinState currentDigitalTwinState) {

        // Dynamically extract registered event keys and subscribe via the event bus
        try {
            currentDigitalTwinState.getEventList()
                    .map(eventList -> eventList.stream()
                            .map(DigitalTwinStateEvent::getKey)
                            .collect(Collectors.toList()))
                    .ifPresent(keys -> {
                        try {
                            observeDigitalTwinEventsNotifications(keys);
                        } catch (EventBusException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDigitalTwinUnSync(DigitalTwinState currentDigitalTwinState) {
    }

    @Override
    public void onDigitalTwinStop() {
    }

    @Override
    public void onDigitalTwinDestroy() {
    }

    // ── Transactional State Monitoring Update Callbacks ──────────────────────

    @Override
    protected void onStateUpdate(DigitalTwinState newState,
                                 DigitalTwinState previousState,
                                 ArrayList<DigitalTwinStateChange> changes) {
    }

    // ── Domain Event Notification Processing Callbacks ───────────────────────

    @Override
    protected void onEventNotificationReceived(DigitalTwinStateEventNotification<?> notification) {
        if (notification == null) return;

        String eventKey = notification.getDigitalEventKey();
        Object body = notification.getBody();

        switch (eventKey) {
            case PatientKeywords.CLINICAL_ASSESSMENT_EVENT_KEY:
                break;
            case PatientKeywords.CLINICAL_DETERIORATION_EVENT_KEY:
                break;
            case PatientKeywords.HANDOVER_COMPLETED_EVENT_KEY:
                break;
            default:
                System.out.println("  (Unmanaged or experimental external notification key: " + eventKey + ")");
                break;
        }

    }

    // ── Infrastructure Diagnostic Helpers ────────────────────────────────────

    private void printStateSnapshot(String title, DigitalTwinState state) {
        System.out.println("\n[PatientDigitalAdapter] ── " + title + " ──");
        if (state == null) {
            System.out.println("  (Target state pointer is null)");
            return;
        }
        try {
            state.getPropertyList().ifPresent(props ->
                    props.forEach(p -> System.out.printf("  [PROPERTY] %-45s = %s%n", p.getKey(), p.getValue()))
            );
            state.getEventList().ifPresent(events ->
                    events.forEach(e -> System.out.printf("  [REGISTERED-EVENT] %-45s (Type=%s)%n", e.getKey(), e.getType()))
            );
        } catch (Exception e) {
            System.err.println("  (Error logging twin structural topology state attributes: " + e.getMessage() + ")");
        }
        System.out.println();
    }

    private void printClinicalStateSummary(DigitalTwinState state) {
        if (state == null) return;

        String[] strictTrackingKeys = {
                PatientKeywords.STATE_PROPERTY_KEY,
                PatientKeywords.SEVERITY_CODE_PROPERTY_KEY,
                PatientKeywords.CONFIRMED_SEVERITY_CODE_PROPERTY_KEY,
                PatientKeywords.PATHOLOGY_PROPERTY_KEY,
                PatientKeywords.GCS_SCORE_PROPERTY_KEY,
                PatientKeywords.AIRWAY_OBSTRUCTED_PROPERTY_KEY,
                PatientKeywords.EXTERNAL_HEMORRHAGE_PROPERTY_KEY,
                PatientKeywords.CLINICAL_DETERIORATED_PROPERTY_KEY
        };

        System.out.println("  [Clinical Telemetry Snapshot]");
        for (String key : strictTrackingKeys) {
            try {
                state.getProperty(key).ifPresent(p -> 
                        System.out.printf("    %-50s = %s%n", p.getKey(), p.getValue()));
            } catch (Exception ignored) {}
        }
    }

    private void printPayloadBodySummary(Object body) {
        if (body == null) {
            System.out.println("  Payload content: (empty body)");
            return;
        }
        if (body instanceof PatientTelemetryPayload payload) {
            System.out.printf("    State: %-15s | Emergency Severity: %-8s | Validated Severity: %-8s%n",
                    payload.state(), payload.severityCode(), payload.confirmedSeverityCode());
            System.out.printf("    GCS: %-6d | Airway Compromised: %-6b | Hemorrhage Present: %-6b | Deteriorated: %-6b%n",
                    payload.gcsScore(), payload.isAirwayObstructed(), payload.hasExternalHemorrhage(), payload.isClinicalDeteriorated());
            System.out.printf("    GIS Coordinates: (Lat: %.5f, Lon: %.5f) | Simulation Clock: %.1f%n",
                    payload.lat(), payload.lon(), payload.timeCalled());
        } else {
            System.out.println("  Raw String content: " + body);
        }
    }
}