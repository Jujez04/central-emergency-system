package it.ausl.emergency.shadowing;

import it.wldt.adapter.digital.event.DigitalActionWldtEvent;
import it.wldt.adapter.physical.PhysicalAssetDescription;
import it.wldt.adapter.physical.PhysicalAssetProperty;
import it.wldt.adapter.physical.event.PhysicalAssetEventWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceCreatedWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceDeletedWldtEvent;
import it.wldt.core.model.ShadowingFunction;
import it.wldt.core.state.DigitalTwinStateEvent;
import it.wldt.core.state.DigitalTwinStateEventNotification;
import it.wldt.core.state.DigitalTwinStateManager;
import it.wldt.core.state.DigitalTwinStateProperty;

import java.util.Map;

/**
 * Shadowing Function dell'Ospedale.
 * Traduce lo stato dell'infrastruttura ospedaliera della simulazione AnyLogic
 * nello stato del Digital Twin esposto verso l'esterno.
 * * Caratteristiche:
 * - Gestisce l'allineamento dei tipi primitivi (Integer per i livelli e i pazienti assistiti, Double per il tempo)
 * - Nessuna azione digitale e nessuna relazione (struttura passiva di ricovero flotta)
 */
public class HospitalShadowingFunction extends ShadowingFunction {

    public HospitalShadowingFunction(String id) {
        super(id);
    }

    // Lifecycle Callbacks

    @Override
    protected void onCreate() {
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected void onStop() {
    }

    // Bound Lifecycle State Management Callbacks

    @Override
    protected void onDigitalTwinBound(Map<String, PhysicalAssetDescription> adaptersPhysicalAssetDescriptionMap) {
        try {
            this.digitalTwinStateManager.startStateTransaction();

            adaptersPhysicalAssetDescriptionMap.values().forEach(pad -> {
                pad.getProperties().forEach(property -> {
                    try {
                        createDigitalTwinStateProperty(property);
                        this.observePhysicalAssetProperty(property);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                pad.getEvents().forEach(event -> {
                    try {
                        DigitalTwinStateEvent dtStateEvent = new DigitalTwinStateEvent(event.getKey(), event.getType());
                        this.digitalTwinStateManager.registerEvent(dtStateEvent);
                        this.observePhysicalAssetEvent(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                pad.getActions().forEach(action -> {
                    try {
                        it.wldt.core.state.DigitalTwinStateAction dtStateAction = new it.wldt.core.state.DigitalTwinStateAction(
                                action.getKey(), action.getType(), action.getContentType());
                        this.digitalTwinStateManager.enableAction(dtStateAction);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });

            this.digitalTwinStateManager.commitStateTransaction();
            observeDigitalActionEvents();
            notifyShadowingSync();

        } catch (Exception e) {
            System.err.println("HospitalShadowingFunction error during onDigitalTwinBound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDigitalTwinUnBound(Map<String, PhysicalAssetDescription> map, String reason) {
    }

    @Override
    protected void onPhysicalAdapterBidingUpdate(String id, PhysicalAssetDescription pad) {
    }

    // Inbound Telemetry Callbacks

    @Override
    protected void onPhysicalAssetPropertyVariation(PhysicalAssetPropertyWldtEvent<?> physicalAssetPropertyWldtEvent) {
        try {
            this.digitalTwinStateManager.startStateTransaction();
            updateDigitalTwinStateProperty(
                    physicalAssetPropertyWldtEvent.getPhysicalPropertyId(),
                    physicalAssetPropertyWldtEvent.getBody());
            this.digitalTwinStateManager.commitStateTransaction();
        } catch (Exception e) {
            System.err.println("HospitalShadowingFunction property variation processing error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onPhysicalAssetEventNotification(PhysicalAssetEventWldtEvent<?> physicalAssetEventWldtEvent) {
        try {
            this.digitalTwinStateManager.notifyDigitalTwinStateEvent(new DigitalTwinStateEventNotification<>(
                    physicalAssetEventWldtEvent.getPhysicalEventKey(),
                    physicalAssetEventWldtEvent.getBody(),
                    physicalAssetEventWldtEvent.getCreationTimestamp()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPhysicalAssetRelationshipEstablished(PhysicalAssetRelationshipInstanceCreatedWldtEvent<?> e) {
    }

    @Override
    protected void onPhysicalAssetRelationshipDeleted(PhysicalAssetRelationshipInstanceDeletedWldtEvent<?> e) {
    }

    @Override
    protected void onDigitalActionEvent(DigitalActionWldtEvent<?> digitalActionWldtEvent) {
    }

    // Structural Helper Methods

    private void createDigitalTwinStateProperty(PhysicalAssetProperty<?> property) throws Exception {
        Object val = property.getInitialValue();
        if (val instanceof Double) {
            this.digitalTwinStateManager.createProperty(new DigitalTwinStateProperty<>(property.getKey(), (Double) val));
        } else if (val instanceof Integer) {
            this.digitalTwinStateManager.createProperty(new DigitalTwinStateProperty<>(property.getKey(), (Integer) val));
        } else if (val instanceof Boolean) {
            this.digitalTwinStateManager.createProperty(new DigitalTwinStateProperty<>(property.getKey(), (Boolean) val));
        } else if (val instanceof String) {
            this.digitalTwinStateManager.createProperty(new DigitalTwinStateProperty<>(property.getKey(), (String) val));
        }
    }

    private void updateDigitalTwinStateProperty(String key, Object val) throws Exception {
        if (val instanceof Double) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, (Double) val));
        } else if (val instanceof Integer) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, (Integer) val));
        } else if (val instanceof Boolean) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, (Boolean) val));
        } else if (val instanceof String) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, (String) val));
        }
    }

    public DigitalTwinStateManager getDigitalTwinStateManager() {
        return this.digitalTwinStateManager;
    }
}