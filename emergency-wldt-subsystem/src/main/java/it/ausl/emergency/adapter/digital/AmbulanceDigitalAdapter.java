package it.ausl.emergency.adapter.digital;

import it.ausl.emergency.adapter.configuration.AmbulanceAdapterConfiguration;
import it.ausl.emergency.payload.AmbulanceTelemetryPayload;
import it.ausl.emergency.utils.AmbulanceKeywords;
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
        if (notification == null)
            return;

        String eventKey = notification.getDigitalEventKey();
        Object body = notification.getBody();
        /* 
        if (AmbulanceKeywords.CRITICAL_FUEL_EVENT_KEY.equals(eventKey)) {
        } else if (AmbulanceKeywords.MAINTENANCE_REQUIRED_EVENT_KEY.equals(eventKey)) {
            System.out.println("  ► WARNING: MISSION COMPLIANCE THRESHOLD EXCEEDED — MAINTENANCE REQUIRED ⚠");
            printPayloadSummary(body);
        } else {
            System.out.println("  (Unmanaged infrastructure alert context: " + eventKey + ")");
        }
        System.out.println("====================================================================\n");
        */
    }

    // ── Diagnostic Helpers ───────────────────────────────────────────────────

    private void printStateSnapshot(String title, DigitalTwinState state) {
        System.out.println("\n[AmbulanceDigitalAdapter] ── " + title + " ──");
        if (state == null)
            return;
        try {
            state.getPropertyList().ifPresent(props -> props
                    .forEach(p -> System.out.printf("  [PROPERTY] %-45s = %s%n", p.getKey(), p.getValue())));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    private void printFleetSnapshot(DigitalTwinState state) {
        if (state == null)
            return;

        String[] logisticsKeys = {
                AmbulanceKeywords.STATE_PROPERTY_KEY,
                AmbulanceKeywords.PATIENT_ID_PROPERTY_KEY,
                AmbulanceKeywords.HOSPITAL_ID_PROPERTY_KEY,
                AmbulanceKeywords.FUEL_LEVEL_PROPERTY_KEY,
                AmbulanceKeywords.MISSIONS_PROPERTY_KEY,
                AmbulanceKeywords.NEEDS_REFUELING_PROPERTY_KEY,
                AmbulanceKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY,
                AmbulanceKeywords.TRIP_DISTANCE_PROPERTY_KEY
        };

        System.out.println("  [Fleet Asset Snapshot]");
        for (String key : logisticsKeys) {
            try {
                state.getProperty(key).ifPresent(p -> System.out.printf("    %-50s = %s%n", p.getKey(), p.getValue()));
            } catch (Exception ignored) {
            }
        }
    }

    private void printPayloadSummary(Object body) {
        if (body instanceof AmbulanceTelemetryPayload p) {
            System.out.printf("    State: %-15s | Patient Bound: %-10s | Hospital Target: %-15s%n",
                    p.state(), p.patientId(), p.hospitalId());
            System.out.printf("    Fuel: %-5.2f | Missions Done: %-4d | Dist. Travelled: %.1f meters%n",
                    p.fuelLevel(), p.missionsSinceMaintenance(), p.tripDistanceSinceEmergency());
        } else {
            System.out.println("    Raw body context: " + body);
        }
    }
}