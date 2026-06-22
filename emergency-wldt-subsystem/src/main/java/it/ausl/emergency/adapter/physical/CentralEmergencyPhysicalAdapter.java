package it.ausl.emergency.adapter.physical;

import it.ausl.emergency.adapter.configuration.CentraleOperativaAdapterConfiguration;
import it.ausl.emergency.utils.CentraleOperativaKeywords;
import it.wldt.adapter.physical.*;
import it.wldt.adapter.physical.event.PhysicalAssetActionWldtEvent;

public class CentralEmergencyPhysicalAdapter extends ConfigurablePhysicalAdapter<CentraleOperativaAdapterConfiguration> {

    public CentralEmergencyPhysicalAdapter(String id, CentraleOperativaAdapterConfiguration configuration) {
        super(id, configuration);
    }

    @Override
    public void onAdapterStart() {
        try {
            System.out.println("[CentraleOperativaPhysicalAdapter] -> Avvio dell'adapter fisico strutturale della Centrale.");
            notifyPhysicalAdapterBound(buildPhysicalAssetDescription());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAdapterStop() {
        System.out.println("[CentraleOperativaPhysicalAdapter] -> Canale di ingestione interrotto.");
    }

    private PhysicalAssetDescription buildPhysicalAssetDescription() {
        PhysicalAssetDescription pad = new PhysicalAssetDescription();

        // 1. Registrazione Proprietà Strutturali
        pad.getProperties().add(new PhysicalAssetProperty<>(CentraleOperativaKeywords.PROPERTY_STATUS, getConfiguration().getInitialStatus()));
        pad.getProperties().add(new PhysicalAssetProperty<>(CentraleOperativaKeywords.PROPERTY_ACTIVE_MISSIONS, getConfiguration().getDefaultActiveMissions()));

        // 2. Azioni Closed-Loop (Triage e Redirection) verso il simulatore
        pad.getActions().add(new PhysicalAssetAction(CentraleOperativaKeywords.ACTION_TRIAGE, "centrale.triage", CentraleOperativaKeywords.CONTENT_TYPE_JSON));
        pad.getActions().add(new PhysicalAssetAction(CentraleOperativaKeywords.ACTION_REDIRECT, "centrale.redirect", CentraleOperativaKeywords.CONTENT_TYPE_JSON));

        // 3. Relazioni Semantiche (Esclusi i punti stazionari come da modello DDD definitivo)
        pad.getRelationships().add(new PhysicalAssetRelationship<>(CentraleOperativaKeywords.REL_MONITORS_HOSPITAL, "hospital"));
        pad.getRelationships().add(new PhysicalAssetRelationship<>(CentraleOperativaKeywords.REL_MANAGES_VEHICLE, "vehicle"));
        pad.getRelationships().add(new PhysicalAssetRelationship<>(CentraleOperativaKeywords.REL_TRACKS_MISSION, "mission"));

        return pad;
    }

    @Override
    public void onIncomingPhysicalAction(PhysicalAssetActionWldtEvent<?> physicalActionEvent) {
        if (physicalActionEvent == null) return;
        
        String actionKey = physicalActionEvent.getActionKey();
        Object body = physicalActionEvent.getBody();

        // Inoltro dei comandi closed-loop verso il motore di simulazione/broker
        System.out.printf("[CentraleOperativaPhysicalAdapter] -> Inoltro azione all'infrastruttura reale: %s | Contenuto: %s%n", actionKey, body);
    }
}