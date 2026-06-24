package it.ausl.emergency.twin;

import it.ausl.emergency.adapter.configuration.CentralEmergencyAdapterConfiguration;
import it.ausl.emergency.adapter.digital.CentralEmergencyDigitalAdapter;
import it.ausl.emergency.adapter.physical.CentralEmergencyPhysicalAdapter;
import it.ausl.emergency.manager.HospitalTwinManager;
import it.ausl.emergency.manager.MissionTwinManager;
import it.ausl.emergency.manager.VehicleTwinManager;
import it.ausl.emergency.shadowing.CentralEmergencyShadowingFunction;
import it.wldt.core.engine.DigitalTwin;
import it.wldt.exception.*;

/**
 * Digital Twin della Centrale Operativa.
 *
 * BUG FIX: aggiunta chiamata a digitalAdapter.setShadowingFunction(shadowingFunction)
 * in entrambi i costruttori, in modo che buildStateResponse() possa leggere i KPI
 * direttamente dalla ShadowingFunction tramite i suoi getter pubblici, invece di
 * affidarsi allo stato WLDT che richiede la chiamata esplicita a onMissionCompleted().
 */
public class CentralEmergencyDigitalTwin extends DigitalTwin {

    private final String id;
    private final CentralEmergencyPhysicalAdapter physicalAdapter;
    private final CentralEmergencyDigitalAdapter digitalAdapter;
    private final CentralEmergencyShadowingFunction shadowingFunction;

    public CentralEmergencyDigitalTwin(
            String digitalTwinId,
            CentralEmergencyShadowingFunction shadowingFunction,
            MissionTwinManager missionManager,
            VehicleTwinManager vehicleManager,
            HospitalTwinManager hospitalManager)
            throws ModelException, EventBusException, WldtRuntimeException,
                   WldtWorkerException, WldtDigitalTwinStateException {
        super(digitalTwinId != null ? digitalTwinId : "dt-central-operative", shadowingFunction);
        this.id = digitalTwinId;
        this.shadowingFunction = (CentralEmergencyShadowingFunction) this.getShadowingFunction();
        CentralEmergencyAdapterConfiguration sharedConfig = new CentralEmergencyAdapterConfiguration();
        this.physicalAdapter = new CentralEmergencyPhysicalAdapter(id + "-physical-adapter", sharedConfig);
        this.digitalAdapter  = new CentralEmergencyDigitalAdapter(
                id + "-digital-adapter", sharedConfig, missionManager, vehicleManager, hospitalManager);

        this.digitalAdapter.setShadowingFunction(shadowingFunction);

        registerAdapters();
    }

    private void registerAdapters() {
        try {
            this.addPhysicalAdapter(physicalAdapter);
            this.addDigitalAdapter(digitalAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public CentralEmergencyPhysicalAdapter   getPhysicalAdapter()   { return physicalAdapter; }
    public CentralEmergencyDigitalAdapter    getDigitalAdapter()    { return digitalAdapter; }
    public CentralEmergencyShadowingFunction getShadowingFunction() { return shadowingFunction; }
}