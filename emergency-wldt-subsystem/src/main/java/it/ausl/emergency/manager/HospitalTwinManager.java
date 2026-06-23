package it.ausl.emergency.manager;

import it.ausl.emergency.payload.HospitalTelemetryPayload;
import it.ausl.emergency.shadowing.HospitalShadowingFunction;
import it.ausl.emergency.twin.HospitalDigitalTwin;
import it.wldt.core.engine.DigitalTwinEngine;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory e registry dei Digital Twin dell'Ospedale.
 * * Responsabilità:
 * - Creare un nuovo HospitalDigitalTwin alla notifica di boot o telemetria sconosciuta
 * - Smistare i dati aggregati al Physical Adapter del twin corretto
 * - Isolare il core di WLDT dalla logica dei protocolli di rete esterni (MQTT)
 */
public class HospitalTwinManager {

    private final DigitalTwinEngine engine;

    // Registro thread-safe per la gestione concorrente delle istanze ospedaliere
    private final ConcurrentHashMap<String, HospitalDigitalTwin> registry = 
            new ConcurrentHashMap<>();

    public HospitalTwinManager(DigitalTwinEngine engine) {
        this.engine = engine;
    }

    /**
     * Invocato dall'adapter alla ricezione di un comando di CREATED su ces/registry/hospital
     */
    public synchronized void onHospitalCreated(String agentId, int assistanceLevel, double lat, double lon) {
        registry.computeIfAbsent(agentId, id -> {
            try {
                HospitalShadowingFunction sf = new HospitalShadowingFunction("hospital-shadowing-" + id);
                HospitalDigitalTwin twin = new HospitalDigitalTwin("dt-" + id, sf);
                twin.getPhysicalAdapter().getConfiguration().setDefaultAssistanceLevel(assistanceLevel);
                twin.getPhysicalAdapter().getConfiguration().setDefaultLatitude(lat);
                twin.getPhysicalAdapter().getConfiguration().setDefaultLongitude(lon);

                engine.addDigitalTwin(twin);
                engine.startDigitalTwin("dt-" + id);

                return twin;
            } catch (Exception e) {
                throw new RuntimeException("Problem with hospital creation: " + id, e);
            }
        });
    }

    public void onTelemetryReceived(String agentId, HospitalTelemetryPayload payload) {
        HospitalDigitalTwin twin = registry.computeIfAbsent(agentId, id -> {
            try {
                HospitalShadowingFunction sf = new HospitalShadowingFunction("hospital-shadowing-" + id);
                HospitalDigitalTwin t = new HospitalDigitalTwin("dt-" + id, sf);

                engine.addDigitalTwin(t);
                engine.startDigitalTwin("dt-" + id);
                return t;
            } catch (Exception e) {
                throw new RuntimeException("Error in telemetry for hospital: " + id, e);
            }
        });
        twin.getPhysicalAdapter().onHospitalTelemetryReceived(payload);
    }

    public int activeHospitalCount() {
        return registry.size();
    }

    public HospitalDigitalTwin getHospital(String agentId) {
        return registry.get(agentId);
    }
}