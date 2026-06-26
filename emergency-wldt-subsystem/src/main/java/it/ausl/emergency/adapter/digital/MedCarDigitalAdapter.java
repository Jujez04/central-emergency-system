package it.ausl.emergency.adapter.digital;

import it.ausl.emergency.adapter.configuration.MedCarAdapterConfiguration;
import it.ausl.emergency.utils.MedCarKeywords;
import it.wldt.adapter.digital.DigitalAdapter;
import it.wldt.core.state.*;
import it.wldt.exception.EventBusException;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Digital Adapter della MedCar.
 *
 * Espone lo stato operativo del mezzo verso l'esterno (dashboard, test,
 * servizi).
 * Segue lo stesso pattern del PatientDigitalAdapter:
 * - ConcurrentHashMap come snapshot leggibile dall'esterno
 * - CountDownLatch per la sincronizzazione con i test JUnit
 * - Contatori per ogni Domain Event ricevuto
 *
 * La MedCar non accetta azioni digitali: onDigitalActionEvent è no-op.
 */
public class MedCarDigitalAdapter extends DigitalAdapter<MedCarAdapterConfiguration> {

    private final ConcurrentHashMap<String, Object> propertySnapshot = new ConcurrentHashMap<>();
    private final CountDownLatch syncLatch = new CountDownLatch(1);

    // Contatori dei Domain Events — utili per le assert nei test
    private volatile int missionAssignedCount = 0;
    private volatile int onSceneTreatingCount = 0;
    private volatile int missionCompletedCount = 0;
    private volatile int criticalFuelCount = 0;
    private volatile int maintenanceRequiredCount = 0;

    public MedCarDigitalAdapter(String id, MedCarAdapterConfiguration configuration) {
        super(id, configuration);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onAdapterStart() {
    }

    @Override
    public void onAdapterStop() {
    }

    // ── DT Life Cycle callbacks ───────────────────────────────────────────────

    @Override
    public void onDigitalTwinCreate() {
    }

    @Override
    public void onDigitalTwinStart() {
    }

    @Override
    public void onDigitalTwinSync(DigitalTwinState currentState) {
        refreshSnapshot(currentState);

        // Iscrizione a tutti gli eventi dichiarati nella DT State
        try {
            currentState.getEventList()
                    .map(list -> list.stream()
                            .map(DigitalTwinStateEvent::getKey)
                            .collect(Collectors.toList()))
                    .ifPresent(keys -> {
                        try {
                            observeDigitalTwinEventsNotifications(keys);
                            System.out.println("[MedCarDigitalAdapter] -> Observing events: " + keys);
                        } catch (EventBusException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Segnala ai test che il DT è pronto
        syncLatch.countDown();
    }

    @Override
    public void onDigitalTwinUnSync(DigitalTwinState currentState) {
        System.out.println("[MedCarDigitalAdapter] -> Warning: MedCar Twin desynchronized.");
    }

    @Override
    public void onDigitalTwinStop() {
        System.out.println("[MedCarDigitalAdapter] -> Monitoring suspended.");
    }

    @Override
    public void onDigitalTwinDestroy() {
        System.out.println("[MedCarDigitalAdapter] -> Twin destroyed.");
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

        if (MedCarKeywords.CRITICAL_FUEL_EVENT_KEY.equals(eventKey)) {
            criticalFuelCount++;

        } else if (MedCarKeywords.MAINTENANCE_REQUIRED_EVENT_KEY.equals(eventKey)) {
            maintenanceRequiredCount++;
        }
    }

    // ── API ───────────────────────────────────────────────

    /** Valore corrente di una proprietà del DT State. */
    public Optional<Object> getProperty(String key) {
        return Optional.ofNullable(propertySnapshot.get(key));
    }

    public CountDownLatch getSyncLatch() {
        return syncLatch;
    }

    public int getMissionAssignedCount() {
        return missionAssignedCount;
    }

    public int getOnSceneTreatingCount() {
        return onSceneTreatingCount;
    }

    public int getMissionCompletedCount() {
        return missionCompletedCount;
    }

    public int getCriticalFuelCount() {
        return criticalFuelCount;
    }

    public int getMaintenanceRequiredCount() {
        return maintenanceRequiredCount;
    }

    // ── Helper ────────────────────────────────────────────────────────

    private void refreshSnapshot(DigitalTwinState state) {
        if (state == null)
            return;
        try {
            state.getPropertyList().ifPresent(props -> props.forEach(p -> {
                if (p.getValue() != null)
                    propertySnapshot.put(p.getKey(), p.getValue());
            }));
        } catch (Exception e) {
            System.err.println("[MedCarDigitalAdapter] Snapshot refresh error: " + e.getMessage());
        }
    }
}