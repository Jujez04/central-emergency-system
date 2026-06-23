package it.ausl.emergency.shadowing;

import it.ausl.emergency.utils.MedCarKeywords;
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
import it.wldt.core.state.DigitalTwinStateProperty;

import java.util.Map;

/**
 * Shadowing Function della MedCar.
 *
 * Traduce lo stato dell'agente MedCar della simulazione AnyLogic
 * nello stato del Digital Twin, esposto poi verso l'esterno dal
 * MedCarDigitalAdapter.
 *
 * Rispetto all'AmbulanceShadowingFunction:
 *  - Nessuna azione digitale (la MedCar non espone redirect)
 *  - Gestisce homeBaseId (String) invece di hospitalId
 *  - Helper createDigitalTwinStateProperty / updateDigitalTwinStateProperty
 *    identici per struttura, specifici per namespace MedCar
 */
public class MedCarShadowingFunction extends ShadowingFunction {

    public MedCarShadowingFunction(String id) {
        super(id);
    }

    // Lifecycle

    @Override
    protected void onCreate() {
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected void onStop() {
    }

    // Binding
    @Override
    protected void onDigitalTwinBound(Map<String, PhysicalAssetDescription> adaptersMap) {
        try {
            this.digitalTwinStateManager.startStateTransaction();

            adaptersMap.values().forEach(pad -> {

                // Proprietà
                pad.getProperties().forEach(property -> {
                    try {
                        createDigitalTwinStateProperty(property);
                        this.observePhysicalAssetProperty(property);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                // Events
                pad.getEvents().forEach(event -> {
                    try {
                        DigitalTwinStateEvent dtEvent = new DigitalTwinStateEvent(
                                event.getKey(), event.getType());
                        this.digitalTwinStateManager.registerEvent(dtEvent);
                        this.observePhysicalAssetEvent(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                pad.getActions().forEach(action -> {
                    try {
                        it.wldt.core.state.DigitalTwinStateAction dtAction =
                                new it.wldt.core.state.DigitalTwinStateAction(
                                        action.getKey(), action.getType(), action.getContentType());
                        this.digitalTwinStateManager.enableAction(dtAction);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });

            this.digitalTwinStateManager.commitStateTransaction();
            observeDigitalActionEvents();
            notifyShadowingSync();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDigitalTwinUnBound(Map<String, PhysicalAssetDescription> map, String reason) {
    }

    @Override
    protected void onPhysicalAdapterBidingUpdate(String id, PhysicalAssetDescription pad) {
    }

    // Property Variation

    @Override
    protected void onPhysicalAssetPropertyVariation(PhysicalAssetPropertyWldtEvent<?> event) {
        try {
            this.digitalTwinStateManager.startStateTransaction();
            updateDigitalTwinStateProperty(
                    event.getPhysicalPropertyId(),
                    event.getBody());
            this.digitalTwinStateManager.commitStateTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Event Notification ────────────────────────────────────────────────────

    @Override
    protected void onPhysicalAssetEventNotification(PhysicalAssetEventWldtEvent<?> event) {
        try {
            this.digitalTwinStateManager.notifyDigitalTwinStateEvent(
                    new DigitalTwinStateEventNotification<>(
                            event.getPhysicalEventKey(),
                            event.getBody(),
                            event.getCreationTimestamp()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPhysicalAssetRelationshipEstablished(
            PhysicalAssetRelationshipInstanceCreatedWldtEvent<?> e) {}

    @Override
    protected void onPhysicalAssetRelationshipDeleted(
            PhysicalAssetRelationshipInstanceDeletedWldtEvent<?> e) {}

    @Override
    protected void onDigitalActionEvent(DigitalActionWldtEvent<?> event) {
    }

    // Helper Methods

    private void createDigitalTwinStateProperty(PhysicalAssetProperty<?> property) throws Exception {
        Object val = property.getInitialValue();
        if (val instanceof Double) {
            this.digitalTwinStateManager.createProperty(
                    new DigitalTwinStateProperty<>(property.getKey(), (Double) val));
        } else if (val instanceof Integer) {
            this.digitalTwinStateManager.createProperty(
                    new DigitalTwinStateProperty<>(property.getKey(), (Integer) val));
        } else if (val instanceof Boolean) {
            this.digitalTwinStateManager.createProperty(
                    new DigitalTwinStateProperty<>(property.getKey(), (Boolean) val));
        } else if (val instanceof String) {
            this.digitalTwinStateManager.createProperty(
                    new DigitalTwinStateProperty<>(property.getKey(), (String) val));
        } else {
            throw new IllegalArgumentException(
                    "[MedCarShadowingFunction] Unsupported property type for key: " + property.getKey());
        }
    }

    private void updateDigitalTwinStateProperty(String key, Object val) throws Exception {
        if (val instanceof Double) {
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(key, (Double) val));
        } else if (val instanceof Integer) {
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(key, (Integer) val));
        } else if (val instanceof Boolean) {
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(key, (Boolean) val));
        } else if (val instanceof String) {
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(key, (String) val));
        } else {
            throw new IllegalArgumentException(
                    "MedCarShadowingFunction Unsupported value type for key: " + key);
        }
    }
}