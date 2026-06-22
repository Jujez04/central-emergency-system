package it.ausl.emergency;

import it.ausl.emergency.adapter.physical.patient.PatientMqttIngestionAdapter;
import it.ausl.emergency.adapter.physical.VehicleMqttIngestionAdapter;
import it.ausl.emergency.adapter.physical.HospitalMqttIngestionAdapter;
import it.ausl.emergency.adapter.physical.MissionMqttIngestionAdapter; // Nuovo adapter
import it.ausl.emergency.manager.PatientTwinManager;
import it.ausl.emergency.manager.VehicleTwinManager;
import it.ausl.emergency.manager.HospitalTwinManager;
import it.ausl.emergency.manager.MissionTwinManager; // Nuovo manager
import it.wldt.core.engine.DigitalTwinEngine;
import it.ausl.emergency.shadowing.CentralEmergencyShadowingFunction;
import it.ausl.emergency.twin.CentralEmergencyDigitalTwin;

public class App {

    private static final String BROKER_URL = System.getenv().getOrDefault("MQTT_BROKER_URL", "tcp://localhost:1883");

    public static void main(String[] args) {

        // 1. Inizializzazione dell'Engine Core di WLDT
        DigitalTwinEngine engine = new DigitalTwinEngine();

        // ── NUOVO REGISTRAZIONE STATICA: AUTOMATICA AL BOOT ──────────────────
        try {
            System.out.println("[Main] -> Boot dell'infrastruttura di controllo...");
            CentralEmergencyShadowingFunction coSf = new CentralEmergencyShadowingFunction("centrale-operativa-sf");
            CentralEmergencyDigitalTwin centraleTwin = new CentralEmergencyDigitalTwin("dt-centrale-operativa", coSf);
            
            engine.addDigitalTwin(centraleTwin);
            // Non c'è bisogno di aspettare messaggi MQTT, la centrale parte subito!
            System.out.println("[Main] -> Digital Twin della Centrale Operativa caricato nel core engine.");
        } catch (Exception e) {
            System.err.println("[Main] Errore fatale nell'inizializzazione della Centrale Operativa: " + e.getMessage());
            return;
        }
        // ─────────────────────────────────────────────────────────────────────

        // 2. Setup Componenti Gestione Pazienti, Veicoli, Ospedali e Missioni
        PatientTwinManager patientManager = new PatientTwinManager(engine);
        PatientMqttIngestionAdapter patientIngestionAdapter = new PatientMqttIngestionAdapter(BROKER_URL, patientManager);

        VehicleTwinManager vehicleManager = new VehicleTwinManager(engine);
        VehicleMqttIngestionAdapter vehicleIngestionAdapter = new VehicleMqttIngestionAdapter(BROKER_URL, vehicleManager);

        HospitalTwinManager hospitalManager = new HospitalTwinManager(engine);
        HospitalMqttIngestionAdapter hospitalIngestionAdapter = new HospitalMqttIngestionAdapter(BROKER_URL, hospitalManager);

        MissionTwinManager missionManager = new MissionTwinManager(engine);
        MissionMqttIngestionAdapter missionIngestionAdapter = new MissionMqttIngestionAdapter(BROKER_URL, missionManager);

        // 3. Avvio coordinato di tutti i moduli di Ingestion MQTT ed Engine
        try {
            patientIngestionAdapter.start();
            vehicleIngestionAdapter.start();
            hospitalIngestionAdapter.start();
            missionIngestionAdapter.start();
            
            // Avvia tutti i twin registrati (inclusa la Centrale)
            engine.startAll(); 
            System.out.println("[Main] -> Layer di esecuzione dei Digital Twin completamente attivo.");
        } catch (Exception e) {
            System.err.println("[Main] Errore critico in fase di avvio delle ingestion MQTT: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Keep-alive thread logica già presente...
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("[Main] Thread principale interrotto.");
            Thread.currentThread().interrupt();
        }
    }
}