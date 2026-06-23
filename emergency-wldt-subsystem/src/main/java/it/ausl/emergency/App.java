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
        DigitalTwinEngine engine = new DigitalTwinEngine();

        try {
            CentralEmergencyShadowingFunction coSf = new CentralEmergencyShadowingFunction("centrale-operativa-sf");
            CentralEmergencyDigitalTwin centraleTwin = new CentralEmergencyDigitalTwin("dt-centrale-operativa", coSf);
            engine.addDigitalTwin(centraleTwin);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        PatientTwinManager patientManager = new PatientTwinManager(engine);
        PatientMqttIngestionAdapter patientIngestionAdapter = new PatientMqttIngestionAdapter(BROKER_URL, patientManager);

        VehicleTwinManager vehicleManager = new VehicleTwinManager(engine);
        VehicleMqttIngestionAdapter vehicleIngestionAdapter = new VehicleMqttIngestionAdapter(BROKER_URL, vehicleManager);

        HospitalTwinManager hospitalManager = new HospitalTwinManager(engine);
        HospitalMqttIngestionAdapter hospitalIngestionAdapter = new HospitalMqttIngestionAdapter(BROKER_URL, hospitalManager);

        MissionTwinManager missionManager = new MissionTwinManager(engine);
        MissionMqttIngestionAdapter missionIngestionAdapter = new MissionMqttIngestionAdapter(BROKER_URL, missionManager);
        try {
            patientIngestionAdapter.start();
            vehicleIngestionAdapter.start();
            hospitalIngestionAdapter.start();
            missionIngestionAdapter.start();
            engine.startAll();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}