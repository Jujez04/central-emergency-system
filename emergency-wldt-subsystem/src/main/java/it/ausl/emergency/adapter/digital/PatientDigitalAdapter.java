package it.ausl.emergency.adapter.digital;

import it.ausl.emergency.adapter.configuration.PatientAdapterConfiguration;
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

    @Override
    protected void onEventNotificationReceived(DigitalTwinStateEventNotification<?> notification) {
        if (notification == null) return;

        String eventKey = notification.getDigitalEventKey();

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
}