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
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceCreatedWldtEvent;
import it.wldt.adapter.physical.event.PhysicalAssetRelationshipInstanceDeletedWldtEvent;

/**
 * Physical Adapter per il Mission Digital Twin.
 *
 * La missione è un'entità AGGREGATA: non esiste un topic MQTT dedicato.
 * Il MissionTwinManager la costruisce aggregando i payload di:
 *   - PatientTelemetryPayload  → stato clinico + timings
 *   - AmbulanceTelemetryPayload → hospitalId + stato veicolo
 *
 * Questo adapter:
 * 1. Pubblica la PAD al binding con tutte le proprietà, gli eventi, le
 *    relazioni e l'azione di reroute.
 * 2. Riceve MissionTelemetryPayload già aggregati dal MissionTwinManager
 *    e li inoltra alla ShadowingFunction.
 * 3. Gestisce i link semantici verso Patient, Vehicle e Hospital tramite
 *    i metodi linkPatient / linkVehicle / linkHospital / unlinkHospital
 *    che vengono chiamati dal MissionTwinManager.
 */
public class MissionPhysicalAdapter extends ConfigurablePhysicalAdapter<MissionAdapterConfiguration> {

    // Relationship descriptors conservati per creare le istanze in seguito
    private PhysicalAssetRelationship<String> hasPatientRelationship    = null;
    private PhysicalAssetRelationship<String> involvesVehicleRelationship = null;
    private PhysicalAssetRelationship<String> targetsHospitalRelationship = null;

    // Flag per evitare di ri-pubblicare relazioni già attive
    private volatile boolean patientRelPublished  = false;
    private volatile boolean hospitalRelPublished = false;

    public MissionPhysicalAdapter(String id, MissionAdapterConfiguration configuration) {
        super(id, configuration);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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

    // ── PAD Builder ───────────────────────────────────────────────────────────

    private PhysicalAssetDescription buildPhysicalAssetDescription() {
        PhysicalAssetDescription pad = new PhysicalAssetDescription();
        MissionAdapterConfiguration cfg = getConfiguration();

        // Proprietà fisiche
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.STATE_PROPERTY_KEY,              cfg.getDefaultState()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.SEVERITY_CODE_PROPERTY_KEY,      cfg.getDefaultSeverityCode()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.CONFIRMED_SEVERITY_PROPERTY_KEY, cfg.getDefaultConfirmedSeverity()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.PATHOLOGY_PROPERTY_KEY,          cfg.getDefaultPathology()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.PATIENT_ID_PROPERTY_KEY,         cfg.getDefaultPatientId()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.HOSPITAL_ID_PROPERTY_KEY,        cfg.getDefaultHospitalId()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.CLINICAL_DETERIORATED_PROPERTY_KEY, cfg.isDefaultClinicalDeteriorated()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.TIME_CALLED_PROPERTY_KEY,        cfg.getDefaultTimeCalled()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.TIME_ON_SCENE_PROPERTY_KEY,      cfg.getDefaultTimeOnScene()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.TIME_DEPARTED_PROPERTY_KEY,      cfg.getDefaultTimeDeparted()));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.TIME_HANDOVER_PROPERTY_KEY,      cfg.getDefaultTimeHandover()));

        // Proprietà KPI augmented (inizializzate a 0)
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.KPI_D09Z_PROPERTY_KEY,           0.0));
        pad.getProperties().add(new PhysicalAssetProperty<>(MissionKeywords.KPI_TOTAL_DURATION_PROPERTY_KEY, 0.0));

        // Domain Events
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.VEHICLE_DISPATCHED_EVENT_KEY,      MissionKeywords.CONTENT_TYPE_JSON));
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.ARRIVED_ON_SCENE_EVENT_KEY,        MissionKeywords.CONTENT_TYPE_JSON));
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.CLINICAL_ASSESSMENT_EVENT_KEY,     MissionKeywords.CONTENT_TYPE_JSON));
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.CLINICAL_DETERIORATION_EVENT_KEY,  MissionKeywords.CONTENT_TYPE_JSON));
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.HOSPITAL_ASSIGNED_EVENT_KEY,       MissionKeywords.CONTENT_TYPE_TEXT));
        pad.getEvents().add(new PhysicalAssetEvent(MissionKeywords.HANDOVER_COMPLETED_EVENT_KEY,      MissionKeywords.CONTENT_TYPE_JSON));

        // Relazioni semantiche
        hasPatientRelationship     = new PhysicalAssetRelationship<>(MissionKeywords.REL_HAS_PATIENT,      "patient");
        involvesVehicleRelationship = new PhysicalAssetRelationship<>(MissionKeywords.REL_INVOLVES_VEHICLE, "vehicle");
        targetsHospitalRelationship = new PhysicalAssetRelationship<>(MissionKeywords.REL_TARGETS_HOSPITAL, "hospital");

        pad.getRelationships().add(hasPatientRelationship);
        pad.getRelationships().add(involvesVehicleRelationship);
        pad.getRelationships().add(targetsHospitalRelationship);

        return pad;
    }

    // ── Telemetry Ingestion ───────────────────────────────────────────────────

    /**
     * Punto di ingresso principale: riceve il payload aggregato dal
     * MissionTwinManager e lo propaga alla ShadowingFunction tramite
     * gli eventi WLDT sul bus interno.
     */
    public void onMissionTelemetryReceived(MissionTelemetryPayload payload) {
        if (payload == null) return;

        try {
            publishPhysicalAssetPropertyWldtEvent(
                    new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.STATE_PROPERTY_KEY,              payload.state()));
            publishPhysicalAssetPropertyWldtEvent(
                    new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.SEVERITY_CODE_PROPERTY_KEY,      payload.severityCode()));
            publishPhysicalAssetPropertyWldtEvent(
                    new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.CONFIRMED_SEVERITY_PROPERTY_KEY, payload.confirmedSeverityCode()));
            publishPhysicalAssetPropertyWldtEvent(
                    new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.PATHOLOGY_PROPERTY_KEY,          payload.pathology()));
            publishPhysicalAssetPropertyWldtEvent(
                    new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.PATIENT_ID_PROPERTY_KEY,         payload.patientId()));
            publishPhysicalAssetPropertyWldtEvent(
                    new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.HOSPITAL_ID_PROPERTY_KEY,        payload.hospitalId()));
            publishPhysicalAssetPropertyWldtEvent(
                    new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.CLINICAL_DETERIORATED_PROPERTY_KEY, payload.clinicalDeteriorated()));
            publishPhysicalAssetPropertyWldtEvent(
                    new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.TIME_CALLED_PROPERTY_KEY,        payload.timeCalled()));
            publishPhysicalAssetPropertyWldtEvent(
                    new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.TIME_ON_SCENE_PROPERTY_KEY,      payload.timeOnScene()));
            publishPhysicalAssetPropertyWldtEvent(
                    new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.TIME_DEPARTED_PROPERTY_KEY,      payload.timeDeparted()));
            publishPhysicalAssetPropertyWldtEvent(
                    new PhysicalAssetPropertyWldtEvent<>(MissionKeywords.TIME_HANDOVER_PROPERTY_KEY,      payload.timeHandover()));

            publishStateEvent(payload);

        } catch (Exception e) {
            System.err.println("[MissionPhysicalAdapter] EventBus error on telemetry: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Relationship Link API (chiamata dal MissionTwinManager) ──────────────

    /**
     * Crea la relazione has_patient verso il paziente.
     * Idempotente: una volta pubblicata, non viene ri-pubblicata.
     *
     * @param patientId ID dell'agente paziente nella simulazione
     */
    public void linkPatient(String patientId) {
        if (patientRelPublished || patientId == null || "null".equals(patientId)) return;
        if (hasPatientRelationship == null) return;

        try {
            publishPhysicalAssetRelationshipCreatedWldtEvent(
                    new PhysicalAssetRelationshipInstanceCreatedWldtEvent<>(
                            hasPatientRelationship.createRelationshipInstance(
                                    "rel-patient-" + patientId)));
            patientRelPublished = true;
            System.out.println("[MissionPhysicalAdapter] Relationship has_patient → " + patientId);
        } catch (Exception e) {
            System.err.println("[MissionPhysicalAdapter] linkPatient error: " + e.getMessage());
        }
    }

    /**
     * Aggiunge (o aggiorna) la relazione involves_vehicle verso un veicolo.
     * Può essere chiamato più volte per veicoli diversi coinvolti nella stessa
     * missione (ambulanza + MedCar, ecc.).
     *
     * @param vehicleId ID dell'agente veicolo nella simulazione
     */
    public void linkVehicle(String vehicleId) {
        if (vehicleId == null || "null".equals(vehicleId)) return;
        if (involvesVehicleRelationship == null) return;

        try {
            publishPhysicalAssetRelationshipCreatedWldtEvent(
                    new PhysicalAssetRelationshipInstanceCreatedWldtEvent<>(
                            involvesVehicleRelationship.createRelationshipInstance(
                                    "rel-vehicle-" + vehicleId)));
            System.out.println("[MissionPhysicalAdapter] Relationship involves_vehicle → " + vehicleId);
        } catch (Exception e) {
            System.err.println("[MissionPhysicalAdapter] linkVehicle error: " + e.getMessage());
        }
    }

    /**
     * Crea la relazione targets_hospital verso l'ospedale di destinazione.
     * Se viene chiamato con un nuovo hospitalId (rerouting), prima rimuove
     * il link precedente tramite unlinkHospital.
     *
     * @param hospitalId ID dell'ospedale di destinazione
     */
    public void linkHospital(String hospitalId) {
        if (hospitalId == null || "null".equals(hospitalId)) return;
        if (targetsHospitalRelationship == null) return;

        try {
            publishPhysicalAssetRelationshipCreatedWldtEvent(
                    new PhysicalAssetRelationshipInstanceCreatedWldtEvent<>(
                            targetsHospitalRelationship.createRelationshipInstance(
                                    "rel-hospital-" + hospitalId)));
            hospitalRelPublished = true;
            System.out.println("[MissionPhysicalAdapter] Relationship targets_hospital → " + hospitalId);
        } catch (Exception e) {
            System.err.println("[MissionPhysicalAdapter] linkHospital error: " + e.getMessage());
        }
    }

    /**
     * Rimuove la relazione targets_hospital verso un ospedale precedente.
     * Usato dal MissionTwinManager quando rileva un cambio di destinazione
     * (rerouting per deterioramento clinico).
     *
     * @param hospitalId ID dell'ospedale da scollegare
     */
    public void unlinkHospital(String hospitalId) {
        if (hospitalId == null || "null".equals(hospitalId)) return;
        if (targetsHospitalRelationship == null) return;

        try {
            // WLDT richiede un'istanza per poter pubblicare la deletion
            var instance = targetsHospitalRelationship.createRelationshipInstance(
                    "rel-hospital-" + hospitalId);
            publishPhysicalAssetRelationshipDeletedWldtEvent(
                    new PhysicalAssetRelationshipInstanceDeletedWldtEvent<>(instance));
            System.out.println("[MissionPhysicalAdapter] Relationship targets_hospital ✗ " + hospitalId);
        } catch (Exception e) {
            System.err.println("[MissionPhysicalAdapter] unlinkHospital error: " + e.getMessage());
        }
    }

    // ── Domain Event Publishing ───────────────────────────────────────────────

    private void publishStateEvent(MissionTelemetryPayload payload) throws EventBusException {
        switch (payload.state()) {
            case MissionKeywords.STATE_DISPATCHED ->
                publishPhysicalAssetEventWldtEvent(
                        new PhysicalAssetEventWldtEvent<>(MissionKeywords.VEHICLE_DISPATCHED_EVENT_KEY, payload));

            case MissionKeywords.STATE_ON_SCENE ->
                publishPhysicalAssetEventWldtEvent(
                        new PhysicalAssetEventWldtEvent<>(MissionKeywords.ARRIVED_ON_SCENE_EVENT_KEY, payload));

            case MissionKeywords.STATE_TRANSPORTING -> {
                publishPhysicalAssetEventWldtEvent(
                        new PhysicalAssetEventWldtEvent<>(MissionKeywords.CLINICAL_ASSESSMENT_EVENT_KEY, payload));

                if (payload.clinicalDeteriorated()) {
                    publishPhysicalAssetEventWldtEvent(
                            new PhysicalAssetEventWldtEvent<>(MissionKeywords.CLINICAL_DETERIORATION_EVENT_KEY, payload));
                }

                if (!"null".equals(payload.hospitalId())) {
                    publishPhysicalAssetEventWldtEvent(
                            new PhysicalAssetEventWldtEvent<>(MissionKeywords.HOSPITAL_ASSIGNED_EVENT_KEY, payload.hospitalId()));
                }
            }

            case MissionKeywords.STATE_COMPLETED ->
                publishPhysicalAssetEventWldtEvent(
                        new PhysicalAssetEventWldtEvent<>(MissionKeywords.HANDOVER_COMPLETED_EVENT_KEY, payload));

            default -> { /* STATE_TRIAGING: nessun evento preliminare */ }
        }
    }

    // ── Incoming Digital Action (Reroute Hospital) ────────────────────────────

    @Override
    public void onIncomingPhysicalAction(PhysicalAssetActionWldtEvent<?> event) {
    }
}