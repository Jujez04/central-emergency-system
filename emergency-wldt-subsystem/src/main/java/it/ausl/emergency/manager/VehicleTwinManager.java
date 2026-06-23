package it.ausl.emergency.manager;

import it.ausl.emergency.shadowing.AmbulanceShadowingFunction;
import it.ausl.emergency.shadowing.MedCarShadowingFunction;
import it.ausl.emergency.shadowing.MedHelicopterShadowingFunction;
import it.ausl.emergency.twin.AmbulanceDigitalTwin;
import it.ausl.emergency.twin.MedCarDigitalTwin;
import it.ausl.emergency.twin.MedHelicopterDigitalTwin;
import it.wldt.core.engine.DigitalTwin;
import it.wldt.core.engine.DigitalTwinEngine;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory e Registry dei Digital Twin flotta (Ambulanze, MedCar, MedHelicopter).
 * Responsabilità:
 * - Istanziare il Digital Twin specifico al momento della notifica di CREATED su ces/registry
 * - Mantenere il riferimento attivo dei twin per il successivo smistamento della telemetria
 */
public class VehicleTwinManager {

    private final DigitalTwinEngine engine;

    private final ConcurrentHashMap<String, DigitalTwin> registry = new ConcurrentHashMap<>();

    public VehicleTwinManager(DigitalTwinEngine engine) {
        this.engine = engine;
    }

    /**
     * Chiamato quando l'ingestion adapter riceve una notifica di creazione veicolo.
     */
    public synchronized void onVehicleCreated(String type, String agentId) {
        if (registry.containsKey(agentId)) {
            return;
        }

        try {
            DigitalTwin twin;

            switch (type.toLowerCase()) {
                case "ambulance":
                    AmbulanceShadowingFunction asf = new AmbulanceShadowingFunction("ambulance-shadowing-" + agentId);
                    twin = new AmbulanceDigitalTwin("dt-" + agentId, asf);
                    break;
                case "medcar":
                    MedCarShadowingFunction msf = new MedCarShadowingFunction("medcar-shadowing-" + agentId);
                    twin = new MedCarDigitalTwin("dt-" + agentId, msf);
                    break;
                case "medhelicopter":
                    MedHelicopterShadowingFunction hsf = new MedHelicopterShadowingFunction("helicopter-shadowing-" + agentId);
                    twin = new MedHelicopterDigitalTwin("dt-" + agentId, hsf);
                    break;
                default:
                    System.err.println("VehicleTwinManager unsupported vehicle: " + type);
                    return;
            }

            engine.addDigitalTwin(twin);
            engine.startDigitalTwin("dt-" + agentId);
            registry.put(agentId, twin);

        } catch (Exception e) {
            System.err.println("VehicleTwinManager error in creation of vehicle: " + agentId);
            e.printStackTrace();
        }
    }

    public DigitalTwin getVehicleTwin(String agentId) {
        return registry.get(agentId);
    }

    public int getRegisteredVehicleCount() {
        return registry.size();
    }
}