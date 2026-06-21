package it.ausl.emergency;

import it.ausl.emergency.adapter.physical.patient.PatientMqttIngestionAdapter;
import it.ausl.emergency.manager.PatientTwinManager;
import it.wldt.core.engine.DigitalTwinEngine;

public class App {

    // Sostituisci con l'indirizzo reale del broker su cui pubblica AnyLogic
    private static final String BROKER_URL = System.getenv().getOrDefault("MQTT_BROKER_URL", "tcp://mosquitto:1883");

    public static void main(String[] args) {

        DigitalTwinEngine engine = new DigitalTwinEngine();
        PatientTwinManager manager = new PatientTwinManager(engine);
        PatientMqttIngestionAdapter ingestionAdapter =
                new PatientMqttIngestionAdapter(BROKER_URL, manager);

        try {
            ingestionAdapter.start();
            System.out.println("[Main] Ingestion MQTT avviata. In ascolto su ces/patient/+/state ...");
        } catch (Exception e) {
            System.err.println("[Main] Errore in fase di avvio dell'ingestion MQTT: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Shutdown pulito: ferma adapter MQTT ed engine WLDT alla chiusura del processo
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Main] Shutdown in corso...");
            try {
                ingestionAdapter.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                engine.stopAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("[Main] Shutdown completato.");
        }));

        // Mantiene vivo il processo principale finché non viene terminato (es. CTRL+C)
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}