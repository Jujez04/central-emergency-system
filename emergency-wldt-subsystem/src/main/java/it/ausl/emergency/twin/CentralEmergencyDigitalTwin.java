package it.ausl.emergency.twin;

import it.ausl.emergency.adapter.configuration.CentraleOperativaAdapterConfiguration;
import it.ausl.emergency.adapter.digital.CentralEmergencyDigitalAdapter;
import it.ausl.emergency.adapter.physical.CentralEmergencyPhysicalAdapter;
import it.ausl.emergency.shadowing.CentralEmergencyShadowingFunction;
import it.wldt.core.engine.DigitalTwin;
import it.wldt.exception.*;

public class CentralEmergencyDigitalTwin extends DigitalTwin {

    private final String id;
    private final CentralEmergencyPhysicalAdapter physicalAdapter;
    private final CentralEmergencyDigitalAdapter digitalAdapter;
    private final CentralEmergencyShadowingFunction shadowingFunction;

    public CentralEmergencyDigitalTwin(String digitalTwinId, CentralEmergencyShadowingFunction shadowingFunction) 
            throws ModelException, EventBusException, WldtRuntimeException, WldtWorkerException, WldtDigitalTwinStateException {
        super(digitalTwinId, shadowingFunction);
        this.id = digitalTwinId;
        this.shadowingFunction = shadowingFunction;
        CentraleOperativaAdapterConfiguration sharedConfig = new CentraleOperativaAdapterConfiguration();
        this.physicalAdapter = new CentralEmergencyPhysicalAdapter(id, sharedConfig);
        this.digitalAdapter = new CentralEmergencyDigitalAdapter(id, sharedConfig);

        try {
            this.addPhysicalAdapter(physicalAdapter);
            this.addDigitalAdapter(digitalAdapter);
        } catch (WldtConfigurationException e) {
            e.printStackTrace();
        }
    }

    public CentralEmergencyPhysicalAdapter   getPhysicalAdapter()   { return physicalAdapter; }
    public CentralEmergencyDigitalAdapter    getDigitalAdapter()    { return digitalAdapter; }
    public CentralEmergencyShadowingFunction getShadowingFunction() { return shadowingFunction; }
}