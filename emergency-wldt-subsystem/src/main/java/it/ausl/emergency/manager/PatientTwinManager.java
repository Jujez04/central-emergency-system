package it.ausl.emergency.manager;

import it.ausl.emergency.payload.PatientTelemetryPayload;
import it.ausl.emergency.shadowing.PatientShadowingFunction;
import it.ausl.emergency.twin.PatientDigitalTwin;
import it.ausl.emergency.utils.PatientKeywords;
import it.wldt.core.engine.DigitalTwinEngine;
import it.wldt.core.state.DigitalTwinState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Factory e registry dei Digital Twin del Paziente.
 *
 * Responsabilità:
 *  - creare un nuovo PatientDigitalTwin al primo messaggio di un agentId sconosciuto
 *  - smistare la telemetria al Physical Adapter del twin corretto
 *  - rilevare lo stato "handover" e opzionalmente rimuovere il twin a missione conclusa
 *
 * Non sa nulla di MQTT: riceve dati già deserializzati da PatientMqttIngestionAdapter.
 */
public class PatientTwinManager {

    private static final long BIND_TIMEOUT_MS = 10_000L;
    private static final long BIND_POLL_INTERVAL_MS = 100L;

    private final DigitalTwinEngine engine;
    private final MissionTwinManager missionManager;

    // Thread-safe: più messaggi MQTT possono arrivare in parallelo
    private final ConcurrentHashMap<String, PatientDigitalTwin> registry =
            new ConcurrentHashMap<>();

    public PatientTwinManager(DigitalTwinEngine engine, MissionTwinManager manager) {
        this.engine = engine;
        this.missionManager = manager;
    }

    public void onTelemetryReceived(String agentId, PatientTelemetryPayload payload) {
        AtomicBoolean justCreated = new AtomicBoolean(false);

        PatientDigitalTwin twin = registry.computeIfAbsent(agentId, id -> {
            try {
                justCreated.set(true);
                return createAndStartTwin(id);
            } catch (Exception e) {
                throw new RuntimeException("Creation failed for: " + id, e);
            }
        });

        if (justCreated.get()) {
            boolean bound = waitForBinding(twin, agentId);
            if (!bound) {
                System.err.println("Error in patient binding");
            }
        }

        twin.getPhysicalAdapter().onPatientTelemetryReceived(payload);
        missionManager.onPatientTelemetryUpdate(agentId, payload);
        if (PatientKeywords.HANDOVER_STATE_VALUE.equalsIgnoreCase(payload.state())) {
            scheduleCleanup(agentId);
        }
    }

    public int activeTwinCount() {
        return registry.size();
    }

    public PatientDigitalTwin getTwin(String agentId) {
        return registry.get(agentId);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private PatientDigitalTwin createAndStartTwin(String agentId) throws Exception {

        PatientShadowingFunction sf =
                new PatientShadowingFunction("patient-shadowing-" + agentId);
        PatientDigitalTwin twin =
                new PatientDigitalTwin("dt-" + agentId, sf);

        engine.addDigitalTwin(twin);
        engine.startDigitalTwin("dt-" + agentId);
        return twin;
    }

    /**
     * Attende qualche secondo (per garantire la propagazione dell'ultimo stato)
     * poi rimuove il twin dal registry. Il twin rimane attivo nel motore WLDT
     * per eventuali query storiche; si può chiamare engine.stopDigitalTwin() se
     * si vuole rilasciare anche le risorse WLDT.
     */
    private void scheduleCleanup(String agentId) {
        Thread cleanupThread = new Thread(() -> {
            try {
                Thread.sleep(10_000);
                registry.remove(agentId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "patient-twin-cleanup-" + agentId);
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    private boolean waitForBinding(PatientDigitalTwin twin, String agentId) {
        long deadline = System.currentTimeMillis() + BIND_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            try {
                DigitalTwinState state = twin
                        .getShadowingFunction()
                        .getDigitalTwinStateManager()
                        .getDigitalTwinState();

                if (state != null && state.getProperty(PatientKeywords.STATE_PROPERTY_KEY).isPresent()) {
                    return true;
                }
            } catch (Exception ignored) {
                // stato non ancora pronto, continua il poll
            }

            try {
                Thread.sleep(BIND_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}