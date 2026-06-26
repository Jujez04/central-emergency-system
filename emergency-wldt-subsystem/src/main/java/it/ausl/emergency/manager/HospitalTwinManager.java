package it.ausl.emergency.manager;

import it.ausl.emergency.payload.HospitalTelemetryPayload;
import it.ausl.emergency.shadowing.HospitalShadowingFunction;
import it.ausl.emergency.twin.HospitalDigitalTwin;
import it.wldt.core.engine.DigitalTwinEngine;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class HospitalTwinManager {

    private final DigitalTwinEngine engine;

    private final ConcurrentHashMap<String, HospitalDigitalTwin> registry =
            new ConcurrentHashMap<>();

    public HospitalTwinManager(DigitalTwinEngine engine) {
        this.engine = engine;
    }

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

    public Map<String, HospitalDigitalTwin> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }
}