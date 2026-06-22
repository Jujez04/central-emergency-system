package it.ausl.emergency.twin;

import it.ausl.emergency.adapter.configuration.MedHelicopterAdapterConfiguration;
import it.ausl.emergency.adapter.digital.MedHelicopterDigitalAdapter;
import it.ausl.emergency.adapter.physical.MedHelicopterPhysicalAdapter;
import it.ausl.emergency.shadowing.MedHelicopterShadowingFunction;
import it.wldt.core.engine.DigitalTwin;
import it.wldt.exception.EventBusException;
import it.wldt.exception.ModelException;
import it.wldt.exception.WldtConfigurationException;
import it.wldt.exception.WldtDigitalTwinStateException;
import it.wldt.exception.WldtRuntimeException;
import it.wldt.exception.WldtWorkerException;

/**
 * Assembla i componenti del Digital Twin del MedHelicopter e li registra
 * nel WLDT engine.
 *
 * Uso tipico (test / main):
 * <pre>
 *   MedHelicopterShadowingFunction sf = new MedHelicopterShadowingFunction("heli-sf-" + agentId);
 *   MedHelicopterDigitalTwin dt = new MedHelicopterDigitalTwin("dt-" + agentId, sf);
 *   DigitalTwinEngine engine = new DigitalTwinEngine();
 *   engine.addDigitalTwin(dt);
 *   engine.startAll();
 * </pre>
 */
public class MedHelicopterDigitalTwin extends DigitalTwin {

    private final String                        id;
    private final MedHelicopterPhysicalAdapter  physicalAdapter;
    private final MedHelicopterDigitalAdapter   digitalAdapter;
    private final MedHelicopterShadowingFunction shadowingFunction;

    public MedHelicopterDigitalTwin(String digitalTwinId,
                                    MedHelicopterShadowingFunction shadowingFunction)
            throws ModelException, EventBusException, WldtRuntimeException,
                   WldtWorkerException, WldtDigitalTwinStateException {

        super(digitalTwinId, shadowingFunction);
        this.id                = digitalTwinId;
        this.shadowingFunction = shadowingFunction;
        this.physicalAdapter   = new MedHelicopterPhysicalAdapter(
                                        id, new MedHelicopterAdapterConfiguration());
        this.digitalAdapter    = new MedHelicopterDigitalAdapter(
                                        id, new MedHelicopterAdapterConfiguration());

        try {
            this.addPhysicalAdapter(physicalAdapter);
            this.addDigitalAdapter(digitalAdapter);
        } catch (WldtConfigurationException | WldtWorkerException e) {
            e.printStackTrace();
        }
    }

    public MedHelicopterPhysicalAdapter  getPhysicalAdapter()   { return physicalAdapter; }
    public MedHelicopterDigitalAdapter   getDigitalAdapter()    { return digitalAdapter; }
    public MedHelicopterShadowingFunction getShadowingFunction(){ return shadowingFunction; }
}
