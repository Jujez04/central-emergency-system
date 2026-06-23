package it.ausl.emergency.adapter.physical;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ausl.emergency.manager.HospitalTwinManager;
import it.ausl.emergency.payload.HospitalTelemetryPayload;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MQTT Subscriber in ascolto sui topic dell'infrastruttura ospedaliera flotta.
 * Sottoscrive "ces/registry/hospital" e "ces/hospital/+/state".
 * Implementa una logica di sanificazione stringa per ovviare ai refusi di
 * virgolette del simulatore.
 */
public class HospitalMqttIngestionAdapter implements MqttCallback {

    private static final String TOPIC_REGISTRY = "ces/registry/hospital";
    private static final String TOPIC_STATE_WILDCARD = "ces/hospital/+";
    private static final int QOS = 1;

    private final String brokerUrl;
    private final HospitalTwinManager twinManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private MqttClient client;

    private final Pattern registryIdPattern = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private final Pattern registryLevelPattern = Pattern.compile("\"assistanceLevel\"\\s*:\\s*\"?(\\d+)\"?");
    private final Pattern registryLatPattern = Pattern.compile("\"lat\"\\s*:\\s*\"?([0-9.]+)\"?");
    private final Pattern registryLonPattern = Pattern.compile("\"lon\"\\s*:\\s*\"?([0-9.]+)\"?");

    public HospitalMqttIngestionAdapter(String brokerUrl, HospitalTwinManager twinManager) {
        this.brokerUrl = brokerUrl;
        this.twinManager = twinManager;
    }

    public void start() throws MqttException {
        client = new MqttClient(brokerUrl, "ces-hospital-ingestion-" + System.currentTimeMillis(),
                new MemoryPersistence());
        client.setCallback(this);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);

        client.connect(opts);
        client.subscribe(TOPIC_REGISTRY, QOS);
        client.subscribe(TOPIC_STATE_WILDCARD, QOS);
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
            String payloadRaw = new String(message.getPayload());

            if (topic.equals(TOPIC_REGISTRY)) {
                Matcher idMatcher = registryIdPattern.matcher(payloadRaw);
                Matcher levelMatcher = registryLevelPattern.matcher(payloadRaw);
                Matcher latMatcher = registryLatPattern.matcher(payloadRaw);
                Matcher lonMatcher = registryLonPattern.matcher(payloadRaw);

                if (idMatcher.find() && levelMatcher.find() && latMatcher.find() && lonMatcher.find()) {
                    String agentId = idMatcher.group(1);
                    int assistanceLevel = Integer.parseInt(levelMatcher.group(1));
                    double lat = Double.parseDouble(latMatcher.group(1));
                    double lon = Double.parseDouble(lonMatcher.group(1));
                    twinManager.onHospitalCreated(agentId, assistanceLevel, lat, lon);
                }
                return;
            }

            if (topic.startsWith("ces/hospital/") && topic.endsWith("/state")) {
                String[] segments = topic.split("/");
                if (segments.length < 4)
                    return;
                String agentId = segments[2];

                String payloadSanitized = payloadRaw.replaceAll(
                        "\"assistanceLevel\"\\s*:\\s*(\\d+)\"\\s*,",
                        "\"assistanceLevel\":$1,");

                HospitalTelemetryPayload payload = mapper.readValue(
                        payloadSanitized,
                        HospitalTelemetryPayload.class);

                twinManager.onTelemetryReceived(agentId, payload);
            }

        } catch (Exception e) {
            System.err.println("[HospitalMqttIngestionAdapter] Errore nell'elaborazione del messaggio sul topic "
                    + topic + ": " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[HospitalMqttIngestionAdapter] Connessione persa: " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}