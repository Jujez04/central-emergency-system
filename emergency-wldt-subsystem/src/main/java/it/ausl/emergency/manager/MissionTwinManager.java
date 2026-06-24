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

    /** Twin attivi indicizzati per missionId ("M-" + patientId). */
    private final ConcurrentHashMap<String, MissionDigitalTwin> registry = new ConcurrentHashMap<>();

    /**
     * Ultimo snapshot aggregato per ogni missione.
     * Viene mantenuto anche dopo che il twin è stato rimosso dal registry,
     * in modo che onVehicleTelemetryUpdate possa ancora leggere l'hospitalId
     * corrente senza accedere allo stato WLDT.
     */
    private final ConcurrentHashMap<String, MissionTelemetryPayload> missionSnapshots = new ConcurrentHashMap<>();

    public MissionTwinManager(DigitalTwinEngine engine) {
        this.engine = engine;
    }

    // ── Ingestion dal Paziente ────────────────────────────────────────────────

    /**
     * Punto di ingresso primario: chiamato ogni volta che arriva un payload
     * dal paziente.
     *
     * Se è la prima telemetria per questo patientId, crea la MissioneDT.
     * Aggiorna quindi tutte le proprietà della missione derivandole dallo
     * stato clinico del paziente.
     *
     * @param patientId      ID agente AnyLogic del paziente
     * @param patientPayload payload telemetrico corrente del paziente
     */
    public synchronized void onPatientTelemetryUpdate(String patientId,
            PatientTelemetryPayload patientPayload) {
        if (patientId == null || "null".equals(patientId))
            return;

        String missionId = "M-" + patientId;
        MissionTelemetryPayload oldSnapshot = missionSnapshots.get(missionId);

        // Preserva l'hospitalId già impostato dai veicoli (il paziente non lo conosce)
        String currentHospitalId = (oldSnapshot != null) ? oldSnapshot.hospitalId() : "null";

        // Ricostruisce i timestamp incrementalmente
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

    // ── Ingestion dai Veicoli ─────────────────────────────────────────────────

    /**
     * Chiamato quando arriva un payload da un veicolo (ambulanza, MedCar,
     * MedHelicopter) che dichiara di avere un paziente assegnato.
     *
     * IMPORTANTE: se non esiste già una missione per il patientId dichiarato
     * dal veicolo, il payload viene ignorato. La missione nasce SOLO dalla
     * telemetria del paziente.
     *
     * Il veicolo contribuisce con:
     * - hospitalId (se valorizzato e diverso da quello corrente → rerouting)
     * - collegamento semantico involves_vehicle
     *
     * @param vehicleId      ID agente AnyLogic del veicolo
     * @param vehiclePayload payload compatibile (già normalizzato dal chiamante)
     */
    public synchronized void onVehicleTelemetryUpdate(String vehicleId,
            AmbulanceTelemetryPayload vehiclePayload) {
        String patientId = vehiclePayload.patientId();
        if (patientId == null || "null".equals(patientId) || patientId.isEmpty())
            return;

        String missionId = "M-" + patientId;
        MissionTelemetryPayload oldSnapshot = missionSnapshots.get(missionId);

        // FIX PRINCIPALE: se la missione non esiste ancora, ignora il payload.
        // Sarà il primo payload del paziente a crearla.
        if (oldSnapshot == null) {
            System.out.printf(
                    "[MissionTwinManager] Veicolo %s dichiara paziente %s ma la missione %s "
                            + "non esiste ancora — payload ignorato (la missione nasce dal paziente).%n",
                    vehicleId, patientId, missionId);
            return;
        }

        // Determina lo stato della missione: se il veicolo sta andando all'ospedale,
        // aggiorna lo stato a TRANSPORTING (solo se la missione è ancora ON_SCENE)
        String missionState = oldSnapshot.state();
        if ("MovingToHospital".equalsIgnoreCase(vehiclePayload.state())
                && !MissionKeywords.STATE_TRANSPORTING.equalsIgnoreCase(missionState)
                && !MissionKeywords.STATE_COMPLETED.equalsIgnoreCase(missionState)) {
            missionState = MissionKeywords.STATE_TRANSPORTING;
        }

        // ─── NUOVA LOGICA DI ACQUISIZIONE GEOMETRICA DEI TEMPI ───
        double timeOnScene = oldSnapshot.timeOnScene();
        double timeDeparted = oldSnapshot.timeDeparted();
        double timeHandover = oldSnapshot.timeHandover();

        String vState = vehiclePayload.state();

        // 1. Il veicolo arriva sul posto (Inizio trattamento o supporto)
        if (timeOnScene == 0.0 && ("TakingPatient".equalsIgnoreCase(vState)
                || "Supporting".equalsIgnoreCase(vState)
                || "TreatingPatient".equalsIgnoreCase(vState))) {
            timeOnScene = vehiclePayload.timestamp(); // Timestamp reale della simulazione!
        }

        // 2. Il veicolo parte verso l'ospedale (Inizio trasporto)
        if (timeDeparted == 0.0 && "MovingToHospital".equalsIgnoreCase(vState)) {
            timeDeparted = vehiclePayload.timestamp();
        }

        // 3. Il veicolo arriva in ospedale (Handover completato)
        if (timeHandover == 0.0 && "Handover".equalsIgnoreCase(vState)) {
            timeHandover = vehiclePayload.timestamp();
        }
        // ─────────────────────────────────────────────────────────

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
                timeOnScene, // Passiamo i tempi correttamente valorizzati
                timeDeparted,
                timeHandover);

        MissionDigitalTwin twin = updateAndSyncTwin(missionId, newPayload);

        if (twin != null && twin.getPhysicalAdapter() != null) {
            // Collega il veicolo alla missione
            twin.getPhysicalAdapter().linkVehicle(vehicleId);

            // Gestione del link ospedaliero (eventuale rerouting)
            if (newHospitalId != null && !"null".equals(newHospitalId) && !newHospitalId.isEmpty()
                    && !newHospitalId.equals(oldHospitalId)) {
                if (oldHospitalId != null && !"null".equals(oldHospitalId)) {
                    twin.getPhysicalAdapter().unlinkHospital(oldHospitalId);
                }
                twin.getPhysicalAdapter().linkHospital(newHospitalId);
            }
        }
    }

    // ── API pubblica ──────────────────────────────────────────────────────────

    /**
     * Numero di missioni attive nel registry (usato da
     * CentralEmergencyDigitalAdapter).
     */
    public int activeMissionCount() {
        return registry.size();
    }

    public Map<String, MissionDigitalTwin> getRegistry() {
        return Collections.unmodifiableMap(registry);
    }

    // ── Logica interna ────────────────────────────────────────────────────────

    /**
     * Aggiorna lo snapshot in memoria e propaga il payload al physical adapter
     * del twin (creandolo se necessario).
     */
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

    /**
     * Mappa lo stato del paziente (statechart AnyLogic) allo stato della
     * missione (lifecycle DT).
     *
     * Mapping completo:
     * Signaled → Triaging (chiamata ricevuta, triage telefonico in corso)
     * WaitingSupport → Dispatched (veicolo inviato, in avvicinamento)
     * BeingTreated → OnScene (équipe sul posto, valutazione clinica)
     * MovingToHospital → Transporting (paziente a bordo, in viaggio verso ospedale)
     * AtHospital → Completed (handover completato, missione conclusa)
     * Completed → Completed (alias presente in alcune versioni della sim)
     * Handover → Completed (alias esplicito di handover)
     */
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

    /**
     * Attende che il twin abbia completato il binding (PAD pubblicata e stato
     * iniziale scritto nel DigitalTwinStateManager) prima di procedere con
     * il primo aggiornamento.
     */
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

    /**
     * Rimuove il twin dal registry dopo un ritardo per permettere:
     * 1. al CentralEmergencyDigitalAdapter di rilevare lo stato COMPLETED
     * e aggiornare i KPI (che richiede almeno un ciclo di polling),
     * 2. eventuali payload ritardati di veicoli di aggiornare l'hospitalId
     * prima della rimozione.
     *
     * Lo snapshot in missionSnapshots viene rimosso insieme al twin.
     * I dati storici della missione sono già preservati nel
     * completedMissionsHistory del CentralEmergencyDigitalAdapter.
     */
    private void scheduleCleanup(String missionId) {
        // Evita di scheduleCleanup più volte per la stessa missione
        // controllando se il twin è già stato rimosso
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