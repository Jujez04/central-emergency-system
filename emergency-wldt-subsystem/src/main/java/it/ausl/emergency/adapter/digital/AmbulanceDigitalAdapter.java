package it.ausl.emergency.adapter.digital;

import it.ausl.emergency.model.payload.AmbulanceTelemetryPayload;
import it.ausl.emergency.utils.AmbulanceKeywords;
import it.wldt.adapter.digital.DigitalAdapter;
import it.wldt.core.state.*;
import it.wldt.exception.EventBusException;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Digital Adapter dell'Ambulanza.
 *
 * Espone lo stato operativo del Digital Twin verso l'esterno reagendo ai
 * cambiamenti di stato e alle notifiche dei Domain Events:
 *   - Missione Assegnata       (atRest → MovingToPatient)
 *   - Paziente Preso in Carico (TakingPatient → Supporting/MovingToHospital)
 *   - Handover Ospedale        (→ Handover)
 *   - Rifornimento Necessario  (needsRefueling false→true)
 *   - Manutenzione Necessaria  (needsMaintenance false→true)
 *
 * In un sistema reale questo adapter potrebbe:
 *   - pubblicare su "ces/dt/ambulance/{id}/state"
 *   - aggiornare una dashboard operativa in tempo reale
 *   - notificare la Centrale Operativa di disponibilità / indisponibilità
 *
 * L'ambulanza non accetta azioni digitali in ingresso: il flusso è
 * esclusivamente simulazione → Digital Twin → esterno.
 */
public class AmbulanceDigitalAdapter extends DigitalAdapter<Void> {

    public AmbulanceDigitalAdapter(String id) {
        super(id, null);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onAdapterStart() {
        System.out.println("[AmbulanceDigitalAdapter] -> onAdapterStart(): " + getId());
    }

    @Override
    public void onAdapterStop() {
        System.out.println("[AmbulanceDigitalAdapter] -> onAdapterStop(): " + getId());
    }

    // ── DT Lifecycle ──────────────────────────────────────────────────────────

    @Override
    public void onDigitalTwinCreate() {
        System.out.println("[AmbulanceDigitalAdapter] -> onDigitalTwinCreate()");
    }

    @Override
    public void onDigitalTwinStart() {
        System.out.println("[AmbulanceDigitalAdapter] -> onDigitalTwinStart()");
    }

    /**
     * Il DT è entrato in stato Shadowed: riceviamo lo stato iniziale completo e
     * ci iscriviamo a tutti gli eventi dichiarati nella DT State.
     */
    @Override
    public void onDigitalTwinSync(DigitalTwinState currentDigitalTwinState) {
        System.out.println("[AmbulanceDigitalAdapter] -> onDigitalTwinSync()");
        printStateSnapshot("STATO INIZIALE DT AMBULANZA", currentDigitalTwinState);

        try {
            currentDigitalTwinState.getEventList()
                    .map(eventList -> eventList.stream()
                            .map(DigitalTwinStateEvent::getKey)
                            .collect(Collectors.toList()))
                    .ifPresent(keys -> {
                        try {
                            observeDigitalTwinEventsNotifications(keys);
                            System.out.println("[AmbulanceDigitalAdapter] -> Osservando eventi: " + keys);
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
        System.out.println("[AmbulanceDigitalAdapter] -> onDigitalTwinUnSync()");
    }

    @Override
    public void onDigitalTwinStop() {
        System.out.println("[AmbulanceDigitalAdapter] -> onDigitalTwinStop()");
    }

    @Override
    public void onDigitalTwinDestroy() {
        System.out.println("[AmbulanceDigitalAdapter] -> onDigitalTwinDestroy()");
    }

    // ── State Update ──────────────────────────────────────────────────────────

    @Override
    protected void onStateUpdate(DigitalTwinState newState,
                                 DigitalTwinState previousState,
                                 ArrayList<DigitalTwinStateChange> changes) {

        System.out.println("\n[AmbulanceDigitalAdapter] ── STATE UPDATE ────────────────────");

        if (changes != null && !changes.isEmpty()) {
            changes.forEach(change -> System.out.printf("  [%s] %s -> %s%n",
                    change.getOperation(), change.getResourceType(), change.getResource()));
        } else {
            System.out.println("  (nessuna variazione rilevata)");
        }

        printOperationalSnapshot(newState);
        System.out.println("─────────────────────────────────────────────────────────────\n");
    }

    // ── Domain Event Callbacks ────────────────────────────────────────────────

    @Override
    protected void onEventNotificationReceived(DigitalTwinStateEventNotification<?> notification) {
        if (notification == null) return;

        String eventKey = notification.getDigitalEventKey();
        Object body     = notification.getBody();

        System.out.println("\n[AmbulanceDigitalAdapter] ══ DOMAIN EVENT ═══════════════════");
        System.out.println("  Event Key : " + eventKey);
        System.out.println("  Timestamp : " + notification.getTimestamp());

        if (AmbulanceKeywords.MISSION_ASSIGNED_EVENT_KEY.equals(eventKey)) {
            System.out.println("  ► MISSIONE ASSEGNATA — Ambulanza in movimento verso paziente");
            printPayloadSummary(body);

        } else if (AmbulanceKeywords.PATIENT_ONBOARD_EVENT_KEY.equals(eventKey)) {
            System.out.println("  ► PAZIENTE PRESO IN CARICO — Trasporto verso ospedale avviato");
            printPayloadSummary(body);

        } else if (AmbulanceKeywords.HOSPITAL_HANDOVER_EVENT_KEY.equals(eventKey)) {
            System.out.println("  ► HANDOVER OSPEDALE COMPLETATO ✓");
            printPayloadSummary(body);

        } else if (AmbulanceKeywords.REFUELING_NEEDED_EVENT_KEY.equals(eventKey)) {
            System.out.println("  ► RIFORNIMENTO NECESSARIO ⛽");
            printPayloadSummary(body);

        } else if (AmbulanceKeywords.MAINTENANCE_NEEDED_EVENT_KEY.equals(eventKey)) {
            System.out.println("  ► MANUTENZIONE NECESSARIA 🔧");
            printPayloadSummary(body);

        } else {
            System.out.println("  (evento non gestito: " + eventKey + ")");
        }

        System.out.println("═════════════════════════════════════════════════════════════\n");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void printStateSnapshot(String label, DigitalTwinState state) {
        System.out.println("\n[AmbulanceDigitalAdapter] ── " + label + " ──");
        if (state == null) {
            System.out.println("  (stato null)");
            return;
        }
        try {
            state.getPropertyList().ifPresent(props ->
                    props.forEach(p -> System.out.printf("  PROP  %-50s = %s%n", p.getKey(), p.getValue())));
            state.getEventList().ifPresent(evts ->
                    evts.forEach(e -> System.out.printf("  EVENT %-50s (type=%s)%n", e.getKey(), e.getType())));
        } catch (Exception e) {
            System.err.println("  (errore lettura stato: " + e.getMessage() + ")");
        }
        System.out.println();
    }

    /** Snapshot delle proprietà operative più rilevanti per il monitoraggio. */
    private void printOperationalSnapshot(DigitalTwinState state) {
        if (state == null) return;

        String[] keys = {
                AmbulanceKeywords.STATE_PROPERTY_KEY,
                AmbulanceKeywords.PATIENT_ID_PROPERTY_KEY,
                AmbulanceKeywords.HOSPITAL_ID_PROPERTY_KEY,
                AmbulanceKeywords.FUEL_LEVEL_PROPERTY_KEY,
                AmbulanceKeywords.MISSIONS_SINCE_MAINTENANCE_PROPERTY_KEY,
                AmbulanceKeywords.NEEDS_REFUELING_PROPERTY_KEY,
                AmbulanceKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY,
                AmbulanceKeywords.TRIP_DISTANCE_PROPERTY_KEY
        };

        System.out.println("  [Snapshot operativo]");
        for (String key : keys) {
            try {
                state.getProperty(key).ifPresent(p ->
                        System.out.printf("    %-52s = %s%n", p.getKey(), p.getValue()));
            } catch (Exception ignored) {}
        }
    }

    private void printPayloadSummary(Object body) {
        if (body == null) {
            System.out.println("  Body: (null)");
            return;
        }
        if (body instanceof AmbulanceTelemetryPayload p) {
            System.out.printf("    state=%-20s patientId=%-12s hospitalId=%s%n",
                    p.state(), p.patientId(), p.hospitalId());
            System.out.printf("    fuel=%.2f  missions=%d  needsRefuel=%-5b  needsMaint=%-5b%n",
                    p.fuelLevel(), p.missionsSinceMaintenance(), p.needsRefueling(), p.needsMaintenance());
            System.out.printf("    lat=%.5f  lon=%.5f  tripDist=%.1fm  t=%.1f%n",
                    p.lat(), p.lon(), p.tripDistanceSinceEmergency(), p.timestamp());
        } else {
            System.out.println("  Body: " + body);
        }
    }
}