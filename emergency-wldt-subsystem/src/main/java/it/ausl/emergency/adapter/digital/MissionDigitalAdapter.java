package it.ausl.emergency.adapter.digital;

import it.ausl.emergency.adapter.configuration.MissionAdapterConfiguration;
import it.ausl.emergency.utils.MissionKeywords;
import it.wldt.adapter.digital.DigitalAdapter;
import it.wldt.core.state.DigitalTwinState;
import it.wldt.core.state.DigitalTwinStateChange;
import it.wldt.core.state.DigitalTwinStateEventNotification;
import it.wldt.exception.WldtDigitalTwinStatePropertyException;

import java.util.ArrayList;

/**
 * Mission Digital Adapter.
 * Consumes and exposes synchronized aggregations of the rescue mission state,
 * including processed KPIs, clinical variations, and active DT operational relationships.
 */
public class MissionDigitalAdapter extends DigitalAdapter<MissionAdapterConfiguration> {

    public MissionDigitalAdapter(String id, MissionAdapterConfiguration configuration) {
        super(id, configuration);
    }

    @Override
    public void onAdapterStart() {
        System.out.println("[MissionDigitalAdapter] -> Digital twin exposure layer active for mission: " + getId());
    }

    @Override
    public void onAdapterStop() {
        System.out.println("[MissionDigitalAdapter] -> Digital layer terminated for mission: " + getId());
    }

    @Override
    public void onDigitalTwinSync(DigitalTwinState digitalTwinState) {
        System.out.println("[MissionDigitalAdapter] -> Synchronization achieved. Reading core twin state context...");
        System.out.println();
        System.out.println("[MissionDigitalAdapter] ── INITIAL SYNCHRONIZED AGGREGATE MISSION STATE ──");

        try {
            digitalTwinState.getProperty(MissionKeywords.STATE_PROPERTY_KEY)
                    .ifPresent(p -> System.out.println("   [PROPERTY] " + MissionKeywords.STATE_PROPERTY_KEY + "                 = " + p.getValue()));
            digitalTwinState.getProperty(MissionKeywords.SEVERITY_CODE_PROPERTY_KEY)
                    .ifPresent(p -> System.out.println("   [PROPERTY] " + MissionKeywords.SEVERITY_CODE_PROPERTY_KEY + "         = " + p.getValue()));
            digitalTwinState.getProperty(MissionKeywords.CONFIRMED_SEVERITY_PROPERTY_KEY)
                    .ifPresent(p -> System.out.println("   [PROPERTY] " + MissionKeywords.CONFIRMED_SEVERITY_PROPERTY_KEY + " = " + p.getValue()));
            digitalTwinState.getProperty(MissionKeywords.PATHOLOGY_PROPERTY_KEY)
                    .ifPresent(p -> System.out.println("   [PROPERTY] " + MissionKeywords.PATHOLOGY_PROPERTY_KEY + "             = " + p.getValue()));
            digitalTwinState.getProperty(MissionKeywords.PATIENT_ID_PROPERTY_KEY)
                    .ifPresent(p -> System.out.println("   [PROPERTY] " + MissionKeywords.PATIENT_ID_PROPERTY_KEY + "             = " + p.getValue()));
            digitalTwinState.getProperty(MissionKeywords.HOSPITAL_ID_PROPERTY_KEY)
                    .ifPresent(p -> System.out.println("   [PROPERTY] " + MissionKeywords.HOSPITAL_ID_PROPERTY_KEY + "            = " + p.getValue()));
            
            // Stampa delle metriche Augmented (KPI calcolati dalla Shadowing Function)
            digitalTwinState.getProperty(MissionKeywords.KPI_D09Z_PROPERTY_KEY)
                    .ifPresent(p -> System.out.println("   [AUGMENTED KPI] " + MissionKeywords.KPI_D09Z_PROPERTY_KEY + "           = " + p.getValue() + " s"));
            digitalTwinState.getProperty(MissionKeywords.KPI_TOTAL_DURATION_PROPERTY_KEY)
                    .ifPresent(p -> System.out.println("   [AUGMENTED KPI] " + MissionKeywords.KPI_TOTAL_DURATION_PROPERTY_KEY + " = " + p.getValue() + " s"));

        } catch (WldtDigitalTwinStatePropertyException e) {
            System.err.println("[MissionDigitalAdapter] Error reading properties on sync: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
    }

    @Override
    public void onDigitalTwinUnSync(DigitalTwinState digitalTwinState) {
        System.out.println("[MissionDigitalAdapter] -> Digital twin desynchronized from operational pipeline: " + getId());
    }

    @Override
    public void onDigitalTwinCreate() {}

    @Override
    public void onDigitalTwinStart() {}

    @Override
    public void onDigitalTwinStop() {}

    @Override
    public void onDigitalTwinDestroy() {}

    @Override
    protected void onStateUpdate(DigitalTwinState newDigitalTwinState, 
                                 DigitalTwinState previousDigitalTwinState, 
                                 ArrayList<DigitalTwinStateChange> digitalTwinStateChangeList) {
        System.out.println("[MissionDigitalAdapter] -> Dynamic state update transaction committed for mission: " + getId());
        
        // Monitoraggio atomico dei cambiamenti delle istanze di relazione sul log digitale
        if (digitalTwinStateChangeList != null) {
            digitalTwinStateChangeList.stream()
                .filter(change -> change.getResourceType() == DigitalTwinStateChange.ResourceType.RELATIONSHIP_INSTANCE)
                .forEach(change -> System.out.println("   [RELATIONSHIP UPDATE] Operation: " + change.getOperation() + " | Resource: " + change.getResource()));
        }
    }

    @Override
    protected void onEventNotificationReceived(DigitalTwinStateEventNotification<?> digitalTwinStateEventNotification) {
        if (digitalTwinStateEventNotification != null) {
            System.out.println("[MissionDigitalAdapter] -> Outbound Domain Event intercepted: " 
                    + digitalTwinStateEventNotification.getDigitalEventKey() 
                    + " | Body: " + digitalTwinStateEventNotification.getBody());
        }
    }
}