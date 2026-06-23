package it.ausl.emergency.adapter.physical;

import it.ausl.emergency.adapter.configuration.CentralEmergencyAdapterConfiguration;
import it.ausl.emergency.utils.CentralEmergencyKeywords;
import it.wldt.adapter.physical.*;
import it.wldt.adapter.physical.event.PhysicalAssetActionWldtEvent;

public class CentralEmergencyPhysicalAdapter extends ConfigurablePhysicalAdapter<CentralEmergencyAdapterConfiguration> {

    public CentralEmergencyPhysicalAdapter(String id, CentralEmergencyAdapterConfiguration configuration) {
        super(id, configuration);
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

        pad.getActions().add(new PhysicalAssetAction(CentralEmergencyKeywords.ACTION_TRIAGE, "centrale.triage", CentralEmergencyKeywords.CONTENT_TYPE_JSON));
        pad.getActions().add(new PhysicalAssetAction(CentralEmergencyKeywords.ACTION_REDIRECT, "centrale.redirect", CentralEmergencyKeywords.CONTENT_TYPE_JSON));

        pad.getRelationships().add(new PhysicalAssetRelationship<>(CentralEmergencyKeywords.REL_MONITORS_HOSPITAL, "hospital"));
        pad.getRelationships().add(new PhysicalAssetRelationship<>(CentralEmergencyKeywords.REL_MANAGES_VEHICLE, "vehicle"));
        pad.getRelationships().add(new PhysicalAssetRelationship<>(CentralEmergencyKeywords.REL_TRACKS_MISSION, "mission"));

        return pad;
    }

    @Override
    public void onIncomingPhysicalAction(PhysicalAssetActionWldtEvent<?> physicalActionEvent) {
        if (physicalActionEvent == null) return;
        String actionKey = physicalActionEvent.getActionKey();
        Object body = physicalActionEvent.getBody();

    }
}