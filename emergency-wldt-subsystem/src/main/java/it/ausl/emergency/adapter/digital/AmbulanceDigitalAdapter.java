package it.ausl.emergency.adapter.digital;

import it.ausl.emergency.adapter.configuration.AmbulanceAdapterConfiguration;
import it.wldt.adapter.digital.DigitalAdapter;
import it.wldt.core.state.*;
import it.wldt.exception.EventBusException;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Ambulance Digital Adapter.
 * Exposes vehicle fleet logistics, coordinates, and operational updates to the
 * control center dashboard.
 */
public class AmbulanceDigitalAdapter extends DigitalAdapter<AmbulanceAdapterConfiguration> {

    public AmbulanceDigitalAdapter(String id, AmbulanceAdapterConfiguration configuration) {
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

    // ── Transactional State Monitoring Callback ──────────────────────────────

    @Override
    protected void onStateUpdate(DigitalTwinState newState,
            DigitalTwinState previousState,
            ArrayList<DigitalTwinStateChange> changes) {

        if (changes != null && !changes.isEmpty()) {
            changes.forEach(change -> System.out.printf("  [%s] %s -> %s%n",
                    change.getOperation(), change.getResourceType(), change.getResource()));
        }
    }

    // ── Domain Event Notification Callback ───────────────────────────────────

    @Override
    protected void onEventNotificationReceived(DigitalTwinStateEventNotification<?> notification) {
    }
}