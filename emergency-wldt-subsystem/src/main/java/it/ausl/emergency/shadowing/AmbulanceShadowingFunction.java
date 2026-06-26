package it.ausl.emergency.shadowing;

import it.ausl.emergency.utils.AmbulanceKeywords;
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
 * Ambulance Shadowing Function.
 * Orchestrates the data translation loop for fleet vehicle assets and exposes
 * optimization execution action targets back to the physical simulation layout.
 */
public class AmbulanceShadowingFunction extends ShadowingFunction {

    public AmbulanceShadowingFunction(String id) {
        super(id);
    }

    @Override
    protected void onCreate() {
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected void onStop() {
    }

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

            // Start listening to inbound actions triggered from the digital adapters layer
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

    @Override
protected void onPhysicalAssetPropertyVariation(PhysicalAssetPropertyWldtEvent<?> event) {
    try {
        String key = event.getPhysicalPropertyId();
        Object val = event.getBody();
        if (val instanceof Double dVal && dVal == 0.0) {
            if (key.contains("fuel") || key.contains("latitude") || key.contains("longitude")) {
                return;
            }
        }
        this.digitalTwinStateManager.startStateTransaction();
        updateDigitalTwinStateProperty(key, val);
        this.digitalTwinStateManager.commitStateTransaction();

    } catch (Exception e) {
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

    /**
     * Catches digital actions (e.g., routing optimization decisions) from the
     * digital layer,
     * validates the contract, and forwards them directly down to the physical loop
     * via the adapter.
     */
    @Override
    protected void onDigitalActionEvent(DigitalActionWldtEvent<?> digitalActionWldtEvent) {
        if (digitalActionWldtEvent == null)
            return;

        String actionKey = digitalActionWldtEvent.getActionKey();

        if (AmbulanceKeywords.REDIRECT_VEHICLE_ACTION_KEY.equals(actionKey)) {
            try {
                // Publish the action down to the Physical Adapter layer for execution mapping
                this.publishPhysicalAssetActionWldtEvent(actionKey, digitalActionWldtEvent.getBody());
            } catch (Exception e) {
                System.err.println("AmbulanceShadowingFunction actuation delivery failure: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onPhysicalAssetRelationshipEstablished(PhysicalAssetRelationshipInstanceCreatedWldtEvent<?> e) {
    }

    @Override
    protected void onPhysicalAssetRelationshipDeleted(PhysicalAssetRelationshipInstanceDeletedWldtEvent<?> e) {
    }

    private void createDigitalTwinStateProperty(PhysicalAssetProperty<?> property) throws Exception {
        Object val = property.getInitialValue();
        if (val instanceof Double) {
            this.digitalTwinStateManager
                    .createProperty(new DigitalTwinStateProperty<>(property.getKey(), (Double) val));
        } else if (val instanceof Integer) {
            this.digitalTwinStateManager
                    .createProperty(new DigitalTwinStateProperty<>(property.getKey(), (Integer) val));
        } else if (val instanceof Boolean) {
            this.digitalTwinStateManager
                    .createProperty(new DigitalTwinStateProperty<>(property.getKey(), (Boolean) val));
        } else if (val instanceof String) {
            this.digitalTwinStateManager
                    .createProperty(new DigitalTwinStateProperty<>(property.getKey(), (String) val));
        }
    }

    private void updateDigitalTwinStateProperty(String key, Object val) throws Exception {
        if (val == null) {
            return;
        }
        java.util.Optional<DigitalTwinStateProperty<?>> existingProperty = 
                this.digitalTwinStateManager.getDigitalTwinState().getProperty(key);
        if (val instanceof Double) {
            Double newDoubleVal = (Double) val;
            if (newDoubleVal == 0.0 && existingProperty.isPresent()) {
                if (key.contains("fuelLevel") || key.contains("maintenance")) {
                    return; 
                }
            }
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, newDoubleVal));
        } else if (val instanceof Integer) {
            Integer newIntVal = (Integer) val;
            if (newIntVal == 0 && existingProperty.isPresent() && key.contains("Count")) {
                return;
            }
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, newIntVal));
        } else if (val instanceof Boolean) {
            Boolean newBoolVal = (Boolean) val;
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, newBoolVal));
        } else if (val instanceof String) {
            String newStringVal = (String) val;
            if ((newStringVal.isEmpty() || "null".equalsIgnoreCase(newStringVal)) && existingProperty.isPresent()) {
                return;
            }
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, newStringVal));
        }
    }

    public DigitalTwinStateManager getDigitalTwinStateManager() {
        return this.digitalTwinStateManager;
    }
}