package it.ausl.emergency.adapter.digital;

import it.ausl.emergency.adapter.configuration.HospitalAdapterConfiguration;
import it.ausl.emergency.utils.HospitalKeywords;
import it.wldt.adapter.digital.DigitalAdapter;
import it.wldt.core.state.DigitalTwinState;
import it.wldt.core.state.DigitalTwinStateChange;
import it.wldt.core.state.DigitalTwinStateEventNotification;
import it.wldt.exception.WldtDigitalTwinStatePropertyException;

import java.util.ArrayList;

/**
 * Hospital Digital Adapter.
 * Consumes and exposes synchronized core digital twin states of the hospital
 * infrastructure to external consumers, monitoring layers or analytical services.
 */
public class HospitalDigitalAdapter extends DigitalAdapter<HospitalAdapterConfiguration> {

    private volatile int patientAssistedCount = 0;

    public HospitalDigitalAdapter(String id, HospitalAdapterConfiguration configuration) {
        super(id, configuration);
    }

    @Override
    public void onAdapterStart() {
    }

    @Override
    public void onAdapterStop() {
    }

    @Override
    public void onDigitalTwinSync(DigitalTwinState digitalTwinState) {

        try {
            digitalTwinState.getProperty(HospitalKeywords.ASSISTANCE_LEVEL_PROPERTY_KEY)
                    .ifPresent(p -> System.out.println("   [PROPERTY] " + HospitalKeywords.ASSISTANCE_LEVEL_PROPERTY_KEY + "  = " + p.getValue()));
            digitalTwinState.getProperty(HospitalKeywords.PATIENT_ASSISTED_PROPERTY_KEY)
                .ifPresent(p -> System.out.println("   [PROPERTY] " + HospitalKeywords.PATIENT_ASSISTED_PROPERTY_KEY + "   = " + p.getValue()));
            digitalTwinState.getProperty(HospitalKeywords.TIMESTAMP_PROPERTY_KEY)
                .ifPresent(p -> System.out.println("   [PROPERTY] " + HospitalKeywords.TIMESTAMP_PROPERTY_KEY + "          = " + p.getValue()));
        } catch (WldtDigitalTwinStatePropertyException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDigitalTwinUnSync(DigitalTwinState digitalTwinState) {
    }

    @Override
    public void onDigitalTwinCreate() {
    }

    @Override
    public void onDigitalTwinStart() {
    }

    @Override
    public void onDigitalTwinStop() {
    }

    @Override
    public void onDigitalTwinDestroy() {
    }

    @Override
    protected void onStateUpdate(DigitalTwinState newDigitalTwinState, 
                                 DigitalTwinState previousDigitalTwinState, 
                                 ArrayList<DigitalTwinStateChange> digitalTwinStateChangeList) {
    }

    @Override
    protected void onEventNotificationReceived(DigitalTwinStateEventNotification<?> notification) {
        if(notification == null) return;

        String eventKey = notification.getDigitalEventKey();
        Object body     = notification.getBody();

        if(HospitalKeywords.PATIENT_ASSISTED_PROPERTY_KEY.equals(eventKey)) {
            patientAssistedCount++;
        }
    }
}