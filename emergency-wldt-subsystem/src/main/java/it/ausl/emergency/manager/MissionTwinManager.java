package it.ausl.emergency.manager;

import it.ausl.emergency.payload.PatientTelemetryPayload;
import it.ausl.emergency.payload.AmbulanceTelemetryPayload;
import it.ausl.emergency.payload.MedCarTelemetryPayload;
import it.ausl.emergency.payload.MissionTelemetryPayload;
import it.ausl.emergency.twin.MissionDigitalTwin;
import it.ausl.emergency.utils.MissionKeywords;
import it.wldt.core.engine.DigitalTwinEngine;
import it.wldt.core.state.DigitalTwinState;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory e registry dei Mission Digital Twin.
 *
 * Regola fondamentale: la MissioneDT nasce SOLO quando arriva la prima
 * telemetria del paziente. I veicoli e l'ospedale si collegano in seguito
 * tramite i metodi linkVehicle / linkHospital del physical adapter.
 *
 * Non vengono mai create missioni "orfane" originate da payload di veicoli
 * senza una missione paziente già esistente nel registry.
 *
 * Correzioni applicate rispetto alla versione precedente:
 * 1. activeMissionCount() aggiunto (era usato da CentralEmergencyDigitalAdapter
 * ma non esisteva nel manager).
 * 2. mapPatientStateToMission: aggiunto mapping esplicito di "AtHospital"
 * → STATE_COMPLETED (prima mancava, causando lo stato a rimanere su
 * STATE_TRANSPORTING anche a missione conclusa).
 * 3. onVehicleTelemetryUpdate: NON crea più la missione se non esiste già.
 * Se non c'è una missione per il patientId dichiarato dal veicolo, il
 * payload viene silenziosamente ignorato.
 * 4. scheduleCleanup: il timeout di pulizia è allungato a 60s per evitare
 * che il twin venga rimosso prima che il CentralEmergencyDigitalAdapter
 * abbia rilevato lo stato COMPLETED e aggiornato i KPI.
 */
public class MissionTwinManager {

    private static final long CLEANUP_DELAY_MS = 60_000L;
    private static final long BIND_TIMEOUT_MS = 4_000L;
    private static final long BIND_POLL_MS = 50L;

    private final DigitalTwinEngine engine;
    private final ConcurrentHashMap<String, MissionDigitalTwin> registry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MissionTelemetryPayload> missionSnapshots = new ConcurrentHashMap<>();

    public MissionTwinManager(DigitalTwinEngine engine) {
        this.engine = engine;
    }

    public synchronized void onPatientTelemetryUpdate(String patientId,
            PatientTelemetryPayload patientPayload) {
        if (patientId == null || "null".equals(patientId))
            return;

        String missionId = "M-" + patientId;
        MissionTelemetryPayload oldSnapshot = missionSnapshots.get(missionId);
        String currentHospitalId = (oldSnapshot != null) ? oldSnapshot.hospitalId() : "null";

        double timeOnScene = (oldSnapshot != null) ? oldSnapshot.timeOnScene() : 0.0;
        double timeDeparted = (oldSnapshot != null) ? oldSnapshot.timeDeparted() : 0.0;
        double timeHandover = (oldSnapshot != null) ? oldSnapshot.timeHandover() : 0.0;

        String missionState = mapPatientStateToMission(patientPayload.state());

        MissionTelemetryPayload newPayload = new MissionTelemetryPayload(
                missionState,
                patientPayload.severityCode(),
                patientPayload.confirmedSeverityCode(),
                patientPayload.pathology(),
                patientId,
                currentHospitalId,
                patientPayload.isClinicalDeteriorated(),
                patientPayload.timeCalled(),
                timeOnScene,
                timeDeparted,
                timeHandover);

        boolean isNewMission = (oldSnapshot == null);
        MissionDigitalTwin twin = updateAndSyncTwin(missionId, newPayload);

        // Collega il paziente alla missione solo alla prima creazione
        if (isNewMission && twin != null && twin.getPhysicalAdapter() != null) {
            twin.getPhysicalAdapter().linkPatient(patientId);
        }
    }

    public synchronized void onVehicleTelemetryUpdate(String vehicleId,
            AmbulanceTelemetryPayload vehiclePayload) {
        String patientId = vehiclePayload.patientId();
        if (patientId == null || "null".equals(patientId) || patientId.isEmpty())
            return;

        String missionId = "M-" + patientId;
        MissionTelemetryPayload oldSnapshot = missionSnapshots.get(missionId);

        if (oldSnapshot == null) {
            System.out.printf(
                    "[MissionTwinManager] Veicolo %s dichiara paziente %s ma la missione %s "
                            + "non esiste ancora — payload ignorato (la missione nasce dal paziente).%n",
                    vehicleId, patientId, missionId);
            return;
        }

        String missionState = oldSnapshot.state();
        if ("MovingToHospital".equalsIgnoreCase(vehiclePayload.state())
                && !MissionKeywords.STATE_TRANSPORTING.equalsIgnoreCase(missionState)
                && !MissionKeywords.STATE_COMPLETED.equalsIgnoreCase(missionState)) {
            missionState = MissionKeywords.STATE_TRANSPORTING;
        }

        double timeOnScene = oldSnapshot.timeOnScene();
        double timeDeparted = oldSnapshot.timeDeparted();
        double timeHandover = oldSnapshot.timeHandover();

        String vState = vehiclePayload.state();
        if (timeOnScene == 0.0 && ("TakingPatient".equalsIgnoreCase(vState)
                || "Supporting".equalsIgnoreCase(vState)
                || "TreatingPatient".equalsIgnoreCase(vState))) {
            timeOnScene = vehiclePayload.timestamp();
        }

        if (timeDeparted == 0.0 && "MovingToHospital".equalsIgnoreCase(vState)) {
            timeDeparted = vehiclePayload.timestamp();
        }

        if (timeHandover == 0.0 && "Handover".equalsIgnoreCase(vState)) {
            timeHandover = vehiclePayload.timestamp();
        }
        String oldHospitalId = oldSnapshot.hospitalId();
        String newHospitalId = vehiclePayload.hospitalId();

        MissionTelemetryPayload newPayload = new MissionTelemetryPayload(
                missionState,
                oldSnapshot.severityCode(),
                oldSnapshot.confirmedSeverityCode(),
                oldSnapshot.pathology(),
                patientId,
                (newHospitalId != null && !"null".equals(newHospitalId) && !newHospitalId.isEmpty())
                        ? newHospitalId
                        : oldHospitalId,
                oldSnapshot.clinicalDeteriorated(),
                oldSnapshot.timeCalled(),
                timeOnScene,
                timeDeparted,
                timeHandover);

        MissionDigitalTwin twin = updateAndSyncTwin(missionId, newPayload);

        if (twin != null && twin.getPhysicalAdapter() != null) {
            twin.getPhysicalAdapter().linkVehicle(vehicleId);
            if (newHospitalId != null && !"null".equals(newHospitalId) && !newHospitalId.isEmpty()
                    && !newHospitalId.equals(oldHospitalId)) {
                if (oldHospitalId != null && !"null".equals(oldHospitalId)) {
                    twin.getPhysicalAdapter().unlinkHospital(oldHospitalId);
                }
                twin.getPhysicalAdapter().linkHospital(newHospitalId);
            }
        }
    }

    public int activeMissionCount() {
        return registry.size();
    }

    public Map<String, MissionDigitalTwin> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }

    private MissionDigitalTwin updateAndSyncTwin(String missionId,
            MissionTelemetryPayload payload) {
        missionSnapshots.put(missionId, payload);

        MissionDigitalTwin twin = registry.computeIfAbsent(missionId, id -> {
            try {
                it.ausl.emergency.shadowing.MissionShadowingFunction sf = new it.ausl.emergency.shadowing.MissionShadowingFunction(
                        "mission-shadowing-" + id);
                MissionDigitalTwin mDt = new MissionDigitalTwin("dt-" + id, sf);
                engine.addDigitalTwin(mDt);
                engine.startDigitalTwin("dt-" + id);
                waitForBinding(mDt);
                System.out.println("[MissionTwinManager] Missione creata: " + id);
                return mDt;
            } catch (Exception e) {
                throw new RuntimeException("Errore avvio DT Missione: " + id, e);
            }
        });

        if (twin.getPhysicalAdapter() != null) {
            twin.getPhysicalAdapter().onMissionTelemetryReceived(payload);
        }

        if (MissionKeywords.STATE_COMPLETED.equalsIgnoreCase(payload.state())) {
            scheduleCleanup(missionId);
        }

        return twin;
    }

    private String mapPatientStateToMission(String patientState) {
        if (patientState == null)
            return MissionKeywords.STATE_TRIAGING;
        return switch (patientState) {
            case "Signaled" -> MissionKeywords.STATE_TRIAGING;
            case "WaitingSupport" -> MissionKeywords.STATE_DISPATCHED;
            case "BeingTreated" -> MissionKeywords.STATE_ON_SCENE;
            case "MovingToHospital" -> MissionKeywords.STATE_TRANSPORTING;
            // FIX: "AtHospital" era assente — causava la missione a restare
            // su STATE_TRANSPORTING anche dopo l'handover
            case "AtHospital" -> MissionKeywords.STATE_COMPLETED;
            case "Completed" -> MissionKeywords.STATE_COMPLETED;
            case "Handover" -> MissionKeywords.STATE_COMPLETED;
            default -> patientState;
        };
    }

    private void waitForBinding(MissionDigitalTwin twin) {
        long deadline = System.currentTimeMillis() + BIND_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                DigitalTwinState s = twin.getShadowingFunction()
                        .getDigitalTwinStateManager()
                        .getDigitalTwinState();
                if (s != null
                        && s.getProperty(MissionKeywords.STATE_PROPERTY_KEY).isPresent()) {
                    return;
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(BIND_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.err.println("[MissionTwinManager] WARNING: binding non completato entro "
                + BIND_TIMEOUT_MS + "ms per il twin " + twin);
    }

    private void scheduleCleanup(String missionId) {
        if (!registry.containsKey(missionId))
            return;

        new Thread(() -> {
            try {
                Thread.sleep(CLEANUP_DELAY_MS);
                registry.remove(missionId);
                missionSnapshots.remove(missionId);
                System.out.println("[MissionTwinManager] Missione rimossa dal registry: " + missionId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "mission-cleanup-" + missionId).start();
    }

    public synchronized void onVehicleTelemetryUpdate(
            String vehicleId,
            MedCarTelemetryPayload vehiclePayload) {

        String patientId = vehiclePayload.patientId();

        if (patientId == null
                || patientId.isBlank()
                || "null".equals(patientId)) {
            return;
        }

        String missionId = "M-" + patientId;

        MissionTelemetryPayload snapshot = missionSnapshots.get(missionId);

        if (snapshot == null) {
            return;
        }

        MissionDigitalTwin twin = registry.get(missionId);

        if (twin != null && twin.getPhysicalAdapter() != null) {
            twin.getPhysicalAdapter().linkVehicle(vehicleId);
        }
    }

    public synchronized void onAmbulanceTelemetryUpdate(
            String vehicleId,
            String patientId,
            String hospitalId) {

        if (patientId == null || patientId.isBlank()) {
            return;
        }

        String missionId = "M-" + patientId;

        MissionDigitalTwin twin = registry.get(missionId);

        if (twin == null) {
            return;
        }

        twin.getPhysicalAdapter().linkVehicle(vehicleId);

        if (hospitalId != null
                && !hospitalId.isBlank()
                && !"null".equals(hospitalId)) {

            twin.getPhysicalAdapter().linkHospital(hospitalId);
        }
    }

    public synchronized void onMedCarTelemetryUpdate(
            String vehicleId,
            String patientId) {

        if (patientId == null || patientId.isBlank()) {
            return;
        }

        String missionId = "M-" + patientId;

        MissionDigitalTwin twin = registry.get(missionId);

        if (twin == null) {
            return;
        }

        twin.getPhysicalAdapter().linkVehicle(vehicleId);
    }

    public synchronized void onHelicopterTelemetryUpdate(
            String vehicleId,
            String patientId,
            String hospitalId) {

        if (patientId == null
                || patientId.isBlank()
                || "null".equals(patientId)) {
            return;
        }

        String missionId = "M-" + patientId;

        MissionTelemetryPayload oldSnapshot = missionSnapshots.get(missionId);

        if (oldSnapshot == null) {
            return;
        }

        MissionDigitalTwin twin = registry.get(missionId);

        if (twin == null || twin.getPhysicalAdapter() == null) {
            return;
        }

        twin.getPhysicalAdapter().linkVehicle(vehicleId);

        if (hospitalId != null
                && !hospitalId.isBlank()
                && !"null".equals(hospitalId)
                && !hospitalId.equals(oldSnapshot.hospitalId())) {

            if (oldSnapshot.hospitalId() != null
                    && !"null".equals(oldSnapshot.hospitalId())) {

                twin.getPhysicalAdapter()
                        .unlinkHospital(oldSnapshot.hospitalId());
            }

            twin.getPhysicalAdapter()
                    .linkHospital(hospitalId);
        }
    }
}