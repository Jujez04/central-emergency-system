package it.ausl.emergency.adapter.physical;

import it.ausl.emergency.adapter.configuration.CentralEmergencyAdapterConfiguration;
import it.ausl.emergency.utils.CentralEmergencyKeywords;
import it.wldt.adapter.physical.*;
import it.wldt.adapter.physical.event.PhysicalAssetActionWldtEvent;

public class CentralEmergencyPhysicalAdapter extends ConfigurablePhysicalAdapter<CentralEmergencyAdapterConfiguration> {

    public CentralEmergencyPhysicalAdapter(String id, CentralEmergencyAdapterConfiguration configuration) {
        super(id, configuration);
        System.out.println("[DEBUG ADAPTER] Adapter inizializzato con ID: " + id);
        if (id == null) {
            System.err.println("[ERRORE CRITICO] L'ID passato all'adapter è NULL!");
        }
    }

    @Override
    public void onAdapterStart() {
        try {
            notifyPhysicalAdapterBound(buildPhysicalAssetDescription());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAdapterStop() {
    }

    private PhysicalAssetDescription buildPhysicalAssetDescription() {
        PhysicalAssetDescription pad = new PhysicalAssetDescription();

        pad.getProperties().add(new PhysicalAssetProperty<>(CentralEmergencyKeywords.PROPERTY_STATUS, getConfiguration().getInitialStatus()));
        pad.getProperties().add(new PhysicalAssetProperty<>(CentralEmergencyKeywords.PROPERTY_ACTIVE_MISSIONS, getConfiguration().getDefaultActiveMissions()));
        pad.getRelationships().add(new PhysicalAssetRelationship<>(CentralEmergencyKeywords.REL_MONITORS_HOSPITAL, "hospital"));
        pad.getRelationships().add(new PhysicalAssetRelationship<>(CentralEmergencyKeywords.REL_MANAGES_VEHICLE, "vehicle"));
        pad.getRelationships().add(new PhysicalAssetRelationship<>(CentralEmergencyKeywords.REL_TRACKS_MISSION, "mission"));

        return pad;
    }

    @Override
    public void onIncomingPhysicalAction(PhysicalAssetActionWldtEvent<?> physicalActionEvent) {
    }
}