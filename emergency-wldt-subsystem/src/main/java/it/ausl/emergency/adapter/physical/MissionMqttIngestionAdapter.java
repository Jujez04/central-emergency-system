package it.ausl.emergency.adapter.physical;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ausl.emergency.manager.MissionTwinManager;
import it.ausl.emergency.payload.MissionTelemetryPayload;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * MQTT Subscriber in ascolto sul topic wildcard "ces/mission/+/state".
 * Estrae il missionId dal topic, deserializza il payload JSON e delega
 * la gestione del ciclo di vita e dello stato al MissionTwinManager.
 */
public class MissionMqttIngestionAdapter implements MqttCallback {

    private static final String TOPIC_WILDCARD = "ces/mission/+/state";
    private static final int QOS = 1;

    private final String brokerUrl;
    private final MissionTwinManager twinManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private MqttClient client;

    public MissionMqttIngestionAdapter(String brokerUrl, MissionTwinManager twinManager) {
        this.brokerUrl = brokerUrl;
        this.twinManager = twinManager;
    }

    public void start() throws MqttException {
        client = new MqttClient(brokerUrl, "ces-mission-ingestion-" + System.currentTimeMillis(),
                new MemoryPersistence());
        client.setCallback(this);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);

        client.connect(opts);
        client.subscribe(TOPIC_WILDCARD, QOS);
    }

    public void stop() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            client.close();
        }
    }

    // MqttCallback Implementation

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String[] segments = topic.split("/");
            if (segments.length < 4) {
                System.err.println("Error in message format: " + topic);
                return;
            }

            String missionId = segments[2];

            MissionTelemetryPayload payload = mapper.readValue(
                    message.getPayload(),
                    MissionTelemetryPayload.class
            );

            twinManager.onTelemetryReceived(missionId, payload);

        } catch (Exception e) {
            System.err.println("Parsing error in topic " 
                    + topic + ": " + e.getMessage());
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