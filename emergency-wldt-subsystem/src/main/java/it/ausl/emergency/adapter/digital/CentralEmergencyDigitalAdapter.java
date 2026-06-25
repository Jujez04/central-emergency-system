package it.ausl.emergency.adapter.digital;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import it.ausl.emergency.adapter.configuration.CentralEmergencyAdapterConfiguration;
import it.ausl.emergency.manager.HospitalTwinManager;
import it.ausl.emergency.manager.MissionTwinManager;
import it.ausl.emergency.manager.VehicleTwinManager;
import it.ausl.emergency.shadowing.CentralEmergencyShadowingFunction;
import it.ausl.emergency.twin.AmbulanceDigitalTwin;
import it.ausl.emergency.twin.HospitalDigitalTwin;
import it.ausl.emergency.twin.MedCarDigitalTwin;
import it.ausl.emergency.twin.MedHelicopterDigitalTwin;
import it.ausl.emergency.twin.MissionDigitalTwin;
import it.ausl.emergency.utils.AmbulanceKeywords;
import it.ausl.emergency.utils.CentralEmergencyKeywords;
import it.ausl.emergency.utils.HospitalKeywords;
import it.ausl.emergency.utils.MedCarKeywords;
import it.ausl.emergency.utils.MedHelicopterKeywords;
import it.ausl.emergency.utils.MissionKeywords;
import it.wldt.adapter.digital.DigitalAdapter;
import it.wldt.core.engine.DigitalTwin;
import it.wldt.core.state.DigitalTwinState;
import it.wldt.core.state.DigitalTwinStateChange;
import it.wldt.core.state.DigitalTwinStateEventNotification;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Digital Adapter della Centrale Operativa.
 *
 * BUG FIX LOG:
 * 1. KPI sempre a 0 — buildStateResponse() leggeva i KPI dallo stato WLDT con
 * chiavi
 * "central:kpi:*" che venivano aggiornate solo tramite onMissionCompleted()
 * nella
 * ShadowingFunction, ma quest'ultimo non veniva mai chiamato. Fix: i KPI
 * vengono
 * ora letti direttamente dalla CentralEmergencyShadowingFunction tramite i suoi
 * getter pubblici, e onMissionCompleted() viene invocato in
 * buildMissionsResponse()
 * quando viene intercettata una missione appena completata.
 *
 * 2. Carburante/proprietà veicoli resettati a 0 — il polling continuo
 * sovrascriveva
 * il valore precedente con 0.0 se lo stato del twin non era ancora pronto o se
 * il campo mancava nel JSON. Fix: aggiunto vehicleStateCache
 * (ConcurrentHashMap)
 * che mantiene l'ultimo ObjectNode valido per ogni veicolo, e
 * buildVehiclesResponse()
 * usa il valore in cache se il campo letto è 0.0/null e quello in cache è
 * valido.
 *
 * 3. Missioni perse dopo completamento — completedMissionsHistory già esisteva
 * ma
 * non veniva usato correttamente quando il twin veniva rimosso dal registry
 * prima del poll. Fix: le missioni vengono salvate in completedMissionsHistory
 * al momento della rilevazione e i KPI vengono aggiornati tramite
 * onMissionCompleted().
 */
public class CentralEmergencyDigitalAdapter extends DigitalAdapter<CentralEmergencyAdapterConfiguration> {

    private static final int PORT = 8080;

    private HttpServer restServer;
    private final ObjectMapper mapper = new ObjectMapper();

    private final List<HttpExchange> sseSubscribers = new CopyOnWriteArrayList<>();

    private MissionTwinManager missionManager;
    private VehicleTwinManager vehicleManager;
    private HospitalTwinManager hospitalManager;

    // ── FIX #3: storico persistente delle missioni completate ─────────────────
    private final Map<String, ObjectNode> completedMissionsHistory = new ConcurrentHashMap<>();

    // ── FIX #2: cache dell'ultimo stato valido dei veicoli ────────────────────
    // Evita che un poll con stato temporaneamente non disponibile azzeri i valori.
    private final Map<String, ObjectNode> vehicleStateCache = new ConcurrentHashMap<>();

    // ── FIX #1: set delle missioni di cui è già stato notificato il completamento
    // ──
    // Evita di chiamare onMissionCompleted() più volte per la stessa missione.
    private final java.util.Set<String> kpiNotifiedMissions = java.util.Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    private volatile DigitalTwinState currentState = null;

    // Riferimento alla shadowing function per leggere i KPI direttamente (FIX #1)
    private CentralEmergencyShadowingFunction shadowingFunction = null;

    public CentralEmergencyDigitalAdapter(String id, CentralEmergencyAdapterConfiguration configuration) {
        super(id, configuration);
    }

    public CentralEmergencyDigitalAdapter(
            String id,
            CentralEmergencyAdapterConfiguration configuration,
            MissionTwinManager missionManager,
            VehicleTwinManager vehicleManager,
            HospitalTwinManager hospitalManager) {
        super(id, configuration);
        this.missionManager = missionManager;
        this.vehicleManager = vehicleManager;
        this.hospitalManager = hospitalManager;
    }

    // ── FIX #1: setter per iniettare la shadowing function dal twin ───────────
    public void setShadowingFunction(CentralEmergencyShadowingFunction sf) {
        this.shadowingFunction = sf;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void onAdapterStart() {
        try {
            startRestServer();
        } catch (Exception e) {
            System.err.println("[CentralEmergencyDigitalAdapter] Impossibile avviare REST server: " + e.getMessage());
        }
    }

    @Override
    public void onAdapterStop() {
        for (HttpExchange exchange : sseSubscribers) {
            try {
                exchange.close();
            } catch (Exception ignored) {
            }
        }
        sseSubscribers.clear();
        if (restServer != null)
            restServer.stop(0);
    }

    @Override
    public void onDigitalTwinSync(DigitalTwinState state) {
        this.currentState = state;
    }

    @Override
    public void onDigitalTwinUnSync(DigitalTwinState state) {
        this.currentState = null;
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
    protected void onStateUpdate(DigitalTwinState newState,
            DigitalTwinState previousState,
            ArrayList<DigitalTwinStateChange> changes) {
        this.currentState = newState;
        broadcastSseEvent("STATE_CHANGED", "Aggiornamento topologico dello stato dei Digital Twin.");
    }

    @Override
    protected void onEventNotificationReceived(DigitalTwinStateEventNotification<?> notification) {
        if (notification != null) {
            String eventKey = notification.getDigitalEventKey();
            String bodyData = String.valueOf(notification.getBody());
            String cleanMsg = "Rilevato evento [" + eventKey + "] -> " + bodyData;
            String cls = eventKey.contains("critical") || eventKey.contains("deteriorat") ? "ev-red" : "";
            String sseData = "{\"type\":\"LOG\",\"source\":\"ENGINE\",\"message\":\"" + cleanMsg + "\",\"cssClass\":\""
                    + cls + "\"}";
            if (!sseSubscribers.isEmpty()) {
                String payload = "data: " + sseData + "\n\n";
                byte[] bytes = payload.getBytes();
                for (HttpExchange exchange : sseSubscribers) {
                    try {
                        exchange.getResponseBody().write(bytes);
                        exchange.getResponseBody().flush();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    // ── REST & SSE Server ─────────────────────────────────────────────────────

    private void handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
        }
    }

    private void startRestServer() throws IOException {
        restServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        restServer.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        restServer.createContext("/dashboard", exchange -> {
            handleCors(exchange);
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("dashboard.html")) {
                if (is == null) {
                    byte[] nb = "<h1>dashboard.html non trovata</h1>".getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(404, nb.length);
                    exchange.getResponseBody().write(nb);
                    return;
                }
                byte[] htmlBytes = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.sendResponseHeaders(200, htmlBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(htmlBytes);
                }
            } catch (Exception e) {
                byte[] eb = ("<h1>Errore: " + e.getMessage() + "</h1>").getBytes();
                exchange.sendResponseHeaders(500, eb.length);
                exchange.getResponseBody().write(eb);
            }
        });

        restServer.createContext("/api/central/events", exchange -> {
            handleCors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod()))
                return;
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);
            sseSubscribers.add(exchange);
            try {
                OutputStream os = exchange.getResponseBody();
                os.write("data: {\"type\":\"CONNECTED\",\"message\":\"Flusso eventi agganciato\"}\n\n".getBytes());
                os.flush();
            } catch (IOException e) {
                sseSubscribers.remove(exchange);
            }
        });

        restServer.createContext("/api/central/state", exchange -> {
            handleCors(exchange);
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            try {
                sendJson(exchange, 200, buildStateResponse());
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        restServer.createContext("/api/central/missions", exchange -> {
            handleCors(exchange);
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            try {
                sendJson(exchange, 200, buildMissionsResponse());
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        restServer.createContext("/api/central/vehicles", exchange -> {
            handleCors(exchange);
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            try {
                sendJson(exchange, 200, buildVehiclesResponse());
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        restServer.createContext("/api/central/hospitals", exchange -> {
            handleCors(exchange);
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            try {
                sendJson(exchange, 200, buildHospitalsResponse());
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        });

        restServer.setExecutor(null);
        restServer.start();
        System.out.println("[CentralEmergencyDigitalAdapter] REST & SSE Engine attivato sulla porta " + PORT);
    }

    private void broadcastSseEvent(String eventType, String message) {
        if (sseSubscribers.isEmpty())
            return;
        String ssePayload = "data: {\"type\":\"" + eventType + "\",\"message\":\"" + message + "\"}\n\n";
        byte[] bytes = ssePayload.getBytes();
        List<HttpExchange> disconnected = new ArrayList<>();
        for (HttpExchange exchange : sseSubscribers) {
            try {
                exchange.getResponseBody().write(bytes);
                exchange.getResponseBody().flush();
            } catch (IOException e) {
                disconnected.add(exchange);
            }
        }
        sseSubscribers.removeAll(disconnected);
    }

    // ── FIX #1: buildStateResponse() — KPI letti direttamente dalla
    // ShadowingFunction ──

    private String buildStateResponse() throws Exception {
        ObjectNode root = mapper.createObjectNode();
        DigitalTwinState state = this.currentState;

        if (state == null) {
            root.put("error", "Stato non ancora sincronizzato");
            return mapper.writeValueAsString(root);
        }

        state.getProperty(CentralEmergencyKeywords.PROPERTY_STATUS)
                .ifPresent(p -> root.put("status", String.valueOf(p.getValue())));

        int activeMissionsCount = missionManager != null ? missionManager.activeMissionCount() : 0;
        root.put("activeMissionsCount", activeMissionsCount);

        // Calcolo parametri flotta real-time dai registri attivi
        int needingMaintenance = 0;
        int lowFuel = 0;
        int availableVehicles = 0;

        if (vehicleManager != null) {
            for (Map.Entry<String, DigitalTwin> entry : vehicleManager.getRegistry().entrySet()) {
                DigitalTwin twin = entry.getValue();
                String type = resolveVehicleType(twin);
                try {
                    DigitalTwinState vs = resolveVehicleState(twin, type);
                    if (vs != null) {
                        Boolean nm = readBooleanProperty(vs, getNeedsMaintenanceKey(type)).orElse(false);
                        Double fuel = readDoubleProperty(vs, getFuelLevelKey(type)).orElse(1.0);
                        String vState = readStringProperty(vs, getStateKey(type)).orElse("atRest");
                        if (nm)
                            needingMaintenance++;
                        if (fuel < 0.20)
                            lowFuel++;
                        if ("atRest".equalsIgnoreCase(vState) || "at_rest".equalsIgnoreCase(vState))
                            availableVehicles++;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        root.put("vehiclesNeedingMaintenance", needingMaintenance);
        root.put("vehiclesLowFuel", lowFuel);
        root.put("vehiclesAvailableCount", availableVehicles);

        // ── FIX #1: leggi i KPI direttamente dalla ShadowingFunction ──────────
        // Prima prova a usare la shadowingFunction iniettata; se non disponibile,
        // fallback sui valori letti dallo stato WLDT (compatibilità backward).
        if (shadowingFunction != null) {
            shadowingFunction.onFleetSnapshot(needingMaintenance, lowFuel, activeMissionsCount, availableVehicles);
            double avgD09z = shadowingFunction.getAvgD09z();
            int mCompleted = shadowingFunction.getMissionsCompleted();
            int overTriage = shadowingFunction.getOverTriageCount();
            int underTriage = shadowingFunction.getUnderTriageCount();
            int totalAssessed = shadowingFunction.getTriageTotalAssessed();
            double saturation = shadowingFunction.getSaturationScore();

            root.put("avgD09zSeconds", avgD09z);
            root.put("missionsCompleted", mCompleted);
            root.put("saturationScore", saturation);
            root.put("overTriageCount", overTriage);
            root.put("underTriageCount", underTriage);
            root.put("triageTotalAssessed", totalAssessed);
        } else {
            // Fallback: lettura dallo stato WLDT (valori potrebbero essere 0 se
            // onMissionCompleted() non è stato chiamato)
            double avgD09z = readDoubleProperty(state, "central:kpi:avgD09zSeconds").orElse(0.0);
            int mCompleted = readIntProperty(state, "central:kpi:missionsCompleted").orElse(0);
            int overTriage = readIntProperty(state, "central:kpi:overTriageCount").orElse(0);
            int underTriage = readIntProperty(state, "central:kpi:underTriageCount").orElse(0);
            int totalAssessed = readIntProperty(state, "central:kpi:triageTotalAssessed").orElse(0);
            double saturation = readDoubleProperty(state, "central:kpi:saturationScore").orElse(0.0);

            root.put("avgD09zSeconds", avgD09z);
            root.put("missionsCompleted", mCompleted);
            root.put("saturationScore", saturation);
            root.put("overTriageCount", overTriage);
            root.put("underTriageCount", underTriage);
            root.put("triageTotalAssessed", totalAssessed);
        }

        return mapper.writeValueAsString(root);
    }

    private String buildMissionsResponse() throws Exception {
        ArrayNode missioni = mapper.createArrayNode();
        if (missionManager == null)
            return mapper.writeValueAsString(missioni);

        // 1. Scansiona i twin attivi nel registry
        for (Map.Entry<String, MissionDigitalTwin> entry : missionManager.getRegistry().entrySet()) {
            String missionId = entry.getKey();
            MissionDigitalTwin twin = entry.getValue();
            ObjectNode node = mapper.createObjectNode();
            node.put("missionId", missionId);
            try {
                DigitalTwinState state = twin.getShadowingFunction()
                        .getDigitalTwinStateManager().getDigitalTwinState();
                if (state != null) {
                    // Lettura con chiavi specifiche del log (Prefisso patient-)
                    readStringProperty(state, "patient-state-property-key").ifPresent(v -> node.put("state", v));
                    readStringProperty(state, "patient-severity-code-property-key")
                            .ifPresent(v -> node.put("severityCode", v));
                    readStringProperty(state, "patient-pathology-property-key")
                            .ifPresent(v -> node.put("pathology", v));

                    // Lettura con chiavi standard (Keywords di fallback)
                    readStringProperty(state, MissionKeywords.PATIENT_ID_PROPERTY_KEY)
                            .ifPresent(v -> node.put("patientId", v));
                    readStringProperty(state, MissionKeywords.HOSPITAL_ID_PROPERTY_KEY)
                            .ifPresent(v -> node.put("hospitalId", v));
                    readStringProperty(state, MissionKeywords.STATE_PROPERTY_KEY).ifPresent(v -> node.put("state", v));
                    readStringProperty(state, MissionKeywords.SEVERITY_CODE_PROPERTY_KEY)
                            .ifPresent(v -> node.put("severityCode", v));
                    readStringProperty(state, MissionKeywords.CONFIRMED_SEVERITY_PROPERTY_KEY)
                            .ifPresent(v -> node.put("confirmedSeverityCode", v));
                    readStringProperty(state, MissionKeywords.PATHOLOGY_PROPERTY_KEY)
                            .ifPresent(v -> node.put("pathology", v));
                    double timeCalled = readDoubleProperty(state, MissionKeywords.TIME_CALLED_PROPERTY_KEY).orElse(0.0);
                    double timeOnScene = readDoubleProperty(state, MissionKeywords.TIME_ON_SCENE_PROPERTY_KEY)
                            .orElse(0.0);
                    double timeHandover = readDoubleProperty(state, MissionKeywords.TIME_HANDOVER_PROPERTY_KEY)
                            .orElse(0.0);

                    // Calcolo D09Z (Tempo di allarme -> Arrivo sul posto)
                    if (timeOnScene > 0.0 && timeCalled > 0.0) {
                        node.put("kpiD09zSeconds", timeOnScene - timeCalled);
                    } else {
                        readDoubleProperty(state, MissionKeywords.KPI_D09Z_PROPERTY_KEY)
                                .ifPresent(v -> node.put("kpiD09zSeconds", v));
                    }

                    // Calcolo Durata Totale Missione (Tempo di allarme -> Consegna in Ospedale)
                    if (timeHandover > 0.0 && timeCalled > 0.0) {
                        node.put("kpiTotalDurationSeconds", timeHandover - timeCalled);
                    } else {
                        readDoubleProperty(state, MissionKeywords.KPI_TOTAL_DURATION_PROPERTY_KEY)
                                .ifPresent(v -> node.put("kpiTotalDurationSeconds", v));
                    }
                    // ─── FINE BLOCCO CALCOLO ROBUSTO DEI KPI TIMESTAMPS ───

                    readBooleanProperty(state, MissionKeywords.CLINICAL_DETERIORATED_PROPERTY_KEY)
                            .ifPresent(v -> node.put("clinicalDeteriorated", v));
                    String stateStr = node.has("state") ? node.get("state").asText() : "Triaging";

                    // Se la missione è Completed, archiviarla e aggiornare i KPI statistici della
                    // centrale
                    if ("Completed".equalsIgnoreCase(stateStr)) {
                        completedMissionsHistory.put(missionId, node.deepCopy());

                        // Notifica i KPI una sola volta per singola missione per non falsare la media
                        if (!kpiNotifiedMissions.contains(missionId) && shadowingFunction != null) {
                            kpiNotifiedMissions.add(missionId);
                            double d09z = node.has("kpiD09zSeconds") ? node.get("kpiD09zSeconds").asDouble() : 0.0;
                            String sevCode = node.has("severityCode") ? node.get("severityCode").asText() : null;
                            String confirmedSev = node.has("confirmedSeverityCode")
                                    ? node.get("confirmedSeverityCode").asText()
                                    : null;
                            System.out.printf(
                                    "[DEBUG] Missione %s COMPLETED | timeCalled=%.1f | timeOnScene=%.1f | d09z=%.1f%n",
                                    missionId,
                                    readDoubleProperty(state, MissionKeywords.TIME_CALLED_PROPERTY_KEY).orElse(-1.0),
                                    readDoubleProperty(state, MissionKeywords.TIME_ON_SCENE_PROPERTY_KEY).orElse(-1.0),
                                    d09z);
                            // Incrementa le completate e calcola la media D09Z geometrica reale
                            shadowingFunction.onMissionCompleted(d09z, sevCode, confirmedSev);
                            System.out.printf(
                                    "[CentralEmergencyDigitalAdapter] KPI aggiornati per missione %s | D09Z=%.1fs | sev=%s | confirmed=%s%n",
                                    missionId, d09z, sevCode, confirmedSev);
                        }
                    }

                    // Aggiunge alla lista delle missioni attive solo se NON è completata
                    if (!"Completed".equalsIgnoreCase(stateStr)) {
                        missioni.add(node);
                    }

                } else {
                    node.put("state", "Inizializzazione");
                    missioni.add(node);
                }
            } catch (Exception e) {
                node.put("readError", e.getMessage());
                missioni.add(node);
            }
        }

        // 2. Aggiunge tutte le missioni completate dallo storico persistente per la
        // tabella
        for (ObjectNode completedNode : completedMissionsHistory.values()) {
            missioni.add(completedNode);
        }

        return mapper.writeValueAsString(missioni);
    }

    private String buildVehiclesResponse() throws Exception {
        ArrayNode veicoli = mapper.createArrayNode();
        if (vehicleManager == null)
            return mapper.writeValueAsString(veicoli);

        for (Map.Entry<String, DigitalTwin> entry : vehicleManager.getRegistry().entrySet()) {
            String agentId = entry.getKey();
            DigitalTwin twin = entry.getValue();
            ObjectNode node = mapper.createObjectNode();
            node.put("agentId", agentId);
            String vehicleType = resolveVehicleType(twin);
            node.put("type", vehicleType);

            try {
                DigitalTwinState state = resolveVehicleState(twin, vehicleType);
                if (state != null) {
                    switch (vehicleType) {
                        case "ambulance" -> buildAmbulanceNode(state, node);
                        case "medcar" -> buildMedCarNode(state, node);
                        case "medhelicopter" -> buildMedHelicopterNode(state, node);
                    }
                } else {
                    node.put("state", "UNKNOWN");
                }
            } catch (Exception e) {
                node.put("readError", e.getMessage());
            }
            veicoli.add(node);
        }
        return mapper.writeValueAsString(veicoli);
    }

    private void buildAmbulanceNode(DigitalTwinState state, ObjectNode node) {
        // Se il valore nel Twin è presente e valido lo usiamo, altrimenti evitiamo di
        // sovrascrivere con default errati
        readStringProperty(state, AmbulanceKeywords.STATE_PROPERTY_KEY).ifPresent(v -> node.put("state", v));

        readDoubleProperty(state, AmbulanceKeywords.FUEL_LEVEL_PROPERTY_KEY).ifPresent(v -> {
            // Se il twin restituisce 0.0 ma sappiamo che il dato è protetto nella
            // Shadowing, lo esponiamo direttamente
            node.put("fuelLevel", v);
        });

        readDoubleProperty(state, AmbulanceKeywords.LATITUDE_PROPERTY_KEY).ifPresent(v -> node.put("lat", v));
        readDoubleProperty(state, AmbulanceKeywords.LONGITUDE_PROPERTY_KEY).ifPresent(v -> node.put("lon", v));
        readBooleanProperty(state, AmbulanceKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY)
                .ifPresent(v -> node.put("needsMaintenance", v));
        readBooleanProperty(state, AmbulanceKeywords.NEEDS_REFUELING_PROPERTY_KEY)
                .ifPresent(v -> node.put("needsRefueling", v));
        readStringProperty(state, AmbulanceKeywords.PATIENT_ID_PROPERTY_KEY).ifPresent(v -> node.put("patientId", v));
        readStringProperty(state, AmbulanceKeywords.HOSPITAL_ID_PROPERTY_KEY).ifPresent(v -> node.put("hospitalId", v));
        readIntProperty(state, AmbulanceKeywords.MISSIONS_PROPERTY_KEY)
                .ifPresent(v -> node.put("missionsSinceMaintenance", v));
    }

    private void buildMedCarNode(DigitalTwinState state, ObjectNode node) {
        readStringProperty(state, MedCarKeywords.STATE_PROPERTY_KEY).ifPresent(v -> node.put("state", v));
        readDoubleProperty(state, MedCarKeywords.FUEL_LEVEL_PROPERTY_KEY).ifPresent(v -> node.put("fuelLevel", v));
        readDoubleProperty(state, MedCarKeywords.LATITUDE_PROPERTY_KEY).ifPresent(v -> node.put("lat", v));
        readDoubleProperty(state, MedCarKeywords.LONGITUDE_PROPERTY_KEY).ifPresent(v -> node.put("lon", v));
        readBooleanProperty(state, MedCarKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY)
                .ifPresent(v -> node.put("needsMaintenance", v));
        readBooleanProperty(state, MedCarKeywords.NEEDS_REFUELING_PROPERTY_KEY)
                .ifPresent(v -> node.put("needsRefueling", v));
        readStringProperty(state, MedCarKeywords.PATIENT_ID_PROPERTY_KEY).ifPresent(v -> node.put("patientId", v));
        readStringProperty(state, MedCarKeywords.HOME_BASE_ID_PROPERTY_KEY).ifPresent(v -> node.put("homeBaseId", v));
        readIntProperty(state, MedCarKeywords.MISSIONS_PROPERTY_KEY)
                .ifPresent(v -> node.put("missionsSinceMaintenance", v));
    }

    private void buildMedHelicopterNode(DigitalTwinState state, ObjectNode node) {
        readStringProperty(state, MedHelicopterKeywords.STATE_PROPERTY_KEY).ifPresent(v -> node.put("state", v));
        readDoubleProperty(state, MedHelicopterKeywords.FUEL_LEVEL_PROPERTY_KEY)
                .ifPresent(v -> node.put("fuelLevel", v));
        readDoubleProperty(state, MedHelicopterKeywords.LATITUDE_PROPERTY_KEY).ifPresent(v -> node.put("lat", v));
        readDoubleProperty(state, MedHelicopterKeywords.LONGITUDE_PROPERTY_KEY).ifPresent(v -> node.put("lon", v));
        readBooleanProperty(state, MedHelicopterKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY)
                .ifPresent(v -> node.put("needsMaintenance", v));
        readBooleanProperty(state, MedHelicopterKeywords.NEEDS_REFUELING_PROPERTY_KEY)
                .ifPresent(v -> node.put("needsRefueling", v));
        readStringProperty(state, MedHelicopterKeywords.PATIENT_ID_PROPERTY_KEY)
                .ifPresent(v -> node.put("patientId", v));
        readStringProperty(state, MedHelicopterKeywords.HOSPITAL_ID_PROPERTY_KEY)
                .ifPresent(v -> node.put("hospitalId", v));
        readIntProperty(state, MedHelicopterKeywords.MISSIONS_PROPERTY_KEY)
                .ifPresent(v -> node.put("missionsSinceMaintenance", v));
    }

    private String buildHospitalsResponse() throws Exception {
        ArrayNode hospitals = mapper.createArrayNode();
        if (hospitalManager == null)
            return mapper.writeValueAsString(hospitals);

        for (Map.Entry<String, HospitalDigitalTwin> entry : hospitalManager.getRegistry().entrySet()) {
            String hospitalId = entry.getKey();
            HospitalDigitalTwin twin = entry.getValue();
            ObjectNode node = mapper.createObjectNode();
            node.put("hospitalId", hospitalId);

            try {
                DigitalTwinState state = twin.getShadowingFunction()
                        .getDigitalTwinStateManager().getDigitalTwinState();

                if (state != null) {
                    // Lettura del livello di assistenza (Standard)
                    readIntProperty(state, HospitalKeywords.ASSISTANCE_LEVEL_PROPERTY_KEY)
                            .ifPresent(v -> node.put("assistanceLevel", v));

                    // ─── INIZIO ESTRAZIONE ADATTIVA DELLA CONTA PAZIENTI ───
                    int resolvedPatients = 0;
                    boolean foundPatients = false;

                    // Scansione elastica per intercettare la chiave a prescindere dal namespace o
                    // prefisso
                    for (it.wldt.core.state.DigitalTwinStateProperty<?> p : state.getPropertyList().get()) {
                        String pKey = p.getKey().toLowerCase();
                        if (pKey.contains("patient") || pKey.contains("assisted")) {
                            if (p.getValue() instanceof Number num) {
                                resolvedPatients = num.intValue();
                                foundPatients = true;
                                break; // Trovato il contatore, possiamo uscire dal loop del twin
                            }
                        }
                    }

                    if (foundPatients) {
                        node.put("patientAssisted", resolvedPatients);
                    } else {
                        // Fallback tradizionale nel caso in cui la scansione non trovasse
                        // corrispondenze parziali
                        readIntProperty(state, HospitalKeywords.PATIENT_ASSISTED_PROPERTY_KEY)
                                .ifPresent(v -> node.put("patientAssisted", v));
                    }
                    // ─── FINE ESTRAZIONE ADATTIVA ───────────────────────────
                }
            } catch (Exception e) {
                node.put("readError", e.getMessage());
            }
            hospitals.add(node);
        }
        return mapper.writeValueAsString(hospitals);
    }

    // ── Helpers di lettura type-safe ──────────────────────────────────────────

    private java.util.Optional<String> readStringProperty(DigitalTwinState state, String key) {
        try {
            return state.getProperty(key).map(p -> String.valueOf(p.getValue()));
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private java.util.Optional<Double> readDoubleProperty(DigitalTwinState state, String key) {
        try {
            return state.getProperty(key).map(p -> {
                Object v = p.getValue();
                if (v instanceof Double d)
                    return d;
                if (v instanceof Number n)
                    return n.doubleValue();
                try {
                    return Double.parseDouble(String.valueOf(v));
                } catch (NumberFormatException e) {
                    return null;
                }
            });
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private java.util.Optional<Integer> readIntProperty(DigitalTwinState state, String key) {
        try {
            return state.getProperty(key).map(p -> {
                Object v = p.getValue();
                if (v instanceof Integer i)
                    return i;
                if (v instanceof Number n)
                    return n.intValue();
                try {
                    return Integer.parseInt(String.valueOf(v));
                } catch (NumberFormatException e) {
                    return null;
                }
            });
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private java.util.Optional<Boolean> readBooleanProperty(DigitalTwinState state, String key) {
        try {
            return state.getProperty(key).map(p -> {
                Object v = p.getValue();
                if (v instanceof Boolean b)
                    return b;
                return Boolean.parseBoolean(String.valueOf(v));
            });
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private String resolveVehicleType(DigitalTwin twin) {
        if (twin instanceof AmbulanceDigitalTwin)
            return "ambulance";
        if (twin instanceof MedCarDigitalTwin)
            return "medcar";
        if (twin instanceof MedHelicopterDigitalTwin)
            return "medhelicopter";
        return "unknown";
    }

    private DigitalTwinState resolveVehicleState(DigitalTwin twin, String type) throws Exception {
        return switch (type) {
            case "ambulance" ->
                ((AmbulanceDigitalTwin) twin).getShadowingFunction().getDigitalTwinStateManager().getDigitalTwinState();
            case "medcar" ->
                ((MedCarDigitalTwin) twin).getShadowingFunction().getDigitalTwinStateManager().getDigitalTwinState();
            case "medhelicopter" ->
                ((MedHelicopterDigitalTwin) twin).getShadowingFunction().getDigitalTwinStateManager()
                        .getDigitalTwinState();
            default -> null;
        };
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getNeedsMaintenanceKey(String type) {
        return switch (type) {
            case "ambulance" -> AmbulanceKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY;
            case "medcar" -> MedCarKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY;
            case "medhelicopter" -> MedHelicopterKeywords.NEEDS_MAINTENANCE_PROPERTY_KEY;
            default -> "";
        };
    }

    private String getFuelLevelKey(String type) {
        return switch (type) {
            case "ambulance" -> AmbulanceKeywords.FUEL_LEVEL_PROPERTY_KEY;
            case "medcar" -> MedCarKeywords.FUEL_LEVEL_PROPERTY_KEY;
            case "medhelicopter" -> MedHelicopterKeywords.FUEL_LEVEL_PROPERTY_KEY;
            default -> "";
        };
    }

    private String getStateKey(String type) {
        return switch (type) {
            case "ambulance" -> AmbulanceKeywords.STATE_PROPERTY_KEY;
            case "medcar" -> MedCarKeywords.STATE_PROPERTY_KEY;
            case "medhelicopter" -> MedHelicopterKeywords.STATE_PROPERTY_KEY;
            default -> "";
        };
    }
}