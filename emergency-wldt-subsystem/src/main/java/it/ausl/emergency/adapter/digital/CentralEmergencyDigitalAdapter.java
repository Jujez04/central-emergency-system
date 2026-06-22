package it.ausl.emergency.adapter.digital;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import it.ausl.emergency.adapter.configuration.CentraleOperativaAdapterConfiguration;
import it.ausl.emergency.utils.CentraleOperativaKeywords;
import it.wldt.adapter.digital.DigitalAdapter;
import it.wldt.core.state.DigitalTwinState;
import it.wldt.core.state.DigitalTwinStateChange;
import it.wldt.core.state.DigitalTwinStateEventNotification;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class CentralEmergencyDigitalAdapter extends DigitalAdapter<CentraleOperativaAdapterConfiguration> {

    private HttpServer restServer;
    private static final int PORT = 8080;

    public CentralEmergencyDigitalAdapter(String id, CentraleOperativaAdapterConfiguration configuration) {
        super(id, configuration);
    }

    @Override
    public void onAdapterStart() {
        try {
            startRestServer();
            System.out.println("[CentraleOperativaDigitalAdapter] -> REST API Layer attivo sulla porta: " + PORT);
        } catch (Exception e) {
            System.err.println("Impossibile avviare il server REST: " + e.getMessage());
        }
    }

    @Override
    public void onAdapterStop() {
        if (restServer != null) {
            restServer.stop(0);
            System.out.println("[CentraleOperativaDigitalAdapter] -> Server REST arrestato.");
        }
    }

    @Override
    public void onDigitalTwinSync(DigitalTwinState digitalTwinState) {
        System.out.println("[CentraleOperativaDigitalAdapter] -> Core state sincronizzato con il layer digitale.");
    }

    @Override
    public void onDigitalTwinUnSync(DigitalTwinState digitalTwinState) {}

    @Override
    public void onDigitalTwinCreate() {}

    @Override
    public void onDigitalTwinStart() {}

    @Override
    public void onDigitalTwinStop() {}

    @Override
    public void onDigitalTwinDestroy() {}

    private void startRestServer() throws IOException {
        restServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Endpoint 1: Triage Telefonico ed invio mezzo
        restServer.createContext("/api/centrale/triage", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String jsonBody = new String(exchange.getRequestBody().readAllBytes());
                    try {
                        // Pubblica l'azione sul bus di WLDT
                        publishDigitalActionWldtEvent(CentraleOperativaKeywords.ACTION_TRIAGE, jsonBody);
                        sendResponse(exchange, 202, "{\"status\":\"TRIAGE_ACCEPTED\"}");
                    } catch (Exception e) {
                        sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            }
        });

        // Endpoint 2: Reindirizzamento dell'ambulanza/elicottero in transito
        restServer.createContext("/api/centrale/redirect", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String jsonBody = new String(exchange.getRequestBody().readAllBytes());
                    try {
                        publishDigitalActionWldtEvent(CentraleOperativaKeywords.ACTION_REDIRECT, jsonBody);
                        sendResponse(exchange, 200, "{\"status\":\"REDIRECT_COMMAND_FORWARDED\"}");
                    } catch (Exception e) {
                        sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
                    }
                } else {
                    exchange.sendResponseHeaders(405, -1);
                }
            }
        });

        restServer.setExecutor(null);
        restServer.start();
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    @Override
    protected void onStateUpdate(DigitalTwinState newDigitalTwinState, DigitalTwinState previousDigitalTwinState,
            ArrayList<DigitalTwinStateChange> digitalTwinStateChangeList) {
    }

    @Override
    protected void onEventNotificationReceived(DigitalTwinStateEventNotification<?> digitalTwinStateEventNotification) {
    }
}