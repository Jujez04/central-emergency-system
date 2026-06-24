package it.ausl.emergency.adapter.physical;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.ausl.emergency.manager.VehicleTwinManager;
import it.ausl.emergency.twin.AmbulanceDigitalTwin;
import it.ausl.emergency.twin.MedCarDigitalTwin;
import it.ausl.emergency.twin.MedHelicopterDigitalTwin;
import it.ausl.emergency.payload.AmbulanceTelemetryPayload;
import it.ausl.emergency.payload.MedCarTelemetryPayload;
import it.ausl.emergency.payload.MedHelicopterTelemetryPayload;
import it.wldt.core.engine.DigitalTwin;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class VehicleMqttIngestionAdapter implements MqttCallback {

    private static final String REGISTRY_TOPIC = "ces/registry";
    private static final String VEHICLE_STATE_WILDCARD = "ces/+/+/state";
    private static final int QOS = 1;

    private final String brokerUrl;
    private final VehicleTwinManager twinManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private MqttClient client;

    public VehicleMqttIngestionAdapter(String brokerUrl, VehicleTwinManager twinManager) {
        this.brokerUrl = brokerUrl;
        this.twinManager = twinManager;
    }

    public void start() throws MqttException {
        client = new MqttClient(brokerUrl, "ces-vehicle-ingestion-" + System.currentTimeMillis(),
                new MemoryPersistence());
        client.setCallback(this);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);

        client.connect(opts);
        client.subscribe(REGISTRY_TOPIC, QOS);
        client.subscribe(VEHICLE_STATE_WILDCARD, QOS);
    }

    public void stop() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            client.close();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payloadString = new String(message.getPayload());
            JsonNode rootNode = mapper.readTree(payloadString);

            if (topic.equals(REGISTRY_TOPIC)) {
                String action = rootNode.path("action").asText();
                String type = rootNode.path("type").asText();
                String id = rootNode.path("id").asText();

                if ("CREATED".equalsIgnoreCase(action) && !type.equalsIgnoreCase("patient")) {
                    twinManager.onVehicleCreated(type, id);
                }
                return;
            }

            if (topic.startsWith("ces/") && topic.endsWith("/state")) {
                String[] segments = topic.split("/");
                if (segments.length < 4)
                    return;

                String vehicleType = segments[1];
                String agentId = segments[2];

                DigitalTwin twin = twinManager.getVehicleTwin(agentId);
                if (twin == null) {
                    twinManager.onVehicleCreated(vehicleType, agentId);
                }

                // ─── CORREZIONE: Inoltra al Manager invece di bypassarlo andando all'adapter
                // ───
                forwardTelemetryToManager(vehicleType, agentId, payloadString);
            }
        } catch (Exception e) {
            System.err.println("VehicleMqttIngestionAdapter error: " + e.getMessage());
        }
    }

    private void forwardTelemetryToManager(String vehicleType, String agentId, String rawJson) {
        try {
            switch (vehicleType.toLowerCase()) {
                case "ambulance":
                    AmbulanceTelemetryPayload ambPayload = mapper.readValue(rawJson, AmbulanceTelemetryPayload.class);
                    twinManager.onAmbulanceTelemetry(agentId, ambPayload);
                    break;
                case "medcar":
                    MedCarTelemetryPayload carPayload = mapper.readValue(rawJson, MedCarTelemetryPayload.class);
                    twinManager.onMedCarTelemetry(agentId, carPayload);
                    break;
                case "medhelicopter":
                    MedHelicopterTelemetryPayload helPayload = mapper.readValue(rawJson,
                            MedHelicopterTelemetryPayload.class);
                    twinManager.onMedHelicopterTelemetry(agentId, helPayload);
                    break;
                default:
                    System.err.println("VehicleMqttIngestionAdapter unsupported vehicle type: " + vehicleType);
            }
        } catch (Exception e) {
            System.err.println("VehicleMqttIngestionAdapter failed to route telemetry to manager: " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable e) {
        System.err.println(e.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}