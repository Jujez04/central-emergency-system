package it.ausl.emergency.adapter.digital;

import it.ausl.emergency.adapter.configuration.MissionAdapterConfiguration;
import it.wldt.adapter.digital.DigitalAdapter;
import it.wldt.core.state.DigitalTwinState;
import it.wldt.core.state.DigitalTwinStateChange;
import it.wldt.core.state.DigitalTwinStateEventNotification;

import java.util.ArrayList;

/**
 * Mission Digital Adapter.
 * Consumes and exposes synchronized aggregations of the rescue mission state,
 * including processed KPIs, clinical variations, and active DT operational
 * relationships.
 */
public class MissionDigitalAdapter extends DigitalAdapter<MissionAdapterConfiguration> {

    public MissionDigitalAdapter(String id, MissionAdapterConfiguration configuration) {
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
    protected void onEventNotificationReceived(DigitalTwinStateEventNotification<?> digitalTwinStateEventNotification) {
    }
}