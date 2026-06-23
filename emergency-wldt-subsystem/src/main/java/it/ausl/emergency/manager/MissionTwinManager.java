package it.ausl.emergency.manager;

import it.ausl.emergency.payload.MissionTelemetryPayload;
import it.ausl.emergency.shadowing.MissionShadowingFunction;
import it.ausl.emergency.twin.MissionDigitalTwin;
import it.ausl.emergency.utils.MissionKeywords;
import it.wldt.core.engine.DigitalTwinEngine;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Factory e registry dei Digital Twin della Missione.
 * * Responsabilità:
 * - Creare un nuovo MissionDigitalTwin al primo messaggio di un missionId sconosciuto
 * - Smistare la telemetria al Physical Adapter del twin corretto
 * - Rilevare lo stato terminale "Completed" e rimuovere il twin dal registry a missione conclusa
 */
public class MissionTwinManager {

    private final DigitalTwinEngine engine;

    // Thread-safe: più messaggi MQTT di missioni diverse possono arrivare in parallelo
    private final ConcurrentHashMap<String, MissionDigitalTwin> registry =
            new ConcurrentHashMap<>();

    public MissionTwinManager(DigitalTwinEngine engine) {
        this.engine = engine;
    }

    public void onTelemetryReceived(String missionId, MissionTelemetryPayload payload) {
        if (missionId == null || missionId.isEmpty() || "null".equals(missionId)) return;

        AtomicBoolean justCreated = new AtomicBoolean(false);

        // Se il dt della missione non esiste, viene creato e avviato istantaneamente on-the-fly
        MissionDigitalTwin twin = registry.computeIfAbsent(missionId, id -> {
            try {
                justCreated.set(true);
                return createAndStartTwin(id, payload);
            } catch (Exception e) {
                throw new RuntimeException("Creation failed for mission: " + id, e);
            }
        });

        // Inoltra il payload al Configurable Physical Adapter della missione specifica
        twin.getPhysicalAdapter().onMissionTelemetryReceived(payload);

        // Se la missione entra nello stato terminale "Completed", schedula la rimozione dal registro locale
        if (MissionKeywords.STATE_COMPLETED.equalsIgnoreCase(payload.state())) {
            scheduleCleanup(missionId);
        }
    }

    public int activeMissionCount() {
        return registry.size();
    }

    public MissionDigitalTwin getMissionTwin(String missionId) {
        return registry.get(missionId);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private MissionDigitalTwin createAndStartTwin(String missionId, MissionTelemetryPayload payload) throws Exception {
        MissionShadowingFunction sf = new MissionShadowingFunction("mission-shadowing-" + missionId);
        MissionDigitalTwin twin = new MissionDigitalTwin("dt-" + missionId, sf);

        // Iniettiamo i valori iniziali della telemetria nella configurazione dell'adapter prima del boot della PAD
        twin.getPhysicalAdapter().getConfiguration().setDefaultState(payload.state());
        twin.getPhysicalAdapter().getConfiguration().setDefaultPatientId(payload.patientId());
        twin.getPhysicalAdapter().getConfiguration().setDefaultSeverityCode(payload.severityCode());
        twin.getPhysicalAdapter().getConfiguration().setDefaultHospitalId(payload.hospitalId());
        twin.getPhysicalAdapter().getConfiguration().setDefaultTimeCalled(payload.timeCalled());

        engine.addDigitalTwin(twin);
        engine.startDigitalTwin("dt-" + missionId);
        return twin;
    }

    /**
     * Attende qualche secondo per garantire la propagazione dello stato finale verso i Digital Adapter
     * esterni, poi rimuove la missione dal registro locale scaricando la memoria.
     */
    private void scheduleCleanup(String missionId) {
        Thread cleanupThread = new Thread(() -> {
            try {
                Thread.sleep(10_000);
                registry.remove(missionId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "mission-twin-cleanup-" + missionId);
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
}