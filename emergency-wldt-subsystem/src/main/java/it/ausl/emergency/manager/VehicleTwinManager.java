package it.ausl.emergency.manager;

import it.ausl.emergency.payload.AmbulanceTelemetryPayload;
import it.ausl.emergency.payload.MedCarTelemetryPayload;
import it.ausl.emergency.payload.MedHelicopterTelemetryPayload;
import it.ausl.emergency.shadowing.AmbulanceShadowingFunction;
import it.ausl.emergency.shadowing.MedCarShadowingFunction;
import it.ausl.emergency.shadowing.MedHelicopterShadowingFunction;
import it.ausl.emergency.twin.AmbulanceDigitalTwin;
import it.ausl.emergency.twin.MedCarDigitalTwin;
import it.ausl.emergency.twin.MedHelicopterDigitalTwin;
import it.wldt.core.engine.DigitalTwin;
import it.wldt.core.engine.DigitalTwinEngine;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VehicleTwinManager {

    private final DigitalTwinEngine engine;
    private final MissionTwinManager missionManager;

    private final ConcurrentHashMap<String, DigitalTwin> registry = new ConcurrentHashMap<>();

    public VehicleTwinManager(
            DigitalTwinEngine engine,
            MissionTwinManager missionManager) {

        this.engine = engine;
        this.missionManager = missionManager;
    }

    public synchronized void onVehicleCreated(
            String type,
            String agentId) {

        if (registry.containsKey(agentId)) {
            return;
        }

        try {

            DigitalTwin twin;

            switch (type.toLowerCase()) {

                case "ambulance":

                    twin = new AmbulanceDigitalTwin(
                            "dt-" + agentId,
                            new AmbulanceShadowingFunction(
                                    "ambulance-shadowing-" + agentId));

                    break;

                case "medcar":

                    twin = new MedCarDigitalTwin(
                            "dt-" + agentId,
                            new MedCarShadowingFunction(
                                    "medcar-shadowing-" + agentId));

                    break;

                case "medhelicopter":

                    twin = new MedHelicopterDigitalTwin(
                            "dt-" + agentId,
                            new MedHelicopterShadowingFunction(
                                    "helicopter-shadowing-" + agentId));

                    break;

                default:
                    return;
            }

            engine.addDigitalTwin(twin);
            engine.startDigitalTwin("dt-" + agentId);

            registry.put(agentId, twin);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onAmbulanceTelemetry(String vehicleId, AmbulanceTelemetryPayload payload) {
        DigitalTwin twin = registry.get(vehicleId);

        if (twin instanceof AmbulanceDigitalTwin ambulanceTwin) {
            ambulanceTwin.getPhysicalAdapter().onAmbulanceTelemetryReceived(payload);
        }

        // ─── CORREZIONE: Invocazione del metodo corretto per aggiornare i KPI
        // temporali ───
        if (payload.hasPatient()) {
            missionManager.onVehicleTelemetryUpdate(vehicleId, payload);
        }
    }

    public void onMedCarTelemetry(String vehicleId, MedCarTelemetryPayload payload) {
        DigitalTwin twin = registry.get(vehicleId);

        if (twin instanceof MedCarDigitalTwin medCarTwin) {
            medCarTwin.getPhysicalAdapter().onMedCarTelemetryReceived(payload);
        }

        // ─── CORREZIONE: Convertiamo in payload compatibile ed eseguiamo l'update
        // temporale ───
        if (payload.hasPatient()) {
            AmbulanceTelemetryPayload compatPayload = new AmbulanceTelemetryPayload(
                    payload.state(), payload.lat(), payload.lon(), payload.patientId(),
                    "null", payload.fuelLevel(), payload.missionsSinceMaintenance(),
                    payload.needsRefueling(), payload.needsMaintenance(), payload.timestamp(),
                    payload.tripDistanceSinceEmergency());
            missionManager.onVehicleTelemetryUpdate(vehicleId, compatPayload);
        }
    }

    public void onMedHelicopterTelemetry(String vehicleId, MedHelicopterTelemetryPayload payload) {
        DigitalTwin twin = registry.get(vehicleId);

        if (twin instanceof MedHelicopterDigitalTwin helicopterTwin) {
            helicopterTwin.getPhysicalAdapter().onMedHelicopterTelemetryReceived(payload);
        }

        // temporale ───
        if (payload.hasPatient()) {
            AmbulanceTelemetryPayload compatPayload = new AmbulanceTelemetryPayload(
                    payload.state(), payload.lat(), payload.lon(), payload.patientId(),
                    payload.hospitalId() != null ? payload.hospitalId() : "null",
                    payload.fuelLevel(), payload.missionsSinceMaintenance(),
                    payload.needsRefueling(), payload.needsMaintenance(), payload.timestamp(),
                    payload.tripDistanceSinceEmergency());
            missionManager.onVehicleTelemetryUpdate(vehicleId, compatPayload);
        }
    }

    public DigitalTwin getVehicleTwin(String agentId) {
        return registry.get(agentId);
    }

    public int getRegisteredVehicleCount() {
        return registry.size();
    }

    public Map<String, DigitalTwin> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }
}