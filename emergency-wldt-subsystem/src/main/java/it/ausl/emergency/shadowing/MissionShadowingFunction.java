package it.ausl.emergency.shadowing;

import it.ausl.emergency.utils.MissionKeywords;
import it.wldt.adapter.digital.event.DigitalActionWldtEvent;
import it.wldt.adapter.physical.PhysicalAssetDescription;
import it.wldt.adapter.physical.PhysicalAssetProperty;
import it.wldt.adapter.physical.PhysicalAssetRelationship;
import it.wldt.adapter.physical.event.PhysicalAssetEventWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceCreatedWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceDeletedWldtEvent;
import it.wldt.adapter.physical.PhysicalAssetRelationshipInstance;
import it.wldt.core.model.ShadowingFunction;
import it.wldt.core.state.*;

import java.util.Map;

/**
 * Shadowing Function della Missione Corretta.
 */
public class MissionShadowingFunction extends ShadowingFunction {

    // Valori timestamp locali per il calcolo incrementale dei KPI
    private volatile double cachedTimeCalled    = 0.0;
    private volatile double cachedTimeOnScene   = 0.0;
    private volatile double cachedTimeHandover  = 0.0;

    public MissionShadowingFunction(String id) {
        super(id);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate() {
        System.out.println("[MissionShadowingFunction] -> onCreate()");
    }

    @Override
    protected void onStart() {
        System.out.println("[MissionShadowingFunction] -> onStart()");
    }

    @Override
    protected void onStop() {
        System.out.println("[MissionShadowingFunction] -> onStop()");
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    @Override
    protected void onDigitalTwinBound(Map<String, PhysicalAssetDescription> adaptersMap) {
        try {
            System.out.println("[MissionShadowingFunction] -> onDigitalTwinBound(): binding mission state...");
            this.digitalTwinStateManager.startStateTransaction();

            adaptersMap.values().forEach(pad -> {

                // 1. Proprietà fisiche + proprietà augmented (KPI)
                pad.getProperties().forEach(property -> {
                    try {
                        createDigitalTwinStateProperty(property);
                        this.observePhysicalAssetProperty(property);
                        System.out.println("[MissionShadowingFunction] -> Property created & observed: " + property.getKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                // 2. Domain Events
                pad.getEvents().forEach(event -> {
                    try {
                        DigitalTwinStateEvent dtEvent = new DigitalTwinStateEvent(event.getKey(), event.getType());
                        this.digitalTwinStateManager.registerEvent(dtEvent);
                        this.observePhysicalAssetEvent(event);
                        System.out.println("[MissionShadowingFunction] -> Event registered & observed: " + event.getKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                // 3. CORREZIONE: Creazione della Relazione Semantica nello Stato Digitale
                pad.getRelationships().forEach(rel -> {
                    try {
                        // Creiamo la relazione strutturale nel DigitalTwinState prima di osservarla
                        DigitalTwinStateRelationship<String> dtStateRelationship = 
                                new DigitalTwinStateRelationship<>(rel.getName(), rel.getName());
                        this.digitalTwinStateManager.createRelationship(dtStateRelationship);
                        
                        this.observePhysicalAssetRelationship(rel);
                        System.out.println("[MissionShadowingFunction] -> Relationship structurally created & observed: " + rel.getName());
                    } catch (Exception e) {
                        System.err.println("[MissionShadowingFunction] Error creating structural relationship: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

                // 4. Azioni
                pad.getActions().forEach(action -> {
                    try {
                        DigitalTwinStateAction dtAction = new DigitalTwinStateAction(
                                action.getKey(), action.getType(), action.getContentType());
                        this.digitalTwinStateManager.enableAction(dtAction);
                        System.out.println("[MissionShadowingFunction] -> Action enabled: " + action.getKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });

            this.digitalTwinStateManager.commitStateTransaction();
            observeDigitalActionEvents();
            notifyShadowingSync();

        } catch (Exception e) {
            System.err.println("[MissionShadowingFunction] Error in onDigitalTwinBound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDigitalTwinUnBound(Map<String, PhysicalAssetDescription> map, String reason) {
        System.out.println("[MissionShadowingFunction] -> onDigitalTwinUnBound(): " + reason);
    }

    @Override
    protected void onPhysicalAdapterBidingUpdate(String id, PhysicalAssetDescription pad) {}

    // ── Property Variation ────────────────────────────────────────────────────

    @Override
    protected void onPhysicalAssetPropertyVariation(PhysicalAssetPropertyWldtEvent<?> event) {
        try {
            String propertyId = event.getPhysicalPropertyId();
            Object body       = event.getBody();

            this.digitalTwinStateManager.startStateTransaction();
            updateDigitalTwinStateProperty(propertyId, body);

            // Augmentation KPI
            if (MissionKeywords.TIME_CALLED_PROPERTY_KEY.equals(propertyId) && body instanceof Double) {
                cachedTimeCalled = (Double) body;
                recomputeKpis();
            } else if (MissionKeywords.TIME_ON_SCENE_PROPERTY_KEY.equals(propertyId) && body instanceof Double) {
                cachedTimeOnScene = (Double) body;
                recomputeKpis();
            } else if (MissionKeywords.TIME_HANDOVER_PROPERTY_KEY.equals(propertyId) && body instanceof Double) {
                cachedTimeHandover = (Double) body;
                recomputeKpis();
            }

            this.digitalTwinStateManager.commitStateTransaction();

        } catch (Exception e) {
            System.err.println("[MissionShadowingFunction] Property variation error: " + e.getMessage());
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
            System.out.println("[MissionShadowingFunction] -> Domain event forwarded: " + event.getPhysicalEventKey());
        } catch (Exception e) {
            System.err.println("[MissionShadowingFunction] Event notification error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── CORREZIONE: Relationship Lifecycle (Uso corretto di .getBody()) ──────

    @Override
    protected void onPhysicalAssetRelationshipEstablished(
            PhysicalAssetRelationshipInstanceCreatedWldtEvent<?> event) {
        try {
            // CORREZIONE: In WLDT l'istanza fisica si estrae invocando .getBody() sul WldtEvent
            if (event == null || event.getBody() == null) return;

            PhysicalAssetRelationshipInstance<?> paRelInstance = event.getBody();
            String relName = paRelInstance.getRelationship().getName();
            String relKey = paRelInstance.getKey();
            String relTargetId = (String) paRelInstance.getTargetId();

            System.out.println("[MissionShadowingFunction] -> Relationship established: " + relName
                    + " | instance key: " + relKey + " | target DT: " + relTargetId);

            // Creazione dell'istanza digitale corrispondente nello stato di WLDT
            DigitalTwinStateRelationshipInstance<String> dtStateRelInstance = 
                    new DigitalTwinStateRelationshipInstance<>(relName, relTargetId, relKey);

            this.digitalTwinStateManager.startStateTransaction();
            this.digitalTwinStateManager.addRelationshipInstance(dtStateRelInstance);
            this.digitalTwinStateManager.commitStateTransaction();

        } catch (Exception e) {
            System.err.println("[MissionShadowingFunction] Relationship established error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onPhysicalAssetRelationshipDeleted(
            PhysicalAssetRelationshipInstanceDeletedWldtEvent<?> event) {
        try {
            // CORREZIONE: Estratto tramite .getBody() in simmetria con il metodo sopra
            if (event == null || event.getBody() == null) return;

            PhysicalAssetRelationshipInstance<?> paRelInstance = event.getBody();
            String relName = paRelInstance.getRelationship().getName();
            String relKey = paRelInstance.getKey();

            System.out.println("[MissionShadowingFunction] -> Relationship deleted: " + relName + " | key: " + relKey);

            this.digitalTwinStateManager.startStateTransaction();
            this.digitalTwinStateManager.deleteRelationshipInstance(relName, relKey);
            this.digitalTwinStateManager.commitStateTransaction();

        } catch (Exception e) {
            System.err.println("[MissionShadowingFunction] Relationship deleted error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Digital Action (Reroute Hospital) ─────────────────────────────────────

    @Override
    protected void onDigitalActionEvent(DigitalActionWldtEvent<?> event) {
        if (event == null) return;

        String actionKey = event.getActionKey();
        System.out.println("[MissionShadowingFunction] -> Digital action received: " + actionKey);

        if (MissionKeywords.ACTION_REROUTE_HOSPITAL.equals(actionKey)) {
            try {
                this.publishPhysicalAssetActionWldtEvent(actionKey, event.getBody());
                System.out.println("[MissionShadowingFunction] -> Reroute action forwarded to Physical Adapter. Target: " + event.getBody());
            } catch (Exception e) {
                System.err.println("[MissionShadowingFunction] Reroute action delivery failure: " + e.getMessage());
            }
        } else {
            System.out.println("[MissionShadowingFunction] -> Unsupported action key: " + actionKey);
        }
    }

    // ── KPI Augmentation ──────────────────────────────────────────────────────

    private void recomputeKpis() throws Exception {
        double kpiD09z = (cachedTimeOnScene > 0.0 && cachedTimeCalled > 0.0) ? cachedTimeOnScene - cachedTimeCalled : 0.0;
        double kpiTotal = (cachedTimeHandover > 0.0 && cachedTimeCalled > 0.0) ? cachedTimeHandover - cachedTimeCalled : 0.0;

        this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(MissionKeywords.KPI_D09Z_PROPERTY_KEY, kpiD09z));
        this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(MissionKeywords.KPI_TOTAL_DURATION_PROPERTY_KEY, kpiTotal));

        System.out.printf("[MissionShadowingFunction] -> KPIs updated | D09Z=%.1fs | TotalDuration=%.1fs%n", kpiD09z, kpiTotal);
    }

    // ── Type-safe Property Helpers ────────────────────────────────────────────

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