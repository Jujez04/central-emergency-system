package it.ausl.emergency.shadowing;

import java.util.Map;

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

/**
 * Shadowing Function dell'Ambulanza.
 *
 * Traduce lo stato dell'agente Ambulanza della simulazione AnyLogic
 * (ricevuto tramite AmbulancePhysicalAdapter) nello stato del Digital Twin.
 *
 * Le proprietà operative rispecchiano 1:1 i campi di AmbulanceTelemetryPayload.
 * I Domain Events rispecchiano le transizioni operative significative identificate
 * nell'analisi DDD (missione assegnata, paziente preso in carico, handover, ecc.).
 *
 * La Shadowing Function è stateless rispetto alla logica di dominio: il rilevamento
 * dei fronti di transizione è già gestito dall'AmbulancePhysicalAdapter.
 */
public class AmbulanceShadowingFunction extends ShadowingFunction {

    public AmbulanceShadowingFunction(String id) {
        super(id);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate() {
        System.out.println("[AmbulanceShadowingFunction] -> onCreate()");
    }

    @Override
    protected void onStart() {
        System.out.println("[AmbulanceShadowingFunction] -> onStart()");
    }

    @Override
    protected void onStop() {
        System.out.println("[AmbulanceShadowingFunction] -> onStop()");
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    @Override
    protected void onDigitalTwinBound(
            Map<String, PhysicalAssetDescription> adaptersPhysicalAssetDescriptionMap) {
        try {
            System.out.println("[AmbulanceShadowingFunction] -> onDigitalTwinBound()");

            this.digitalTwinStateManager.startStateTransaction();

            adaptersPhysicalAssetDescriptionMap.values().forEach(pad -> {

                // Creazione e osservazione proprietà operative
                pad.getProperties().forEach(property -> {
                    try {
                        createDigitalTwinStateProperty(property);
                        this.observePhysicalAssetProperty(property);
                        System.out.println("[AmbulanceShadowingFunction] -> Property Created & Observed: "
                                + property.getKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                // Registrazione e osservazione Domain Events
                pad.getEvents().forEach(event -> {
                    try {
                        DigitalTwinStateEvent dtStateEvent =
                                new DigitalTwinStateEvent(event.getKey(), event.getType());
                        this.digitalTwinStateManager.registerEvent(dtStateEvent);
                        this.observePhysicalAssetEvent(event);
                        System.out.println("[AmbulanceShadowingFunction] -> Event Registered & Observed: "
                                + event.getKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                // Nessuna azione fisica prevista — blocco lasciato per estendibilità
                pad.getActions().forEach(action -> {
                    try {
                        it.wldt.core.state.DigitalTwinStateAction dtStateAction =
                                new it.wldt.core.state.DigitalTwinStateAction(
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
    protected void onDigitalTwinUnBound(Map<String, PhysicalAssetDescription> map, String reason) {
        System.out.println("[AmbulanceShadowingFunction] -> onDigitalTwinUnBound(): " + reason);
    }

    @Override
    protected void onPhysicalAdapterBidingUpdate(String adapterId,
            PhysicalAssetDescription physicalAssetDescription) {
        // PAD dell'ambulanza è statica: non gestito
    }

    // ── Property Variation ────────────────────────────────────────────────────

    @Override
    protected void onPhysicalAssetPropertyVariation(
            PhysicalAssetPropertyWldtEvent<?> physicalAssetPropertyWldtEvent) {
        try {
            this.digitalTwinStateManager.startStateTransaction();
            updateDigitalTwinStateProperty(
                    physicalAssetPropertyWldtEvent.getPhysicalPropertyId(),
                    physicalAssetPropertyWldtEvent.getBody());
            this.digitalTwinStateManager.commitStateTransaction();

            System.out.println("[AmbulanceShadowingFunction] -> Property updated: "
                    + physicalAssetPropertyWldtEvent.getPhysicalPropertyId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Event Notification ────────────────────────────────────────────────────

    @Override
    protected void onPhysicalAssetEventNotification(
            PhysicalAssetEventWldtEvent<?> physicalAssetEventWldtEvent) {
        try {
            System.out.println("[AmbulanceShadowingFunction] -> Domain Event received: "
                    + physicalAssetEventWldtEvent.getPhysicalEventKey());

            this.digitalTwinStateManager.notifyDigitalTwinStateEvent(
                    new DigitalTwinStateEventNotification<>(
                            physicalAssetEventWldtEvent.getPhysicalEventKey(),
                            physicalAssetEventWldtEvent.getBody(),
                            physicalAssetEventWldtEvent.getCreationTimestamp()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Relationships (non usate) ─────────────────────────────────────────────

    @Override
    protected void onPhysicalAssetRelationshipEstablished(
            PhysicalAssetRelationshipInstanceCreatedWldtEvent<?> event) {}

    @Override
    protected void onPhysicalAssetRelationshipDeleted(
            PhysicalAssetRelationshipInstanceDeletedWldtEvent<?> event) {}

    // ── Digital Actions (non usate) ───────────────────────────────────────────

    @Override
    protected void onDigitalActionEvent(DigitalActionWldtEvent<?> digitalActionWldtEvent) {
        System.out.println("[AmbulanceShadowingFunction] -> Azione digitale non supportata: "
                + (digitalActionWldtEvent != null ? digitalActionWldtEvent.getActionKey() : "null"));
    }

    // ── Getter per i test ─────────────────────────────────────────────────────

    public DigitalTwinStateManager getDigitalTwinStateManager() {
        return this.digitalTwinStateManager;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Crea la DigitalTwinStateProperty con il tipo corretto in base al valore
     * iniziale dichiarato nella PAD. Le proprietà dell'ambulanza includono
     * String, Double, Integer e Boolean.
     */
    private void createDigitalTwinStateProperty(PhysicalAssetProperty<?> property) throws Exception {
        Object v = property.getInitialValue();
        if (v instanceof Double) {
            this.digitalTwinStateManager.createProperty(
                    new DigitalTwinStateProperty<>(property.getKey(), (Double) v));
        } else if (v instanceof Integer) {
            this.digitalTwinStateManager.createProperty(
                    new DigitalTwinStateProperty<>(property.getKey(), (Integer) v));
        } else if (v instanceof Boolean) {
            this.digitalTwinStateManager.createProperty(
                    new DigitalTwinStateProperty<>(property.getKey(), (Boolean) v));
        } else if (v instanceof String) {
            this.digitalTwinStateManager.createProperty(
                    new DigitalTwinStateProperty<>(property.getKey(), (String) v));
        } else {
            throw new IllegalArgumentException(
                    "[AmbulanceShadowingFunction] Tipo non supportato per: " + property.getKey());
        }
    }

    /**
     * Aggiorna la DigitalTwinStateProperty con il tipo corretto,
     * simmetrico a {@link #createDigitalTwinStateProperty}.
     */
    private void updateDigitalTwinStateProperty(String propertyKey, Object value) throws Exception {
        if (value instanceof Double) {
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(propertyKey, (Double) value));
        } else if (value instanceof Integer) {
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(propertyKey, (Integer) value));
        } else if (value instanceof Boolean) {
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(propertyKey, (Boolean) value));
        } else if (value instanceof String) {
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(propertyKey, (String) value));
        } else {
            throw new IllegalArgumentException(
                    "[AmbulanceShadowingFunction] Tipo non supportato per update: " + propertyKey);
        }
    }
}