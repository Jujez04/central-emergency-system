package it.ausl.emergency;

import it.ausl.emergency.adapter.physical.patient.PatientMqttIngestionAdapter;
import it.ausl.emergency.adapter.physical.VehicleMqttIngestionAdapter;
import it.ausl.emergency.adapter.physical.HospitalMqttIngestionAdapter;
import it.ausl.emergency.manager.PatientTwinManager;
import it.ausl.emergency.manager.VehicleTwinManager;
import it.ausl.emergency.manager.HospitalTwinManager;
import it.ausl.emergency.manager.MissionTwinManager;
import it.wldt.core.engine.DigitalTwinEngine;
import it.ausl.emergency.shadowing.CentralEmergencyShadowingFunction;
import it.ausl.emergency.twin.CentralEmergencyDigitalTwin;

public class App {

    private static final String BROKER_URL = System.getenv().getOrDefault("MQTT_BROKER_URL", "tcp://localhost:1883");

    public static void main(String[] args) {

        DigitalTwinEngine engine = new DigitalTwinEngine();
        MissionTwinManager missionManager   = new MissionTwinManager(engine);
        PatientTwinManager patientManager   = new PatientTwinManager(engine, missionManager);
        VehicleTwinManager vehicleManager   = new VehicleTwinManager(engine, missionManager);
        HospitalTwinManager hospitalManager = new HospitalTwinManager(engine);

        try {
            CentralEmergencyShadowingFunction centralEmergencyShadowingFunction = new CentralEmergencyShadowingFunction("central-operative-shadowing-function");
            CentralEmergencyDigitalTwin centralTwin = new CentralEmergencyDigitalTwin(
                    "dt-central-operative",
                    centralEmergencyShadowingFunction,
                    missionManager,
                    vehicleManager,
                    hospitalManager
            );

            engine.addDigitalTwin(centralTwin);

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        PatientMqttIngestionAdapter  patientIngestionAdapter  = new PatientMqttIngestionAdapter(BROKER_URL, patientManager);
        VehicleMqttIngestionAdapter  vehicleIngestionAdapter  = new VehicleMqttIngestionAdapter(BROKER_URL, vehicleManager);
        HospitalMqttIngestionAdapter hospitalIngestionAdapter = new HospitalMqttIngestionAdapter(BROKER_URL, hospitalManager);

        try {
            patientIngestionAdapter.start();
            vehicleIngestionAdapter.start();
            hospitalIngestionAdapter.start();
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