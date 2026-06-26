package it.ausl.emergency.shadowing;

import it.ausl.emergency.utils.MissionKeywords;
import it.wldt.adapter.digital.event.DigitalActionWldtEvent;
import it.wldt.adapter.physical.PhysicalAssetDescription;
import it.wldt.adapter.physical.PhysicalAssetProperty;
import it.wldt.adapter.physical.event.PhysicalAssetEventWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceCreatedWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceDeletedWldtEvent;
import it.wldt.adapter.physical.PhysicalAssetRelationshipInstance;
import it.wldt.core.model.ShadowingFunction;
import it.wldt.core.state.*;

import java.util.Map;


public class MissionShadowingFunction extends ShadowingFunction {

    private volatile double cachedTimeCalled = 0.0;
    private volatile double cachedTimeOnScene = 0.0;
    private volatile double cachedTimeHandover = 0.0;

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
                pad.getProperties().forEach(property -> {
                    try {
                        createDigitalTwinStateProperty(property);
                        this.observePhysicalAssetProperty(property);
                        System.out.println(
                                "[MissionShadowingFunction] -> Property created & observed: " + property.getKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                pad.getEvents().forEach(event -> {
                    try {
                        DigitalTwinStateEvent dtEvent = new DigitalTwinStateEvent(event.getKey(), event.getType());
                        this.digitalTwinStateManager.registerEvent(dtEvent);
                        this.observePhysicalAssetEvent(event);
                        System.out.println(
                                "[MissionShadowingFunction] -> Event registered & observed: " + event.getKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                pad.getRelationships().forEach(rel -> {
                    try {
                        DigitalTwinStateRelationship<String> dtStateRelationship = new DigitalTwinStateRelationship<>(
                                rel.getName(), rel.getName());
                        this.digitalTwinStateManager.createRelationship(dtStateRelationship);

                        this.observePhysicalAssetRelationship(rel);
                        System.out
                                .println("[MissionShadowingFunction] -> Relationship structurally created & observed: "
                                        + rel.getName());
                    } catch (Exception e) {
                        System.err.println(
                                "[MissionShadowingFunction] Error creating structural relationship: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

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
    protected void onPhysicalAdapterBidingUpdate(String id, PhysicalAssetDescription pad) {
    }

    @Override
    protected void onPhysicalAssetPropertyVariation(PhysicalAssetPropertyWldtEvent<?> event) {
        try {
            String propertyId = event.getPhysicalPropertyId();
            Object body = event.getBody();

            this.digitalTwinStateManager.startStateTransaction();
            updateDigitalTwinStateProperty(propertyId, body);
            if (body instanceof Number num) {
                double val = num.doubleValue();
                if (val > 1_000_000_000_000.0) {
                    val = val / 1000.0;
                }
                String lowKey = propertyId.toLowerCase();

                if (lowKey.contains("called")) {
                    this.cachedTimeCalled = val;
                    recomputeKpis();
                } else if (lowKey.contains("scene") || lowKey.contains("onscene")) {
                    this.cachedTimeOnScene = val;
                    recomputeKpis();
                } else if (lowKey.contains("handover") || lowKey.contains("completed")
                        || lowKey.contains("timestamp")) {
                    this.cachedTimeHandover = val;
                    recomputeKpis();
                }
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

    @Override
    protected void onPhysicalAssetRelationshipEstablished(
            PhysicalAssetRelationshipInstanceCreatedWldtEvent<?> event) {
        try {
            // CORREZIONE: In WLDT l'istanza fisica si estrae invocando .getBody() sul
            // WldtEvent
            if (event == null || event.getBody() == null)
                return;

            PhysicalAssetRelationshipInstance<?> paRelInstance = event.getBody();
            String relName = paRelInstance.getRelationship().getName();
            String relKey = paRelInstance.getKey();
            String relTargetId = (String) paRelInstance.getTargetId();

            System.out.println("[MissionShadowingFunction] -> Relationship established: " + relName
                    + " | instance key: " + relKey + " | target DT: " + relTargetId);
            DigitalTwinStateRelationshipInstance<String> dtStateRelInstance = new DigitalTwinStateRelationshipInstance<>(
                    relName, relTargetId, relKey);

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
            if (event == null || event.getBody() == null)
                return;

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

    @Override
    protected void onDigitalActionEvent(DigitalActionWldtEvent<?> event) {
    }

    private void recomputeKpis() throws Exception {
        double kpiD09z = (cachedTimeOnScene > 0.0 && cachedTimeCalled > 0.0) ? cachedTimeOnScene - cachedTimeCalled
                : 0.0;
        double kpiTotal = (cachedTimeHandover > 0.0 && cachedTimeCalled > 0.0) ? cachedTimeHandover - cachedTimeCalled
                : 0.0;

        this.digitalTwinStateManager
                .updateProperty(new DigitalTwinStateProperty<>(MissionKeywords.KPI_D09Z_PROPERTY_KEY, kpiD09z));
        this.digitalTwinStateManager.updateProperty(
                new DigitalTwinStateProperty<>(MissionKeywords.KPI_TOTAL_DURATION_PROPERTY_KEY, kpiTotal));

        System.out.printf("[MissionShadowingFunction] -> KPIs updated | D09Z=%.1fs | TotalDuration=%.1fs%n", kpiD09z,
                kpiTotal);
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
        } else if (val instanceof Long l) {
            this.digitalTwinStateManager
                    .createProperty(new DigitalTwinStateProperty<>(property.getKey(), l.doubleValue()));
        }
    }

    private void updateDigitalTwinStateProperty(String key, Object val) throws Exception {
        if (val instanceof Double d) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, d));
        } else if (val instanceof Long l) {
            // Salva come Double per uniformità con il resto del sistema
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, l.doubleValue()));
        } else if (val instanceof Integer i) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, i));
        } else if (val instanceof Boolean b) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, b));
        } else if (val instanceof String s) {
            this.digitalTwinStateManager.updateProperty(new DigitalTwinStateProperty<>(key, s));
        }
    }

    public DigitalTwinStateManager getDigitalTwinStateManager() {
        return this.digitalTwinStateManager;
    }
}