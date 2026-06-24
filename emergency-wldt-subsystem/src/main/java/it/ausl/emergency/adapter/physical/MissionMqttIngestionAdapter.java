package it.ausl.emergency.adapter.physical;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ausl.emergency.manager.MissionTwinManager;
import it.ausl.emergency.payload.AmbulanceTelemetryPayload;
import it.ausl.emergency.payload.MedCarTelemetryPayload;
import it.ausl.emergency.payload.MedHelicopterTelemetryPayload;
import it.ausl.emergency.payload.PatientTelemetryPayload;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * MQTT Subscriber che alimenta il MissionTwinManager.
 *
 * La simulazione AnyLogic NON pubblica un topic ces/mission/+/state dedicato.
 * La MissioneDT è un'entità aggregata che viene costruita a partire dai payload
 * delle entità fisiche tracciabili (paziente, veicoli, ospedali).
 *
 * Ciclo di vita della MissioneDT:
 *  1. Payload PAZIENTE (ces/patient/+/state)
 *       → creazione della MissioneDT con stato iniziale derivato dallo stato del paziente
 *  2. Payload VEICOLO (ces/ambulance/+/state, ces/medcar/+/state, ces/medhelicopter/+/state)
 *       → aggiornamento dell'hospitalId e collegamento del veicolo alla missione
 *         (solo se il payload contiene un patientId valido)
 *  3. Handover completato (stato paziente = AtHospital)
 *       → la missione viene marcata come Completed e lo stato finale viene consolidato
 *
 * Questo adapter NON gestisce il topic ces/hospital perché l'ospedale viene
 * agganciato alla missione tramite l'hospitalId presente nei payload dei veicoli,
 * non tramite un messaggio MQTT diretto dell'ospedale verso la missione.
 */
public class MissionMqttIngestionAdapter implements MqttCallback {

    // Topic fisici pubblicati dalla simulazione AnyLogic
    private static final String TOPIC_PATIENT_WILDCARD     = "ces/patient/+/state";
    private static final String TOPIC_AMBULANCE_WILDCARD   = "ces/ambulance/+/state";
    private static final String TOPIC_MEDCAR_WILDCARD      = "ces/medcar/+/state";
    private static final String TOPIC_MEDHELICOPTER_WILDCARD = "ces/medhelicopter/+/state";

    private static final int QOS = 1;

    private final String brokerUrl;
    private final MissionTwinManager missionManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private MqttClient client;

    public MissionMqttIngestionAdapter(String brokerUrl, MissionTwinManager missionManager) {
        this.brokerUrl = brokerUrl;
        this.missionManager = missionManager;
    }

    public void start() throws MqttException {
        client = new MqttClient(
                brokerUrl,
                "ces-mission-ingestion-" + System.currentTimeMillis(),
                new MemoryPersistence()
        );
        client.setCallback(this);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setAutomaticReconnect(true);
        opts.setCleanSession(true);

        client.connect(opts);

        // Sottoscrizione ai topic delle entità fisiche tracciabili dalla simulazione
        client.subscribe(TOPIC_PATIENT_WILDCARD,       QOS);
        client.subscribe(TOPIC_AMBULANCE_WILDCARD,     QOS);
        client.subscribe(TOPIC_MEDCAR_WILDCARD,        QOS);
        client.subscribe(TOPIC_MEDHELICOPTER_WILDCARD, QOS);

        System.out.println("[MissionMqttIngestionAdapter] In ascolto sui topic:");
        System.out.println("  - " + TOPIC_PATIENT_WILDCARD);
        System.out.println("  - " + TOPIC_AMBULANCE_WILDCARD);
        System.out.println("  - " + TOPIC_MEDCAR_WILDCARD);
        System.out.println("  - " + TOPIC_MEDHELICOPTER_WILDCARD);
    }

    public void stop() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            client.close();
        }
    }

    // ── MqttCallback ──────────────────────────────────────────────────────────

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payloadStr = new String(message.getPayload());
            String[] segments = topic.split("/");

            if (segments.length < 4) {
                System.err.println("[MissionMqttIngestionAdapter] Formato topic non atteso: " + topic);
                return;
            }

            // ces/{tipo}/{agentId}/state
            String entityType = segments[1]; // patient | ambulance | medcar | medhelicopter
            String agentId    = segments[2];

            switch (entityType) {
                case "patient" -> handlePatientPayload(agentId, payloadStr);
                case "ambulance" -> handleAmbulancePayload(agentId, payloadStr);
                case "medcar" -> handleMedCarPayload(agentId, payloadStr);
                case "medhelicopter" -> handleMedHelicopterPayload(agentId, payloadStr);
                default -> { /* topic non pertinente alla missione, ignorato */ }
            }

        } catch (Exception e) {
            System.err.println("[MissionMqttIngestionAdapter] Errore elaborazione messaggio su topic "
                    + topic + ": " + e.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[MissionMqttIngestionAdapter] Connessione persa: " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    // ── Handler per tipo di entità ────────────────────────────────────────────

    /**
     * Il paziente è il trigger primario: ogni nuovo pazientId crea (o aggiorna)
     * la MissioneDT corrispondente.
     * L'ID della missione è derivato dall'agentId del paziente: "M-" + agentId.
     */
    private void handlePatientPayload(String patientId, String rawJson) {
        try {
            PatientTelemetryPayload payload = mapper.readValue(rawJson, PatientTelemetryPayload.class);
            missionManager.onPatientTelemetryUpdate(patientId, payload);
        } catch (Exception e) {
            System.err.println("[MissionMqttIngestionAdapter] Errore parsing payload paziente ["
                    + patientId + "]: " + e.getMessage());
        }
    }

    /**
     * Il payload dell'ambulanza contiene il patientId e l'hospitalId.
     * Se il patientId è valorizzato, viene usato per agganciare il veicolo
     * alla missione corrispondente e aggiornare la destinazione ospedaliera.
     */
    private void handleAmbulancePayload(String vehicleId, String rawJson) {
        try {
            AmbulanceTelemetryPayload payload = mapper.readValue(rawJson, AmbulanceTelemetryPayload.class);
            if (payload.hasPatient()) {
                missionManager.onVehicleTelemetryUpdate(vehicleId, payload);
            }
        } catch (Exception e) {
            System.err.println("[MissionMqttIngestionAdapter] Errore parsing payload ambulanza ["
                    + vehicleId + "]: " + e.getMessage());
        }
    }

    /**
     * La MedCar contiene il patientId ma NON l'hospitalId (torna alla postazione fissa).
     * Viene ugualmente notificata al MissionTwinManager per il collegamento del veicolo
     * alla missione; l'hospitalId rimarrà quello già impostato dall'ambulanza/elicottero.
     */
    private void handleMedCarPayload(String vehicleId, String rawJson) {
        try {
            MedCarTelemetryPayload payload = mapper.readValue(rawJson, MedCarTelemetryPayload.class);
            if (payload.hasPatient()) {
                // Convertiamo in un payload compatibile con onVehicleTelemetryUpdate.
                // La MedCar non ha hospitalId, usiamo "null" per preservare quello esistente.
                AmbulanceTelemetryPayload compatPayload = new AmbulanceTelemetryPayload(
                        payload.state(),
                        payload.lat(),
                        payload.lon(),
                        payload.patientId(),
                        "null",   // MedCar non trasporta al ospedale
                        payload.fuelLevel(),
                        payload.missionsSinceMaintenance(),
                        payload.needsRefueling(),
                        payload.needsMaintenance(),
                        payload.timestamp(),
                        payload.tripDistanceSinceEmergency()
                );
                missionManager.onVehicleTelemetryUpdate(vehicleId, compatPayload);
            }
        } catch (Exception e) {
            System.err.println("[MissionMqttIngestionAdapter] Errore parsing payload medcar ["
                    + vehicleId + "]: " + e.getMessage());
        }
    }

    /**
     * Il MedHelicopter contiene sia patientId che hospitalId.
     * Funziona come l'ambulanza: aggiorna veicolo e destinazione ospedaliera.
     */
    private void handleMedHelicopterPayload(String vehicleId, String rawJson) {
        try {
            MedHelicopterTelemetryPayload payload = mapper.readValue(rawJson, MedHelicopterTelemetryPayload.class);
            if (payload.hasPatient()) {
                AmbulanceTelemetryPayload compatPayload = new AmbulanceTelemetryPayload(
                        payload.state(),
                        payload.lat(),
                        payload.lon(),
                        payload.patientId(),
                        payload.hospitalId() != null ? payload.hospitalId() : "null",
                        payload.fuelLevel(),
                        payload.missionsSinceMaintenance(),
                        payload.needsRefueling(),
                        payload.needsMaintenance(),
                        payload.timestamp(),
                        payload.tripDistanceSinceEmergency()
                );
                missionManager.onVehicleTelemetryUpdate(vehicleId, compatPayload);
            }
        } catch (Exception e) {
            System.err.println("[MissionMqttIngestionAdapter] Errore parsing payload medhelicopter ["
                    + vehicleId + "]: " + e.getMessage());
        }
    }
}