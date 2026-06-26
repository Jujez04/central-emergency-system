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
import java.util.Optional;

public class MedHelicopterShadowingFunction extends ShadowingFunction {

    public MedHelicopterShadowingFunction(String id) {
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
                        DigitalTwinStateEvent dtEvent =
                                new DigitalTwinStateEvent(event.getKey(), event.getType());
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
            String key = event.getPhysicalPropertyId();
            Object val = event.getBody();
            Optional<DigitalTwinStateProperty<?>> existingProp = 
                    this.digitalTwinStateManager.getDigitalTwinState().getProperty(key);
            if (val instanceof Double dVal) {
                if (dVal == 0.0 && existingProp.isPresent()) {
                    if (key.contains("fuel") || key.contains("latitude") || key.contains("longitude")) {
                        return;
                    }
                }
            }
            
            if (val instanceof String sVal) {
                if ((sVal.isEmpty() || "null".equalsIgnoreCase(sVal)) && existingProp.isPresent()) {
                    return;
                }
            }
            this.digitalTwinStateManager.startStateTransaction();
            if (val instanceof String s) {
                this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, s));
            } else if (val instanceof Integer i) {
                this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, i));
            } else if (val instanceof Double d) {
                this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, d));
            } else if (val instanceof Boolean b) {
                this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, b));
            }
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
                    "MedHelicopterShadowingFunction Unsupported property type for key: "
                            + property.getKey());
        }
    }

    public DigitalTwinStateManager getDigitalTwinStateManager() {
        return this.digitalTwinStateManager;
    }
}
