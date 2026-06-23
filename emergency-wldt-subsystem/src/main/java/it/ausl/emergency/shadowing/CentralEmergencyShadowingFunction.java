package it.ausl.emergency.shadowing;

import it.ausl.emergency.utils.CentralEmergencyKeywords;
import it.wldt.adapter.digital.event.DigitalActionWldtEvent;
import it.wldt.adapter.physical.PhysicalAssetDescription;
import it.wldt.adapter.physical.PhysicalAssetProperty;
import it.wldt.adapter.physical.event.PhysicalAssetEventWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceCreatedWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceDeletedWldtEvent;
import it.wldt.core.model.ShadowingFunction;
import it.wldt.core.state.DigitalTwinStateProperty;
import it.wldt.core.state.DigitalTwinStateRelationship;

import java.util.Map;

public class CentralEmergencyShadowingFunction extends ShadowingFunction {

    public CentralEmergencyShadowingFunction(String id) {
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
                pad.getRelationships().forEach(rel -> {
                    try {
                        DigitalTwinStateRelationship<String> dtRel = new DigitalTwinStateRelationship<>(rel.getName(),
                                rel.getName());
                        this.digitalTwinStateManager.createRelationship(dtRel);
                        this.observePhysicalAssetRelationship(rel);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                pad.getActions().forEach(action -> {
                    try {
                        this.digitalTwinStateManager.enableAction(new it.wldt.core.state.DigitalTwinStateAction(
                                action.getKey(), action.getType(), action.getContentType()));
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

    @Override
    protected void onPhysicalAssetPropertyVariation(PhysicalAssetPropertyWldtEvent<?> event) {
        try {
            this.digitalTwinStateManager.startStateTransaction();
            Object val = event.getBody();
            if (val instanceof String) {
                this.digitalTwinStateManager
                        .updateProperty(new DigitalTwinStateProperty<>(event.getPhysicalPropertyId(), (String) val));
            } else if (val instanceof Integer) {
                this.digitalTwinStateManager
                        .updateProperty(new DigitalTwinStateProperty<>(event.getPhysicalPropertyId(), (Integer) val));
            }
            this.digitalTwinStateManager.commitStateTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPhysicalAssetEventNotification(PhysicalAssetEventWldtEvent<?> event) {
    }

    @Override
    protected void onPhysicalAssetRelationshipEstablished(PhysicalAssetRelationshipInstanceCreatedWldtEvent<?> event) {
        if (event == null || event.getBody() == null)
            return;
        try {
            var instance = event.getBody();
            this.digitalTwinStateManager.startStateTransaction();
            this.digitalTwinStateManager
                    .addRelationshipInstance(new it.wldt.core.state.DigitalTwinStateRelationshipInstance<>(
                            instance.getRelationship().getName(), (String) instance.getTargetId(), instance.getKey()));
            this.digitalTwinStateManager.commitStateTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPhysicalAssetRelationshipDeleted(PhysicalAssetRelationshipInstanceDeletedWldtEvent<?> event) {
        if (event == null || event.getBody() == null)
            return;
        try {
            var instance = event.getBody();
            this.digitalTwinStateManager.startStateTransaction();
            this.digitalTwinStateManager.deleteRelationshipInstance(instance.getRelationship().getName(),
                    instance.getKey());
            this.digitalTwinStateManager.commitStateTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDigitalActionEvent(DigitalActionWldtEvent<?> event) {
        if (event == null)
            return;
        String key = event.getActionKey();

        if (CentralEmergencyKeywords.ACTION_TRIAGE.equals(key)
                || CentralEmergencyKeywords.ACTION_REDIRECT.equals(key)) {
            try {
                this.publishPhysicalAssetActionWldtEvent(key, event.getBody());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void createDigitalTwinStateProperty(PhysicalAssetProperty<?> property) throws Exception {
        Object val = property.getInitialValue();
        if (val instanceof String) {
            this.digitalTwinStateManager
                    .createProperty(new DigitalTwinStateProperty<>(property.getKey(), (String) val));
        } else if (val instanceof Integer) {
            this.digitalTwinStateManager
                    .createProperty(new DigitalTwinStateProperty<>(property.getKey(), (Integer) val));
        }
    }
}