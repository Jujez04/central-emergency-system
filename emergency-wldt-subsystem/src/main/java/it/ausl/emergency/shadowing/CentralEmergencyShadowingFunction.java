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
import it.wldt.core.state.DigitalTwinStateAction;
import it.wldt.core.state.DigitalTwinStateManager;
import it.wldt.core.state.DigitalTwinStateProperty;
import it.wldt.core.state.DigitalTwinStateRelationship;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CentralEmergencyShadowingFunction extends ShadowingFunction {

    private volatile double d09zSum = 0.0;
    private final AtomicInteger d09zSamples = new AtomicInteger(0);
    private final AtomicInteger missionsCompleted = new AtomicInteger(0);
    private final AtomicInteger overTriageCount = new AtomicInteger(0);
    private final AtomicInteger underTriageCount = new AtomicInteger(0);
    private final AtomicInteger triageTotalAssessed = new AtomicInteger(0);
    private final AtomicInteger vehiclesNeedingMaintenance = new AtomicInteger(0);
    private final AtomicInteger vehiclesLowFuel = new AtomicInteger(0);
    private final AtomicInteger activeMissions = new AtomicInteger(0);
    private final AtomicInteger availableVehicles = new AtomicInteger(0);

    private static int severityRank(String severity) {
        if (severity == null)
            return -1;
        return switch (severity.toUpperCase()) {
            case "WHITE" -> 0;
            case "GREEN" -> 1;
            case "YELLOW" -> 2;
            case "RED" -> 3;
            default -> -1;
        };
    }

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
                        this.digitalTwinStateManager.enableAction(new DigitalTwinStateAction(
                                action.getKey(), action.getType(), action.getContentType()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });

            registerAugmentedKpiProperties();

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
            String propId = event.getPhysicalPropertyId();
            Object val = event.getBody();

            this.digitalTwinStateManager.startStateTransaction();

            if (val instanceof String s) {
                this.digitalTwinStateManager.updateProperty(
                        new DigitalTwinStateProperty<>(propId, s));
            } else if (val instanceof Integer i) {
                this.digitalTwinStateManager.updateProperty(
                        new DigitalTwinStateProperty<>(propId, i));
            }
            updateFleetKpisIfVehicleProperty(propId, val);
            this.digitalTwinStateManager.commitStateTransaction();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateFleetKpisIfVehicleProperty(String propId, Object val) throws Exception {
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
    }

    public DigitalTwinStateManager getDigitalTwinStateManager() {
        return this.digitalTwinStateManager;
    }

    private void registerAugmentedKpiProperties() throws Exception {
        this.digitalTwinStateManager.createProperty(
                new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_AVG_D09Z, 0.0));
        this.digitalTwinStateManager.createProperty(
                new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_MISSIONS_COMPLETED, 0));
        this.digitalTwinStateManager.createProperty(
                new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_VEHICLES_NEEDING_MAINTENANCE, 0));
        this.digitalTwinStateManager.createProperty(
                new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_VEHICLES_LOW_FUEL, 0));
        this.digitalTwinStateManager.createProperty(
                new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_SATURATION_SCORE, 0.0));
        this.digitalTwinStateManager.createProperty(
                new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_OVER_TRIAGE_COUNT, 0));
        this.digitalTwinStateManager.createProperty(
                new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_UNDER_TRIAGE_COUNT, 0));
        this.digitalTwinStateManager.createProperty(
                new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_TRIAGE_TOTAL_ASSESSED, 0));
    }

    public synchronized void onFleetSnapshot(int needingMaintenance, int lowFuel,
            int activeMissCount, int availableVehicles) {
        this.vehiclesNeedingMaintenance.set(needingMaintenance);
        this.vehiclesLowFuel.set(lowFuel);
        this.activeMissions.set(activeMissCount);
        this.availableVehicles.set(availableVehicles);

        double saturation = (availableVehicles > 0)
                ? Math.min(1.0, (double) activeMissCount / availableVehicles)
                : (activeMissCount > 0 ? 1.0 : 0.0);

        try {
            this.digitalTwinStateManager.startStateTransaction();
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_VEHICLES_NEEDING_MAINTENANCE,
                            needingMaintenance));
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_VEHICLES_LOW_FUEL, lowFuel));
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_SATURATION_SCORE, saturation));
            this.digitalTwinStateManager.commitStateTransaction();
        } catch (Exception e) {
            System.err
                    .println("[CentralEmergencyShadowingFunction] Errore aggiornamento KPI flotta: " + e.getMessage());
        }
    }

    public synchronized void onMissionCompleted(double d09zSeconds,
            String severityCode,
            String confirmedSeverity) {
        // ── D09Z medio ────────────────────────────────────────────────────────
        int completed = missionsCompleted.incrementAndGet();

        try {
            this.digitalTwinStateManager.startStateTransaction();
            this.digitalTwinStateManager.updateProperty(
                    new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_MISSIONS_COMPLETED, completed));
            if (d09zSeconds > 0.0) {
                d09zSum += d09zSeconds;
                int samples = d09zSamples.incrementAndGet();
                double avg = d09zSum / samples;

                this.digitalTwinStateManager.updateProperty(
                        new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_AVG_D09Z, avg));
            }

            this.digitalTwinStateManager.commitStateTransaction();
        } catch (Exception e) {
            System.err.println(
                    "[CentralEmergencyShadowingFunction] Errore aggiornamento KPI completate/D09Z: " + e.getMessage());
        }

        boolean validTriage = severityRank(severityCode) >= 0;
        boolean validConfirmedTriage = confirmedSeverity != null
                && !"null".equalsIgnoreCase(confirmedSeverity)
                && severityRank(confirmedSeverity) >= 0;

        if (validTriage && validConfirmedTriage) {
            int assessed = triageTotalAssessed.incrementAndGet();
            int rankTriage = severityRank(severityCode);
            int rankConfirmed = severityRank(confirmedSeverity);

            int over = overTriageCount.get();
            int under = underTriageCount.get();

            if (rankTriage > rankConfirmed) {
                over = overTriageCount.incrementAndGet();
            } else if (rankTriage < rankConfirmed) {
                under = underTriageCount.incrementAndGet();
            }

            final int finalOver = over;
            final int finalUnder = under;

            try {
                this.digitalTwinStateManager.startStateTransaction();
                this.digitalTwinStateManager.updateProperty(
                        new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_OVER_TRIAGE_COUNT, finalOver));
                this.digitalTwinStateManager.updateProperty(
                        new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_UNDER_TRIAGE_COUNT, finalUnder));
                this.digitalTwinStateManager.updateProperty(
                        new DigitalTwinStateProperty<>(CentralEmergencyKeywords.KPI_TRIAGE_TOTAL_ASSESSED, assessed));
                this.digitalTwinStateManager.commitStateTransaction();
            } catch (Exception e) {
                System.err.println(
                        "[CentralEmergencyShadowingFunction] Errore aggiornamento KPI triage: " + e.getMessage());
            }
        }
    }

    public double getAvgD09z() {
        return d09zSamples.get() > 0 ? d09zSum / d09zSamples.get() : 0.0;
    }

    public int getMissionsCompleted() {
        return missionsCompleted.get();
    }

    public int getVehiclesNeedingMaintenance() {
        return vehiclesNeedingMaintenance.get();
    }

    public int getVehiclesLowFuel() {
        return vehiclesLowFuel.get();
    }

    public double getSaturationScore() {
        int avail = availableVehicles.get();
        int active = activeMissions.get();
        return (avail > 0) ? Math.min(1.0, (double) active / avail) : (active > 0 ? 1.0 : 0.0);
    }

    public int getOverTriageCount() {
        return overTriageCount.get();
    }

    public int getUnderTriageCount() {
        return underTriageCount.get();
    }

    public int getTriageTotalAssessed() {
        return triageTotalAssessed.get();
    }

    private void createDigitalTwinStateProperty(PhysicalAssetProperty<?> property) throws Exception {
        Object val = property.getInitialValue();
        if (val instanceof String s) {
            this.digitalTwinStateManager.createProperty(new DigitalTwinStateProperty<>(property.getKey(), s));
        } else if (val instanceof Integer i) {
            this.digitalTwinStateManager.createProperty(new DigitalTwinStateProperty<>(property.getKey(), i));
        } else if (val instanceof Double d) {
            this.digitalTwinStateManager.createProperty(new DigitalTwinStateProperty<>(property.getKey(), d));
        } else if (val instanceof Boolean b) {
            this.digitalTwinStateManager.createProperty(new DigitalTwinStateProperty<>(property.getKey(), b));
        }
    }
}