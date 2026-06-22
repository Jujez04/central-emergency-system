package it.ausl.emergency.twin;

import it.ausl.emergency.adapter.configuration.AmbulanceAdapterConfiguration;
import it.ausl.emergency.adapter.digital.AmbulanceDigitalAdapter;
import it.ausl.emergency.adapter.physical.AmbulancePhysicalAdapter;
import it.ausl.emergency.shadowing.AmbulanceShadowingFunction;
import it.wldt.core.engine.DigitalTwin;
import it.wldt.exception.EventBusException;
import it.wldt.exception.ModelException;
import it.wldt.exception.WldtConfigurationException;
import it.wldt.exception.WldtDigitalTwinStateException;
import it.wldt.exception.WldtRuntimeException;
import it.wldt.exception.WldtWorkerException;

public class AmbulanceDigitalTwin extends DigitalTwin {

    private final String id;
    private final AmbulancePhysicalAdapter physicalAdapter;
    private final AmbulanceDigitalAdapter digitalAdapter;
    private final AmbulanceShadowingFunction shadowingFunction;

    public AmbulanceDigitalTwin(String digitalTwinId, AmbulanceShadowingFunction shadowingFunction)
            throws ModelException, EventBusException, WldtRuntimeException,
            WldtWorkerException, WldtDigitalTwinStateException {

        super(digitalTwinId, shadowingFunction);
        this.id = digitalTwinId;
        this.shadowingFunction = shadowingFunction;
        AmbulanceAdapterConfiguration aad = new AmbulanceAdapterConfiguration();
        this.physicalAdapter = new AmbulancePhysicalAdapter(id, aad);
        this.digitalAdapter = new AmbulanceDigitalAdapter(id, aad);

        try {
            this.addPhysicalAdapter(physicalAdapter);
            this.addDigitalAdapter(digitalAdapter);
        } catch (WldtConfigurationException | WldtWorkerException e) {
            e.printStackTrace();
        }
    }

    public AmbulancePhysicalAdapter getPhysicalAdapter() {
        return physicalAdapter;
    }

    public AmbulanceDigitalAdapter getDigitalAdapter() {
        return digitalAdapter;
    }

    public AmbulanceShadowingFunction getShadowingFunction() {
        return shadowingFunction;
    }
}