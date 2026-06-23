package it.ausl.emergency.twin;

import it.ausl.emergency.adapter.configuration.MissionAdapterConfiguration;
import it.ausl.emergency.adapter.digital.MissionDigitalAdapter;
import it.ausl.emergency.adapter.physical.MissionPhysicalAdapter;
import it.ausl.emergency.shadowing.MissionShadowingFunction;
import it.wldt.core.engine.DigitalTwin;
import it.wldt.exception.EventBusException;
import it.wldt.exception.ModelException;
import it.wldt.exception.WldtConfigurationException;
import it.wldt.exception.WldtDigitalTwinStateException;
import it.wldt.exception.WldtRuntimeException;
import it.wldt.exception.WldtWorkerException;

/**
 * Mission Digital Twin Wrapper.
 * Compiles and orchestrates the structural physical adapters, the digital exposure layers,
 * and the KPI augmentation models inside the WLDT core execution frame.
 */
public class MissionDigitalTwin extends DigitalTwin {

    private final String id;
    private final MissionPhysicalAdapter physicalAdapter;
    private final MissionDigitalAdapter digitalAdapter;
    private final MissionShadowingFunction shadowingFunction;

    public MissionDigitalTwin(String digitalTwinId, MissionShadowingFunction shadowingFunction)
            throws ModelException, EventBusException, WldtRuntimeException,
            WldtWorkerException, WldtDigitalTwinStateException {

        super(digitalTwinId, shadowingFunction);
        this.id = digitalTwinId;
        this.shadowingFunction = shadowingFunction;
        MissionAdapterConfiguration sharedConfig = new MissionAdapterConfiguration();
        this.physicalAdapter = new MissionPhysicalAdapter(id, sharedConfig);
        this.digitalAdapter = new MissionDigitalAdapter(id, sharedConfig);

        try {
            this.addPhysicalAdapter(physicalAdapter);
            this.addDigitalAdapter(digitalAdapter);
        } catch (WldtConfigurationException | WldtWorkerException e) {
            e.printStackTrace();
        }
    }

    public MissionPhysicalAdapter getPhysicalAdapter() {
        return physicalAdapter;
    }

    public MissionDigitalAdapter getDigitalAdapter() {
        return digitalAdapter;
    }

    public MissionShadowingFunction getShadowingFunction() {
        return shadowingFunction;
    }
}