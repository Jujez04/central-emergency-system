package it.ausl.emergency.adapter.physical;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.ausl.emergency.adapter.configuration.MissionAdapterConfiguration;
import it.ausl.emergency.payload.MissionTelemetryPayload;
import it.ausl.emergency.utils.MissionKeywords;
import it.wldt.adapter.physical.ConfigurablePhysicalAdapter;
import it.wldt.adapter.physical.PhysicalAssetDescription;
import it.wldt.adapter.physical.PhysicalAssetAction;
import it.wldt.adapter.physical.PhysicalAssetEvent;
import it.wldt.adapter.physical.PhysicalAssetProperty;
import it.wldt.adapter.physical.PhysicalAssetRelationship;
import it.wldt.adapter.physical.event.PhysicalAssetActionWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetPropertyWldtEvent;
import it.wldt.exception.EventBusException;
import it.wldt.adapter.physical.event.PhysicalAssetEventWldtEvent;

/**
 * Physical Adapter for the Mission Digital Twin.
 *
 * Responsibilities:
 * 1. Publish the PhysicalAssetDescription (PAD) at binding time with all
 * mission properties, domain events, relationships and the reroute action.
 * 2. Receive {@link MissionTelemetryPayload} objects from the simulation
 * (injected by the MissionTwinManager) and forward them to the Shadowing Function.
 * 3. Handle the incoming reroute action from the digital world.
 */
public class MissionPhysicalAdapter extends ConfigurablePhysicalAdapter<MissionAdapterConfiguration> {

    // Relationship type descriptors kept for later instance publishing
    private PhysicalAssetRelationship<String> hasPatientRelationship      = null;
    private PhysicalAssetRelationship<String> involvesVehicleRelationship = null;
    private PhysicalAssetRelationship<String> targetsHospitalRelationship = null;

    // Internal flags to avoid re-publishing the relationship instances continuously
    private volatile boolean patientRelPublished   = false;
    private volatile boolean hospitalRelPublished  = false;

    public MissionPhysicalAdapter(String id, MissionAdapterConfiguration configuration) {
        super(id, configuration);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onAdapterStart() {
        try {
            System.out.println("[MissionPhysicalAdapter] -> Starting with config: " + getConfiguration());
            notifyPhysicalAdapterBound(buildPhysicalAssetDescription());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAdapterStop() {
        System.out.println("[MissionPhysicalAdapter] -> Physical adapter terminated for mission.");
    }

    // ── PAD Construction ──────────────────────────────────────────────────────

    private PhysicalAssetDescription buildPhysicalAssetDescription() {
        PhysicalAssetDescription pad = new PhysicalAssetDescription();
        MissionAdapterConfiguration cfg = getConfiguration();

        // ── Properties ────────────────────────────────────────────────────────
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.STATE_PROPERTY_KEY, cfg.getDefaultState()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.SEVERITY_CODE_PROPERTY_KEY, cfg.getDefaultSeverityCode()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.CONFIRMED_SEVERITY_PROPERTY_KEY, cfg.getDefaultConfirmedSeverity()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.PATHOLOGY_PROPERTY_KEY, cfg.getDefaultPathology()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.PATIENT_ID_PROPERTY_KEY, cfg.getDefaultPatientId()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.HOSPITAL_ID_PROPERTY_KEY, cfg.getDefaultHospitalId()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.CLINICAL_DETERIORATED_PROPERTY_KEY, cfg.isDefaultClinicalDeteriorated()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.TIME_CALLED_PROPERTY_KEY, cfg.getDefaultTimeCalled()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.TIME_ON_SCENE_PROPERTY_KEY, cfg.getDefaultTimeOnScene()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.TIME_DEPARTED_PROPERTY_KEY, cfg.getDefaultTimeDeparted()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.TIME_HANDOVER_PROPERTY_KEY, cfg.getDefaultTimeHandover()));

        // Augmented KPI properties (computed by the Shadowing Function)
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.KPI_D09Z_PROPERTY_KEY, 0.0));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.KPI_TOTAL_DURATION_PROPERTY_KEY, 0.0));

        // ── Domain Events ─────────────────────────────────────────────────────
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.VEHICLE_DISPATCHED_EVENT_KEY, MissionKeywords.CONTENT_TYPE_JSON));
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.ARRIVED_ON_SCENE_EVENT_KEY, MissionKeywords.CONTENT_TYPE_JSON));
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.CLINICAL_ASSESSMENT_EVENT_KEY, MissionKeywords.CONTENT_TYPE_JSON));
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.CLINICAL_DETERIORATION_EVENT_KEY, MissionKeywords.CONTENT_TYPE_JSON));
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.HOSPITAL_ASSIGNED_EVENT_KEY, MissionKeywords.CONTENT_TYPE_TEXT));
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.HANDOVER_COMPLETED_EVENT_KEY, MissionKeywords.CONTENT_TYPE_JSON));

        // ── Relationships Corrette (Firma a 2 Argomenti: Nome e Tipo Target) ──
        // 1-1 link verso il Patient DT (Type: "patient")
        hasPatientRelationship = new PhysicalAssetRelationship<>(MissionKeywords.REL_HAS_PATIENT, "patient");
        pad.getRelationships().add(hasPatientRelationship);

        // 1-N link verso i Vehicle DTs coinvolti (Type: "vehicle")
        involvesVehicleRelationship = new PhysicalAssetRelationship<>(MissionKeywords.REL_INVOLVES_VEHICLE, "vehicle");
        pad.getRelationships().add(involvesVehicleRelationship);

        // 0-1 link verso l'Hospital DT target (Type: "hospital")
        targetsHospitalRelationship = new PhysicalAssetRelationship<>(MissionKeywords.REL_TARGETS_HOSPITAL, "hospital");
        pad.getRelationships().add(targetsHospitalRelationship);

        // ── Action: reroute hospital ──────────────────────────────────────────
        pad.getActions().add(new PhysicalAssetAction(
                MissionKeywords.ACTION_REROUTE_HOSPITAL,
                "mission.reroute",
                MissionKeywords.CONTENT_TYPE_TEXT));

        return pad;
    }

    // ── Telemetry Ingestion (Iniettata dal MissionTwinManager) ─────────────────

    public void onMissionTelemetryReceived(MissionTelemetryPayload payload) {
        if (payload == null) return;

        try {
            // Aggiornamento Proprietà Fisiche
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.STATE_PROPERTY_KEY, payload.state()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.SEVERITY_CODE_PROPERTY_KEY, payload.severityCode()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.CONFIRMED_SEVERITY_PROPERTY_KEY, payload.confirmedSeverityCode()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.PATHOLOGY_PROPERTY_KEY, payload.pathology()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.PATIENT_ID_PROPERTY_KEY, payload.patientId()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.HOSPITAL_ID_PROPERTY_KEY, payload.hospitalId()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.CLINICAL_DETERIORATED_PROPERTY_KEY, payload.clinicalDeteriorated()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.TIME_CALLED_PROPERTY_KEY, payload.timeCalled()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.TIME_ON_SCENE_PROPERTY_KEY, payload.timeOnScene()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.TIME_DEPARTED_PROPERTY_KEY, payload.timeDeparted()));
            publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.TIME_HANDOVER_PROPERTY_KEY, payload.timeHandover()));

            // Dispatch degli eventi guidati dallo stato nominale
            publishStateEvent(payload);

            // Generazione delle istanze di relazione se disponibili
            publishRelationshipInstances(payload);

        } catch (Exception e) {
            System.err.println("[MissionPhysicalAdapter] EventBus error on telemetry: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void publishStateEvent(MissionTelemetryPayload payload) throws EventBusException {
        switch (payload.state()) {
            case MissionKeywords.STATE_DISPATCHED ->
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(MissionKeywords.VEHICLE_DISPATCHED_EVENT_KEY, payload));

            case MissionKeywords.STATE_ON_SCENE ->
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(MissionKeywords.ARRIVED_ON_SCENE_EVENT_KEY, payload));

            case MissionKeywords.STATE_TRANSPORTING -> {
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(MissionKeywords.CLINICAL_ASSESSMENT_EVENT_KEY, payload));
                
                if (payload.clinicalDeteriorated()) {
                    publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(MissionKeywords.CLINICAL_DETERIORATION_EVENT_KEY, payload));
                }
                
                if (!"null".equals(payload.hospitalId())) {
                    publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(MissionKeywords.HOSPITAL_ASSIGNED_EVENT_KEY, payload.hospitalId()));
                }
            }

            case MissionKeywords.STATE_COMPLETED ->
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(MissionKeywords.HANDOVER_COMPLETED_EVENT_KEY, payload));

            default -> { /* STATE_TRIAGING: nessun evento preliminare */ }
        }
    }

    private void publishRelationshipInstances(MissionTelemetryPayload payload) throws EventBusException  {

        // Istanziazione lasca della relazione has_patient al boot effettivo
        if (!patientRelPublished
                && payload.patientId() != null
                && !"null".equals(payload.patientId())
                && hasPatientRelationship != null) {

            publishPhysicalAssetRelationshipCreatedWldtEvent(
                    new it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceCreatedWldtEvent<>(
                            hasPatientRelationship.createRelationshipInstance(
                                    "rel-patient-" + payload.patientId()
                            )
                    )
            );
            patientRelPublished = true;
            System.out.println("[MissionPhysicalAdapter] -> Relationship 'has_patient' linked to: " + payload.patientId());
        }

        // Istanziazione lasca targets_hospital quando l'ospedale viene assegnato o aggiornato
        if (!"null".equals(payload.hospitalId())
                && targetsHospitalRelationship != null) {

            if (!hospitalRelPublished) {
                publishPhysicalAssetRelationshipCreatedWldtEvent(
                        new it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceCreatedWldtEvent<>(
                                targetsHospitalRelationship.createRelationshipInstance(
                                        "rel-hospital-" + payload.hospitalId()
                                )
                        )
                );
                hospitalRelPublished = true;
                System.out.println("[MissionPhysicalAdapter] -> Relationship 'targets_hospital' linked to: " + payload.hospitalId());
            }
        }
    }

    // ── Incoming Digital Action Actuation ─────────────────────────────────────

    @Override
    public void onIncomingPhysicalAction(PhysicalAssetActionWldtEvent<?> event) {
        if (event == null) return;

        if (MissionKeywords.ACTION_REROUTE_HOSPITAL.equals(event.getActionKey())
                && event.getBody() instanceof String newHospitalId) {

            System.out.println("[MissionPhysicalAdapter] -> REROUTE ACTION received. New hospital target: " + newHospitalId);

            try {
                // Notifichiamo la variazione della proprietà sul bus interno del twin
                publishPhysicalAssetPropertyWldtEvent(new PhysicalAssetPropertyWldtEvent<>(
                        MissionKeywords.HOSPITAL_ID_PROPERTY_KEY, newHospitalId));

                // Generiamo la nuova istanza di relazione verso il nuovo ospedale di destinazione
                if (targetsHospitalRelationship != null) {
                    publishPhysicalAssetRelationshipCreatedWldtEvent(
                            new it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceCreatedWldtEvent<>(
                                    targetsHospitalRelationship.createRelationshipInstance(
                                            "rel-hospital-" + newHospitalId
                                    )
                            )
                    );
                }

                // Generiamo l'evento di dominio accoppiato
                publishPhysicalAssetEventWldtEvent(new PhysicalAssetEventWldtEvent<>(
                        MissionKeywords.HOSPITAL_ASSIGNED_EVENT_KEY, newHospitalId));

            } catch (EventBusException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("[MissionPhysicalAdapter] -> Unknown action key: "
                    + (event.getActionKey() != null ? event.getActionKey() : "null"));
        }
    }
}