package it.ausl.emergency.shadowing;

import it.ausl.emergency.utils.MedHelicopterKeywords;
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
 * Shadowing Function del MedHelicopter.
 *
 * Traduce lo stato fisico dell'elisoccorso nella rappresentazione
 * digitale esposta dal MedHelicopterDigitalAdapter.
 *
 * Segue lo stesso pattern di MedCarShadowingFunction:
 *  - Nessuna azione digitale (nessun redirect)
 *  - Helper createDigitalTwinStateProperty / updateDigitalTwinStateProperty
 *    con dispatch sui tipi Double, Integer, Boolean, String
 */
public class MedHelicopterShadowingFunction extends ShadowingFunction {

    public MedHelicopterShadowingFunction(String id) {
        super(id);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate() {
        System.out.println("[MedHelicopterShadowingFunction] -> onCreate()");
    }

    @Override
    protected void onStart() {
        System.out.println("[MedHelicopterShadowingFunction] -> onStart()");
    }

    @Override
    protected void onStop() {
        System.out.println("[MedHelicopterShadowingFunction] -> onStop()");
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    @Override
    protected void onDigitalTwinBound(Map<String, PhysicalAssetDescription> adaptersMap) {
        try {
            System.out.println("[MedHelicopterShadowingFunction] -> onDigitalTwinBound()");
            this.digitalTwinStateManager.startStateTransaction();

            adaptersMap.values().forEach(pad -> {

                // Proprietà
                pad.getProperties().forEach(property -> {
                    try {
                        createDigitalTwinStateProperty(property);
                        this.observePhysicalAssetProperty(property);
                        System.out.println("[MedHelicopterShadowingFunction] -> Property created & observed: "
                                + property.getKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                // Events
                pad.getEvents().forEach(event -> {
                    try {
                        DigitalTwinStateEvent dtEvent =
                                new DigitalTwinStateEvent(event.getKey(), event.getType());
                        this.digitalTwinStateManager.registerEvent(dtEvent);
                        this.observePhysicalAssetEvent(event);
                        System.out.println("[MedHelicopterShadowingFunction] -> Event registered & observed: "
                                + event.getKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                // Nessuna azione nella PAD del MedHelicopter
                pad.getActions().forEach(action -> {
                    try {
                        it.wldt.core.state.DigitalTwinStateAction dtAction =
                                new it.wldt.core.state.DigitalTwinStateAction(
                                        action.getKey(), action.getType(), action.getContentType());
                        this.digitalTwinStateManager.enableAction(dtAction);
                        System.out.println("[MedHelicopterShadowingFunction] -> Action enabled: "
                                + action.getKey());
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
        System.out.println("[MedHelicopterShadowingFunction] -> onDigitalTwinUnBound(): " + reason);
    }

    @Override
    protected void onPhysicalAdapterBidingUpdate(String id, PhysicalAssetDescription pad) {
        // PAD statica per tutta la missione
    }

    // ── Property Variation ────────────────────────────────────────────────────

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

    // ── Relationships (no-op) ─────────────────────────────────────────────────

    @Override
    protected void onPhysicalAssetRelationshipEstablished(
            PhysicalAssetRelationshipInstanceCreatedWldtEvent<?> e) {}

    @Override
    protected void onPhysicalAssetRelationshipDeleted(
            PhysicalAssetRelationshipInstanceDeletedWldtEvent<?> e) {}

    // ── Digital Action (no-op con log) ────────────────────────────────────────

    @Override
    protected void onDigitalActionEvent(DigitalActionWldtEvent<?> event) {
        System.out.println("[MedHelicopterShadowingFunction] -> Unsupported digital action: "
                + (event != null ? event.getActionKey() : "null"));
    }

    // ── Helper Methods ────────────────────────────────────────────────────────

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
                    "[MedHelicopterShadowingFunction] Unsupported property type for key: "
                            + property.getKey());
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
                    "[MedHelicopterShadowingFunction] Unsupported value type for key: " + key);
        }
    }
}
