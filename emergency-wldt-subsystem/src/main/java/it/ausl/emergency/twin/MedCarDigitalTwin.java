package it.ausl.emergency.twin;

import it.ausl.emergency.adapter.configuration.MedCarAdapterConfiguration;
import it.ausl.emergency.adapter.digital.MedCarDigitalAdapter;
import it.ausl.emergency.adapter.physical.MedCarPhysicalAdapter;
import it.ausl.emergency.shadowing.MedCarShadowingFunction;
import it.wldt.core.engine.DigitalTwin;
import it.wldt.exception.EventBusException;
import it.wldt.exception.ModelException;
import it.wldt.exception.WldtConfigurationException;
import it.wldt.exception.WldtDigitalTwinStateException;
import it.wldt.exception.WldtRuntimeException;
import it.wldt.exception.WldtWorkerException;

public class MedCarDigitalTwin extends DigitalTwin {

    private final String id;
    private final MedCarPhysicalAdapter physicalAdapter;
    private final MedCarDigitalAdapter digitalAdapter;
    private final MedCarShadowingFunction shadowingFunction;

    public MedCarDigitalTwin(String digitalTwinId, MedCarShadowingFunction shadowingFunction)
            throws ModelException, EventBusException, WldtRuntimeException,
            WldtWorkerException, WldtDigitalTwinStateException {

        super(digitalTwinId, shadowingFunction);
        this.id = digitalTwinId;
        this.shadowingFunction = shadowingFunction;
        MedCarAdapterConfiguration mac = new MedCarAdapterConfiguration();
        this.physicalAdapter = new MedCarPhysicalAdapter(id, mac);
        this.digitalAdapter = new MedCarDigitalAdapter(id, mac);

        try {
            this.addPhysicalAdapter(physicalAdapter);
            this.addDigitalAdapter(digitalAdapter);
        } catch (WldtConfigurationException | WldtWorkerException e) {
            e.printStackTrace();
        }
    }

    public MedCarPhysicalAdapter getPhysicalAdapter() {
        return physicalAdapter;
    }

    public MedCarDigitalAdapter getDigitalAdapter() {
        return digitalAdapter;
    }

    public MedCarShadowingFunction getShadowingFunction() {
        return shadowingFunction;
    }
}