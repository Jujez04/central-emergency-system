package it.ausl.emergency.adapter.digital;

import it.ausl.emergency.adapter.configuration.MedHelicopterAdapterConfiguration;
import it.ausl.emergency.payload.MedHelicopterTelemetryPayload;
import it.ausl.emergency.utils.MedHelicopterKeywords;
import it.wldt.adapter.digital.DigitalAdapter;
import it.wldt.core.state.*;
import it.wldt.exception.EventBusException;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Digital Adapter del MedHelicopter.
 *
 * Espone lo stato operativo dell'elisoccorso verso l'esterno
 * (dashboard, test, servizi REST).
 *
 * Segue lo stesso pattern di MedCarDigitalAdapter:
 * - ConcurrentHashMap come snapshot leggibile dall'esterno
 * - CountDownLatch per la sincronizzazione con i test JUnit
 * - Contatori per ogni Domain Event ricevuto
 *
 * Il MedHelicopter non accetta azioni digitali: onDigitalActionEvent è no-op.
 */
public class MedHelicopterDigitalAdapter extends DigitalAdapter<MedHelicopterAdapterConfiguration> {

    private final ConcurrentHashMap<String, Object> propertySnapshot = new ConcurrentHashMap<>();
    private final CountDownLatch syncLatch = new CountDownLatch(1);

    private volatile int missionAssignedCount = 0;
    private volatile int patientOnboardCount = 0;
    private volatile int hospitalHandoverCount = 0;
    private volatile int criticalFuelCount = 0;
    private volatile int maintenanceRequiredCount = 0;

    public MedHelicopterDigitalAdapter(String id, MedHelicopterAdapterConfiguration configuration) {
        super(id, configuration);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onAdapterStart() {
    }

    @Override
    public void onAdapterStop() {
    }

    // ── DT Life Cycle Callbacks ───────────────────────────────────────────────

    @Override
    public void onDigitalTwinCreate() {
    }

    @Override
    public void onDigitalTwinStart() {
    }

    @Override
    public void onDigitalTwinSync(DigitalTwinState currentState) {
        refreshSnapshot(currentState);

        try {
            currentState.getEventList()
                    .map(list -> list.stream()
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
        syncLatch.countDown();
    }

    @Override
    public void onDigitalTwinUnSync(DigitalTwinState currentState) {
    }

    @Override
    public void onDigitalTwinStop() {
    }

    @Override
    public void onDigitalTwinDestroy() {
    }

    // ── State Update ──────────────────────────────────────────────────────────

    @Override
    protected void onStateUpdate(DigitalTwinState newState,
            DigitalTwinState previousState,
            ArrayList<DigitalTwinStateChange> changes) {

        if (changes != null && !changes.isEmpty()) {
            changes.forEach(c -> System.out.printf("  [%s] %s -> %s%n",
                    c.getOperation(), c.getResourceType(), c.getResource()));
        }

        refreshSnapshot(newState);
    }

    // ── Domain Event Callbacks ────────────────────────────────────────────────

    @Override
    protected void onEventNotificationReceived(DigitalTwinStateEventNotification<?> notification) {
        if (notification == null)
            return;

        String eventKey = notification.getDigitalEventKey();

        if (MedHelicopterKeywords.CRITICAL_FUEL_EVENT_KEY.equals(eventKey)) {
            criticalFuelCount++;
        } else if (MedHelicopterKeywords.MAINTENANCE_REQUIRED_EVENT_KEY.equals(eventKey)) {
            maintenanceRequiredCount++;
        } else {
            System.out.println("  (unhandled event: " + eventKey + ")");
        }
    }

    // ── API ───────────────────────────────────────────────

    /** Valore corrente di una proprietà del DT State. */
    public Optional<Object> getProperty(String key) {
        return Optional.ofNullable(propertySnapshot.get(key));
    }

    /** Latch sbloccato quando il DT è in stato Shadowed. */
    public CountDownLatch getSyncLatch() {
        return syncLatch;
    }

    public int getMissionAssignedCount() {
        return missionAssignedCount;
    }

    public int getPatientOnboardCount() {
        return patientOnboardCount;
    }

    public int getHospitalHandoverCount() {
        return hospitalHandoverCount;
    }

    public int getCriticalFuelCount() {
        return criticalFuelCount;
    }

    public int getMaintenanceRequiredCount() {
        return maintenanceRequiredCount;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private void refreshSnapshot(DigitalTwinState state) {
        if (state == null)
            return;
        try {
            state.getPropertyList().ifPresent(props -> props.forEach(p -> {
                if (p.getValue() != null)
                    propertySnapshot.put(p.getKey(), p.getValue());
            }));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void printStateSnapshot(String title, DigitalTwinState state) {
        System.out.println("\n[MedHelicopterDigitalAdapter] ── " + title + " ──");
        if (state == null)
            return;
        try {
            state.getPropertyList().ifPresent(props -> props.forEach(p -> System.out.printf(
                    "  [PROP] %-55s = %s%n", p.getKey(), p.getValue())));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    private void printOperationalSnapshot(DigitalTwinState state) {
        if (state == null)
            return;
        String[] keys = {
                MedHelicopterKeywords.STATE_PROPERTY_KEY,
                MedHelicopterKeywords.PATIENT_ID_PROPERTY_KEY,
                MedHelicopterKeywords.HOSPITAL_ID_PROPERTY_KEY,
                MedHelicopterKeywords.HOME_BASE_PROPERTY_KEY,
                MedHelicopterKeywords.FUEL_LEVEL_PROPERTY_KEY,
                MedHelicopterKeywords.MISSIONS_PROPERTY_KEY,
                MedHelicopterKeywords.NEEDS_REFUELING_PROPERTY_KEY,
                MedHelicopterKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY,
                MedHelicopterKeywords.TRIP_DISTANCE_PROPERTY_KEY
        };
        System.out.println("  [Operational Snapshot]");
        for (String key : keys) {
            try {
                state.getProperty(key).ifPresent(p -> System.out.printf("    %-55s = %s%n", p.getKey(), p.getValue()));
            } catch (Exception ignored) {
            }
        }
    }

    private void printPayloadSummary(Object body) {
        if (body instanceof MedHelicopterTelemetryPayload p) {
            System.out.printf("    State: %-20s | Patient: %-15s | Hospital: %s%n",
                    p.state(), p.patientId(), p.hospitalId());
            System.out.printf("    Fuel: %.2f | Missions: %d | TripDist: %.1f m%n",
                    p.fuelLevel(), p.missionsSinceMaintenance(), p.tripDistanceSinceEmergency());
        } else {
            System.out.println("    Body: " + body);
        }
    }
}
