package it.ausl.emergency.shadowing;

import java.util.Map;

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

/**
 * Shadowing Function del Paziente: è il componente che traduce lo stato
 * dell'agente Paziente della
 * simulazione (ricevuto tramite il
 * {@link it.ausl.emergency.adapter.physical.patient.PatientPhysicalAdapter})
 * nello stato del Digital Twin, esposto poi verso l'esterno tramite i Digital
 * Adapter.
 *
 * Le proprietà rispecchiano 1:1 i campi di PatientTelemetryPayload (vedi
 * PatientKeywords), gli eventi
 * rispecchiano i Domain Events individuati nell'analisi DDD della tesi
 * (Riscontro Clinico Eseguito,
 * Deterioramento Clinico Rilevato, Handover Completato).
 */
public class PatientShadowingFunction extends ShadowingFunction {

    public PatientShadowingFunction(String id) {
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

    //// Bound LifeCycle State Management Callbacks ////

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
            e.printStackTrace();
        }
    }

    @Override
    protected void onDigitalTwinUnBound(Map<String, PhysicalAssetDescription> map, String detachReason) {
    }

    @Override
    protected void onPhysicalAdapterBidingUpdate(String adapterId, PhysicalAssetDescription physicalAssetDescription) {
    }

    //// Physical Property Variation Callback ////

    @Override
    protected void onPhysicalAssetPropertyVariation(PhysicalAssetPropertyWldtEvent<?> physicalAssetPropertyWldtEvent) {

        try {
            this.digitalTwinStateManager.startStateTransaction();

            updateDigitalTwinStateProperty(
                    physicalAssetPropertyWldtEvent.getPhysicalPropertyId(),
                    physicalAssetPropertyWldtEvent.getBody());

            this.digitalTwinStateManager.commitStateTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //// Physical Event Notification Callback ////

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
    protected void onPhysicalAssetRelationshipEstablished(
            PhysicalAssetRelationshipInstanceCreatedWldtEvent<?> physicalAssetRelationshipInstanceCreatedWldtEvent) {
    }

    @Override
    protected void onPhysicalAssetRelationshipDeleted(
            PhysicalAssetRelationshipInstanceDeletedWldtEvent<?> physicalAssetRelationshipInstanceDeletedWldtEvent) {
    }

    @Override
    protected void onDigitalActionEvent(DigitalActionWldtEvent<?> digitalActionWldtEvent) {
    }

    //// Helper Methods ////

    /**
     * Crea la DigitalTwinStateProperty con il tipo corretto a partire dal valore
     * iniziale dichiarato
     * nella PhysicalAssetProperty, dato che i campi del Paziente non sono tutti
     * dello stesso tipo.
     */
    private void createDigitalTwinStateProperty(PhysicalAssetProperty<?> property) throws Exception {

        Object initialValue = property.getInitialValue();

        if (initialValue instanceof Double) {
            this.digitalTwinStateManager
                    .createProperty(new DigitalTwinStateProperty<>(property.getKey(), (Double) initialValue));
        } else if (initialValue instanceof Integer) {
            this.digitalTwinStateManager
                    .createProperty(new DigitalTwinStateProperty<>(property.getKey(), (Integer) initialValue));
        } else if (initialValue instanceof Boolean) {
            this.digitalTwinStateManager
                    .createProperty(new DigitalTwinStateProperty<>(property.getKey(), (Boolean) initialValue));
        } else if (initialValue instanceof String) {
            this.digitalTwinStateManager
                    .createProperty(new DigitalTwinStateProperty<>(property.getKey(), (String) initialValue));
        } else {
            throw new IllegalArgumentException(
                    "PatientShadowingFunction Unsupported property type for key: " + property.getKey());
        }
    }

    /**
     * Aggiorna la DigitalTwinStateProperty con il tipo corretto a partire dal body
     * dell'evento fisico
     * di variazione, simmetrico a {@link #createDigitalTwinStateProperty}.
     */
    private void updateDigitalTwinStateProperty(String propertyKey, Object value) throws Exception {

        if (value instanceof Double) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(propertyKey, (Double) value));
        } else if (value instanceof Integer) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(propertyKey, (Integer) value));
        } else if (value instanceof Boolean) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(propertyKey, (Boolean) value));
        } else if (value instanceof String) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(propertyKey, (String) value));
        } else {
            throw new IllegalArgumentException(
                    "PatientShadowingFunction Unsupported value type for property key: " + propertyKey);
        }
    }

    public DigitalTwinStateManager getDigitalTwinStateManager() {
        return this.digitalTwinStateManager;
    }
}