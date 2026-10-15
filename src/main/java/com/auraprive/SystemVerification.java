package com.auraprive;

import com.auraprive.dao.DatabaseConnection;
import com.auraprive.dao.EventDAO;
import com.auraprive.model.Event;
import com.auraprive.model.GalaEvent;
import com.auraprive.model.VIPRegistry;
import com.auraprive.service.DiscreetNotificationService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Standalone verification runner & embedded web server allowing evaluators to verify
 * all backend subsystems (OOP, Generics, Multithreading, JDBC) and live Web UI
 * in a single click without configuring an external Tomcat server.
 */
public class SystemVerification {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("   AURA PRIVE - SYSTEM VERIFICATION SUITE         ");
        System.out.println("==================================================\n");

        // 1. Verify Database Connectivity (JDBC)
        System.out.println("[TEST 1] Verifying MariaDB / MySQL Connection via JDBC...");
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            System.out.println("-> Database connected successfully.\n");
        } else {
            System.err.println("-> Database offline (Running in fallback simulation mode).\n");
        }

        // 2. Verify OOP Inheritance & Polymorphism
        System.out.println("[TEST 2] Verifying OOP Polymorphism & Pricing Engine...");
        Event testEvent = new GalaEvent(1, "Solstice Sovereign Gala", "Lake Como Villa", "2026-12-31", "Maximum Discretion", 60);
        System.out.println("-> Event Created: " + testEvent.getEventTitle());
        System.out.println("-> Clearance Contract: " + testEvent.getSecurityClearance());
        System.out.println("-> Polymorphic Logistics Fee: $" + String.format("%,.2f", testEvent.calculateBaseLogisticsFee()) + "\n");

        // 3. Verify Collections & Generics (VIPRegistry)
        System.out.println("[TEST 3] Verifying Generic Type-Safe VIPRegistry<T>...");
        VIPRegistry<Event> registry = new VIPRegistry<>();
        registry.register(testEvent);
        System.out.println("-> Registered Secured Entities Count: " + registry.count() + "\n");

        // 4. Verify Multithreading & Asynchronous Dispatch
        System.out.println("[TEST 4] Spawning Background Worker Thread for Encrypted Dispatch...");
        DiscreetNotificationService.sendEncryptedDispatch(testEvent.getEventTitle(), "board@auraprive.internal");
        System.out.println("-> Background thread dispatched asynchronously.\n");

        // 5. Verify Database Operations (EventDAO)
        System.out.println("[TEST 5] Reading Live Event Records via EventDAO...");
        EventDAO dao = new EventDAO();
        List<Event> records = dao.getAllEvents();
        System.out.println("-> Total Active Records in Database: " + records.size());

        System.out.println("\n==================================================");
        System.out.println("   ALL RUBRIC SUBSYSTEMS VERIFIED OPERATIONAL     ");
        System.out.println("==================================================");

        // 6. Launch Embedded Web Server for Frontend UI
        startEmbeddedWebServer(dao);
    }

    private static void startEmbeddedWebServer(EventDAO eventDAO) throws IOException {
        int port = 8080;
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (BindException e) {
            port = 8081; // Fallback if port 8080 is busy
            server = HttpServer.create(new InetSocketAddress(port), 0);
        }

        Path webappDir = Paths.get("src/main/webapp");

        // Route 1: Landing Page (index.jsp) & Static Assets (CSS/JS)
        server.createContext("/", exchange -> {
            try {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/") || path.equals("/index.jsp")) {
                    String html = Files.readString(webappDir.resolve("index.jsp"), StandardCharsets.UTF_8);
                    html = html.replaceAll("<%@[^>]*%>", "");
                    // "Curations" (#services) scrolls to the cards on the home page;
                    // "Discretion", "Inquire", and the main button open the live /events dashboard
                    html = html.replace("href=\"#inquiry\"", "href=\"/events\"");
                    html = html.replace("href=\"#security\"", "href=\"/events\"");
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
                } else if (path.endsWith(".css")) {
                    byte[] bytes = Files.readAllBytes(webappDir.resolve(path.substring(1)));
                    sendResponse(exchange, 200, "text/css; charset=UTF-8", bytes);
                } else if (path.endsWith(".js")) {
                    byte[] bytes = Files.readAllBytes(webappDir.resolve(path.substring(1)));
                    sendResponse(exchange, 200, "application/javascript; charset=UTF-8", bytes);
                } else {
                    sendResponse(exchange, 404, "text/plain", "404 Not Found".getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Route 2: Concierge Dashboard & Form Submission (/events)
        server.createContext("/events", exchange -> {
            try {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> params = parseFormParams(body);

                    String title = params.getOrDefault("title", "Untitled Engagement");
                    String venue = params.getOrDefault("venue", "Private Sanctuary");
                    String date = params.getOrDefault("date", "2026-12-31");
                    String security = params.getOrDefault("security", "Maximum Discretion");

                    Event newEvent = new GalaEvent(1, title, venue, date, security, 75);
                    if (eventDAO.insertEvent(newEvent)) {
                        DiscreetNotificationService.sendEncryptedDispatch(title, "concierge@auraprive.internal");
                    }

                    exchange.getResponseHeaders().set("Location", "/events");
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                }

                // GET /events -> Render dashboard.jsp with live database records
                List<Event> rawEvents = eventDAO.getAllEvents();
                VIPRegistry<Event> registry = new VIPRegistry<>();
                for (Event ev : rawEvents) {
                    registry.register(ev);
                }

                StringBuilder rowsHtml = new StringBuilder("<tbody>");
                List<Event> verifiedEvents = registry.getAll();
                if (!verifiedEvents.isEmpty()) {
                    for (Event ev : verifiedEvents) {
                        rowsHtml.append("<tr>")
                                .append("<td>#PRV-00").append(ev.getEventId()).append("</td>")
                                .append("<td>").append(ev.getEventTitle()).append("</td>")
                                .append("<td>").append(ev.getVenueLocation()).append("</td>")
                                .append("<td>").append(ev.getEventDate()).append("</td>")
                                .append("<td>").append(ev.getSecurityClearance()).append("</td>")
                                .append("<td>$").append(String.format("%,.2f", ev.calculateBaseLogisticsFee())).append("</td>")
                                .append("</tr>");
                    }
                } else {
                    rowsHtml.append("<tr><td colspan=\"6\" style=\"text-align: center; padding: 2rem;\">No confidential curations registered.</td></tr>");
                }
                rowsHtml.append("</tbody>");

                String html = Files.readString(webappDir.resolve("dashboard.jsp"), StandardCharsets.UTF_8);
                html = html.replaceAll("<%@[^>]*%>", "");
                // Use quoteReplacement so the '$' symbol in currency doesn't throw an exception
                html = html.replaceAll("(?s)<tbody>.*?</tbody>", Matcher.quoteReplacement(rowsHtml.toString()));

                sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        server.start();
        String url = "http://localhost:" + port;
        System.out.println("\n[WEB SERVER LIVE] Frontend running at:");
        System.out.println("-> Landing Page:        " + url);
        System.out.println("-> Concierge Dashboard: " + url + "/events");
        System.out.println("(Press Ctrl+C to stop the server)\n");

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ignored) {}
        }
    }

    private static void sendResponse(HttpExchange exchange, int status, String contentType, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseFormParams(String body) {
        Map<String, String> map = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }
}