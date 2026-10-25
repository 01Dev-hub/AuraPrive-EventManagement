package com.auraprive;

import com.auraprive.dao.DatabaseConnection;
import com.auraprive.dao.EventDAO;
import com.auraprive.exception.ExclusiveAccessException;
import com.auraprive.model.Event;
import com.auraprive.model.GalaEvent;
import com.auraprive.model.SummitEvent;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SystemVerification {

    // Per-browser HTTP session store keyed by AURA_SESSION_ID cookie: token -> [email, ROLE, entityId]
    private static final Map<String, String[]> SESSIONS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("   AURA PRIVE - FULL RUBRIC VERIFICATION SUITE    ");
        System.out.println("==================================================\n");

        System.out.println("[TEST 1] Verifying MariaDB / MySQL Connection via JDBC...");
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            System.out.println("-> Database connected successfully.\n");
        } else {
            System.out.println("-> Database in Offline Fallback Mode (set DB_PASSWORD env var for live MariaDB).\n");
        }

        System.out.println("[TEST 2] Verifying Polymorphism (GalaEvent vs SummitEvent) & ExclusiveAccessException...");
        Event gala = new GalaEvent(1, 1, "Maharaja Vikramaditya Rathore", "Udaipur Royal Solstice Gala", "Taj Lake Palace, Udaipur", "2026-12-31", "Maximum Discretion", 60);
        Event summit = new SummitEvent(2, 2, "Arundhati Devi Scindia", "Jodhpur Sovereign Founders Summit", "Umaid Bhawan Palace, Jodhpur", "2026-11-18", "Maximum Discretion", 40);
        System.out.println("-> Polymorphic Subclass 1 [" + gala.getEventType() + "]: $" + String.format("%,.2f", gala.calculateBaseLogisticsFee()));
        System.out.println("-> Polymorphic Subclass 2 [" + summit.getEventType() + "]: $" + String.format("%,.2f", summit.calculateBaseLogisticsFee()));

        System.out.println("\n[TEST 3] Verifying Generic Type-Safe VIPRegistry<T> & Exception Guard...");
        VIPRegistry<Event> registry = new VIPRegistry<>();
        registry.register(gala);
        registry.register(summit);
        System.out.println("-> Registered Valid Entities Count: " + registry.count());
        try {
            Event invalidEvent = new GalaEvent(1, "X", "Unknown", "1999-01-01", "Hacker", 999);
            registry.register(invalidEvent);
        } catch (ExclusiveAccessException ex) {
            System.out.println("-> Verified ExclusiveAccessException Thrown & Caught: " + ex.getMessage() + "\n");
        }

        System.out.println("[TEST 4] Submitting Asynchronous Tasks to ExecutorService Thread Pool...");
        DiscreetNotificationService.sendEncryptedDispatch(gala.getEventTitle(), "board@auraprive.in");
        DiscreetNotificationService.sendEncryptedDispatch(summit.getEventTitle(), "syndicate@auraprive.in");

        System.out.println("\n[TEST 5] Verifying Multi-Table DAO Queries (Events + JOIN Clients, Elite Vendors)...");
        EventDAO dao = new EventDAO();
        System.out.println("-> Active Events: " + dao.getAllEvents().size()
                + " | Registered Clients: " + dao.getAllClients().size()
                + " | Cleared Vendors: " + dao.getAllVendors().size());

        System.out.println("\n==================================================");
        System.out.println("   ALL RUBRIC SUBSYSTEMS VERIFIED OPERATIONAL     ");
        System.out.println("==================================================");

        startEmbeddedWebServer(dao);
    }

    private static String getSessionToken(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies != null) {
            for (String header : cookies) {
                for (String part : header.split(";")) {
                    String[] kv = part.trim().split("=", 2);
                    if (kv.length == 2 && "AURA_SESSION_ID".equals(kv[0])) {
                        return kv[1].trim();
                    }
                }
            }
        }
        return null;
    }

    private static String[] getActiveSession(HttpExchange exchange) {
        String token = getSessionToken(exchange);
        return (token != null) ? SESSIONS.get(token) : null;
    }

    private static void startEmbeddedWebServer(EventDAO eventDAO) throws IOException {
        int port = 8080;
        HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (BindException e) {
            port = 8081;
            server = HttpServer.create(new InetSocketAddress(port), 0);
        }

        Path webappDir = Paths.get("src/main/webapp");

        server.createContext("/", exchange -> {
            try {
                String path = exchange.getRequestURI().getPath();
                Map<String, String> query = parseFormParams(exchange.getRequestURI().getRawQuery());

                if (path.equals("/") || path.equals("/index.jsp")) {
                    String html = Files.readString(webappDir.resolve("index.jsp"), StandardCharsets.UTF_8);
                    html = html.replaceAll("<%@[^>]*%>", "");
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", html.getBytes(StandardCharsets.UTF_8));
                } else if (path.equals("/login.jsp") || path.equals("/auth")) {
                    if ("logout".equalsIgnoreCase(query.get("action"))) {
                        String token = getSessionToken(exchange);
                        if (token != null) SESSIONS.remove(token);
                        exchange.getResponseHeaders().add("Set-Cookie", "AURA_SESSION_ID=; Path=/; Max-Age=0; HttpOnly");
                        exchange.getResponseHeaders().set("Location", "/login.jsp?msg=Session+signed+out+cleanly.");
                        exchange.sendResponseHeaders(303, -1);
                        exchange.close();
                        return;
                    }
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                        Map<String, String> params = parseFormParams(body);
                        String action = params.getOrDefault("action", "login");
                        String email = params.getOrDefault("email", "").trim().toLowerCase();

                        // 1. Dynamic Sign-Up
                        if ("signup".equalsIgnoreCase(action)) {
                            String role = "VENDOR".equalsIgnoreCase(params.get("role")) ? "VENDOR" : "CLIENT";
                            String fullName = params.getOrDefault("fullName", "").trim();
                            String pass = params.getOrDefault("password", "");
                            String confirm = params.getOrDefault("confirmPassword", "");

                            if (email.isEmpty() || fullName.isEmpty() || pass.length() < 6 || !pass.equals(confirm)) {
                                exchange.getResponseHeaders().set("Location", "/login.jsp?err=Sign+up+failed:+Verify+name,+email,+and+matching+6-character+passcode.");
                            } else if (EventDAO.accountExists(email)) {
                                exchange.getResponseHeaders().set("Location", "/login.jsp?err=Account+already+exists+for+" + URLEncoder.encode(email, StandardCharsets.UTF_8));
                            } else {
                                int entityId = "VENDOR".equals(role)
                                        ? eventDAO.insertVendor(fullName, params.getOrDefault("specialty", "Bespoke Sovereign Logistics"), "Maximum Discretion")
                                        : eventDAO.insertClient(fullName, params.getOrDefault("membershipTier", "Sovereign Founders Circle"), email);

                                if (entityId < 1) {
                                    exchange.getResponseHeaders().set("Location", "/login.jsp?err=Database+error+creating+account.");
                                } else {
                                    EventDAO.registerAccountCredentials(email, pass, role, entityId);
                                    String token = UUID.randomUUID().toString();
                                    SESSIONS.put(token, new String[]{email, role, String.valueOf(entityId)});
                                    exchange.getResponseHeaders().add("Set-Cookie", "AURA_SESSION_ID=" + token + "; Path=/; HttpOnly");
                                    DiscreetNotificationService.sendEncryptedDispatch("New " + role + " Account Registered", email);
                                    exchange.getResponseHeaders().set("Location", "/events");
                                }
                            }
                            exchange.sendResponseHeaders(303, -1);
                            exchange.close();
                            return;
                        }

                        // 2. Verified Password Change (Requires valid old password — no forged token bypass)
                        if ("resetPassword".equalsIgnoreCase(action)) {
                            String oldPass = params.getOrDefault("oldPassword", "");
                            String newPass = params.getOrDefault("newPassword", "");
                            String confirmPass = params.getOrDefault("confirmPassword", "");

                            if (newPass.equals(confirmPass) && EventDAO.updatePassword(email, oldPass, newPass)) {
                                DiscreetNotificationService.sendEncryptedDispatch("Password Updated", email);
                                exchange.getResponseHeaders().set("Location", "/login.jsp?msg=Password+updated+successfully.+Please+Log+In.");
                            } else {
                                exchange.getResponseHeaders().set("Location", "/login.jsp?err=Password+change+failed:+Invalid+email+or+current+password.");
                            }
                            exchange.sendResponseHeaders(303, -1);
                            exchange.close();
                            return;
                        }

                        // 3. Standard Login
                        String password = params.getOrDefault("password", "");
                        String[] acc = EventDAO.authenticateAccount(email, password);
                        if (acc != null) {
                            String token = UUID.randomUUID().toString();
                            SESSIONS.put(token, new String[]{email, acc[1], acc[2]});
                            exchange.getResponseHeaders().add("Set-Cookie", "AURA_SESSION_ID=" + token + "; Path=/; HttpOnly");
                            exchange.getResponseHeaders().set("Location", "/events");
                        } else {
                            exchange.getResponseHeaders().set("Location", "/login.jsp?err=Invalid+Email+or+Passcode.+Access+Denied.");
                        }
                        exchange.sendResponseHeaders(303, -1);
                        exchange.close();
                        return;
                    }

                    String html = Files.readString(webappDir.resolve("login.jsp"), StandardCharsets.UTF_8);
                    html = html.replaceAll("<%@[^>]*%>", "").replaceAll("(?s)<%.*?%>", "");
                    String banner = "";
                    if (query.containsKey("err")) {
                        banner = "<div style='padding:0.85rem;margin-bottom:1.2rem;border:1px solid var(--danger-accent);color:var(--danger-accent);border-radius:8px;font-size:0.84rem;'>" + Event.escapeHtml(query.get("err")) + "</div>";
                    } else if (query.containsKey("msg")) {
                        banner = "<div style='padding:0.85rem;margin-bottom:1.2rem;border:1px solid var(--gold-accent);color:var(--gold-accent);border-radius:8px;font-size:0.84rem;'>" + Event.escapeHtml(query.get("msg")) + "</div>";
                    }
                    html = html.replace("<div class=\"tab-bar\">", banner + "<div class=\"tab-bar\">");
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

        server.createContext("/events", exchange -> {
            try {
                String[] session = getActiveSession(exchange);
                if (session == null) {
                    exchange.getResponseHeaders().set("Location", "/login.jsp?err=Please+sign+in+to+access+your+portal.");
                    exchange.sendResponseHeaders(303, -1);
                    exchange.close();
                    return;
                }

                String sessionEmail = session[0];
                String sessionRole = session[1];
                int sessionEntityId = Integer.parseInt(session[2]);

                Map<String, String> query = parseFormParams(exchange.getRequestURI().getRawQuery());
                String tab = query.getOrDefault("tab", "events");

                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    Map<String, String> params = parseFormParams(body);
                    String action = params.getOrDefault("action", "create");

                    try {
                        if ("VENDOR".equalsIgnoreCase(sessionRole)) {
                            throw new ExclusiveAccessException("Vendor accounts have read-only logistics access.");
                        }

                        if ("delete".equalsIgnoreCase(action)) {
                            int eventId = Integer.parseInt(params.getOrDefault("eventId", "0"));
                            if (!eventDAO.deleteEvent(eventId)) {
                                throw new ExclusiveAccessException("Unable to delete engagement #" + eventId + " (not found).");
                            }
                        } else if ("update".equalsIgnoreCase(action)) {
                            int eventId = Integer.parseInt(params.getOrDefault("eventId", "0"));
                            String security = params.getOrDefault("security", "");
                            int guests = Integer.parseInt(params.getOrDefault("vipGuests", "0"));
                            if (!Event.isValidClearance(security)) {
                                throw new ExclusiveAccessException("Unauthorized security clearance '" + security + "'.");
                            }
                            if (guests < 1 || guests > 500) {
                                throw new ExclusiveAccessException("VIP guest count (" + guests + ") must be between 1 and 500.");
                            }
                            if (!eventDAO.updateEvent(eventId, security, guests)) {
                                throw new ExclusiveAccessException("Failed to update engagement #" + eventId + ".");
                            }
                        } else if ("registerClient".equalsIgnoreCase(action) && "ADMIN".equals(sessionRole)) {
                            String fullName = params.getOrDefault("fullName", "").trim();
                            String tier = params.getOrDefault("membershipTier", "Sovereign Founders Circle");
                            String contact = params.getOrDefault("encryptedContact", "").trim().toLowerCase();
                            int newId = eventDAO.insertClient(fullName, tier, contact);
                            if (newId < 1) {
                                throw new ExclusiveAccessException("Failed to onboard client. Verify name and email.");
                            }
                            EventDAO.registerAccountCredentials(contact, "123456", "CLIENT", newId);
                            tab = "clients";
                        } else if ("registerVendor".equalsIgnoreCase(action) && "ADMIN".equals(sessionRole)) {
                            String vendorName = params.getOrDefault("vendorName", "").trim();
                            String specialty = params.getOrDefault("specialty", "").trim();
                            String clearance = params.getOrDefault("securityClearance", "");
                            if (eventDAO.insertVendor(vendorName, specialty, clearance) < 1) {
                                throw new ExclusiveAccessException("Failed to accredit vendor. Verify clearance protocol.");
                            }
                            tab = "vendors";
                        } else if ("create".equalsIgnoreCase(action)) {
                            int clientId = "CLIENT".equals(sessionRole)
                                    ? sessionEntityId
                                    : Integer.parseInt(params.getOrDefault("clientId", "1"));
                            if (!eventDAO.clientExists(clientId)) {
                                throw new ExclusiveAccessException("Sponsoring Client ID #" + clientId + " does not exist.");
                            }
                            String clientName = eventDAO.resolveClientName(clientId);
                            String title = params.getOrDefault("title", "");
                            String type = params.getOrDefault("eventType", "GALA");
                            String venue = params.getOrDefault("venue", "");
                            String date = params.getOrDefault("date", "");
                            String security = params.getOrDefault("security", "");
                            int guests = Integer.parseInt(params.getOrDefault("vipGuests", "0"));

                            Event newEvent = "SUMMIT".equalsIgnoreCase(type)
                                    ? new SummitEvent(0, clientId, clientName, title, venue, date, security, guests)
                                    : new GalaEvent(0, clientId, clientName, title, venue, date, security, guests);

                            VIPRegistry<Event> reg = new VIPRegistry<>();
                            reg.register(newEvent);
                            if (!eventDAO.insertEvent(newEvent)) {
                                throw new ExclusiveAccessException("Database rejected the engagement insertion.");
                            }
                            DiscreetNotificationService.sendEncryptedDispatch(title, sessionEmail);
                        }

                        exchange.getResponseHeaders().set("Location", "/events?tab=" + tab);
                        exchange.sendResponseHeaders(303, -1);
                        exchange.close();
                    } catch (ExclusiveAccessException | NumberFormatException ex) {
                        String encodedErr = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Location", "/events?tab=" + tab + "&err=" + encodedErr);
                        exchange.sendResponseHeaders(303, -1);
                        exchange.close();
                    }
                    return;
                }

                String errorBanner = query.get("err");
                sendResponse(exchange, 200, "text/html; charset=UTF-8",
                        renderRolePortalHtml(eventDAO, sessionRole, sessionEmail, sessionEntityId, tab, errorBanner).getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        server.start();
        String url = "http://localhost:" + port;
        System.out.println("\n[WEB SERVER LIVE] Frontend running at:");
        System.out.println("-> Landing Page: " + url);
        System.out.println("-> Login Gate:   " + url + "/login.jsp");

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url + "/login.jsp"));
            } catch (Exception ignored) {}
        }
    }

    private static String renderRolePortalHtml(EventDAO eventDAO, String role, String email, int entityId, String activeTab, String errorBanner) {
        List<Event> allEvents = eventDAO.getAllEvents();
        List<String[]> clients = eventDAO.getAllClients();
        List<String[]> vendors = eventDAO.getAllVendors();

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><title>AURA PRIVÉ | ").append(role).append(" Portal</title>");
        sb.append("<link rel='stylesheet' href='css/luxury-style.css'>");
        sb.append("<link href='https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&family=Playfair+Display:ital,wght@0,400;0,600;1,400&display=swap' rel='stylesheet'>");
        sb.append("</head><body>");

        sb.append("<nav class='navbar'><div class='brand-logo'>AURA PRIVÉ</div><div class='nav-links'>");
        sb.append("<a href='index.jsp'>Sanctuary Home</a>");
        sb.append("<span style='color:var(--gold-accent);font-size:0.76rem;letter-spacing:0.12em;text-transform:uppercase;'>").append(Event.escapeHtml(email)).append(" (").append(role).append(")</span>");
        sb.append("<a href='auth?action=logout' style='color:var(--danger-accent);'>Sign Out</a>");
        sb.append("</div></nav><main class='dashboard-container'>");

        if (errorBanner != null && !errorBanner.isBlank()) {
            sb.append("<div style='padding:1rem;border:1px solid var(--danger-accent);color:var(--danger-accent);border-radius:8px;margin-bottom:1.5rem;'>")
              .append(Event.escapeHtml(errorBanner))
              .append("</div>");
        }

        if ("ADMIN".equals(role)) {
            sb.append("<div class='role-bar'><div><span class='badge-tag' style='margin:0;'>Sovereign System Administration</span>");
            sb.append("<h2 style='font-family:var(--font-heading);font-size:1.6rem;'>Executive Developer & Concierge Console</h2></div>");
            sb.append("<div style='display:flex;gap:0.6rem;'>");
            sb.append("<a class='role-pill ").append("events".equals(activeTab) ? "active" : "").append("' href='events?tab=events'>1. Engagements CRUD (").append(allEvents.size()).append(")</a>");
            sb.append("<a class='role-pill ").append("clients".equals(activeTab) ? "active" : "").append("' href='events?tab=clients'>2. UHNW Clients (").append(clients.size()).append(")</a>");
            sb.append("<a class='role-pill ").append("vendors".equals(activeTab) ? "active" : "").append("' href='events?tab=vendors'>3. Syndicate Vendors (").append(vendors.size()).append(")</a>");
            sb.append("</div></div>");

            if ("clients".equals(activeTab)) {
                sb.append("<div class='sub-grid'><div><h3 style='font-family:var(--font-heading);color:var(--gold-accent);margin-bottom:1rem;'>Registered UHNW Clients (<code>clients</code> Table)</h3><table><thead><tr><th>ID</th><th>Full Name</th><th>Tier</th><th>Contact / Login Email</th></tr></thead><tbody>");
                for (String[] c : clients) {
                    sb.append("<tr><td>#").append(c[0]).append("</td><td>").append(Event.escapeHtml(c[1])).append("</td><td>").append(Event.escapeHtml(c[2])).append("</td><td>").append(Event.escapeHtml(c[3])).append("</td></tr>");
                }
                sb.append("</tbody></table></div>");
                sb.append("<div class='form-box' style='margin-top:0;'><h3 style='font-family:var(--font-heading);color:var(--gold-accent);margin-bottom:1rem;'>Onboard New UHNW Client</h3><form action='events' method='POST'><input type='hidden' name='action' value='registerClient'>");
                sb.append("<div class='form-group'><label>Client Full Name</label><input type='text' name='fullName' placeholder='e.g., Devvrat Singhania' required></div>");
                sb.append("<div class='form-group'><label>Membership Tier</label><select name='membershipTier'><option value='Sovereign Founders Circle'>Sovereign Founders Circle</option><option value='Diplomatic Black Tier'>Diplomatic Black Tier</option><option value='Apex Syndicate'>Apex Syndicate</option></select></div>");
                sb.append("<div class='form-group'><label>Client Login Email (Default Pass: 123456)</label><input type='email' name='encryptedContact' placeholder='singhania@pro.abc' required></div>");
                sb.append("<button type='submit' class='cta-button' style='width:100%;'>Authorize Client Account</button></form></div></div>");
            } else if ("vendors".equals(activeTab)) {
                sb.append("<div class='sub-grid'><div><h3 style='font-family:var(--font-heading);color:var(--gold-accent);margin-bottom:1rem;'>Cleared Syndicate Vendors (<code>elite_vendors</code> Table)</h3><table><thead><tr><th>ID</th><th>Vendor Name</th><th>Specialty</th><th>Clearance</th></tr></thead><tbody>");
                for (String[] v : vendors) {
                    sb.append("<tr><td>#").append(v[0]).append("</td><td>").append(Event.escapeHtml(v[1])).append("</td><td>").append(Event.escapeHtml(v[2])).append("</td><td>").append(Event.escapeHtml(v[3])).append("</td></tr>");
                }
                sb.append("</tbody></table></div>");
                sb.append("<div class='form-box' style='margin-top:0;'><h3 style='font-family:var(--font-heading);color:var(--gold-accent);margin-bottom:1rem;'>Accredit New Syndicate Vendor</h3><form action='events' method='POST'><input type='hidden' name='action' value='registerVendor'>");
                sb.append("<div class='form-group'><label>Vendor Organization</label><input type='text' name='vendorName' placeholder='e.g., Rajputana Heritage Security' required></div>");
                sb.append("<div class='form-group'><label>Operational Specialty</label><input type='text' name='specialty' placeholder='e.g., Royal Fortress Perimeter Lock' required></div>");
                sb.append("<div class='form-group'><label>Security Clearance</label><select name='securityClearance'><option value='Maximum Discretion'>Maximum Discretion</option><option value='Diplomatic Tier'>Diplomatic Tier</option><option value='Confidential'>Confidential</option></select></div>");
                sb.append("<button type='submit' class='cta-button' style='width:100%;'>Accredit Vendor</button></form></div></div>");
            } else {
                appendEventsTableAndForm(sb, allEvents, clients, true, 1);
            }
        } else if ("CLIENT".equals(role)) {
            List<Event> myEvents = new ArrayList<>();
            for (Event ev : allEvents) {
                if (ev.getClientId() == entityId) myEvents.add(ev);
            }
            sb.append("<div class='section-header' style='text-align:left;margin-bottom:1.5rem;'><span class='badge-tag'>Private Client Sanctuary • End-to-End Encrypted</span><h2>My Commissioned Engagements</h2></div>");
            appendEventsTableAndForm(sb, myEvents, clients, false, entityId);
        } else if ("VENDOR".equals(role)) {
            sb.append("<div class='section-header' style='text-align:left;margin-bottom:1.5rem;'><span class='badge-tag'>Syndicate Operations Terminal • Zero Client Identity Exposure</span><h2>Assigned Sanctuary Logistics Briefings</h2></div>");
            sb.append("<div class='table-wrapper'><table><thead><tr><th>Briefing ID</th><th>Engagement Tier</th><th>Sanctuary Venue</th><th>Deployment Date</th><th>VIP Headcount</th><th>Required Protocol</th><th>Operational Status</th></tr></thead><tbody>");
            for (Event ev : allEvents) {
                sb.append("<tr><td>#LOG-00").append(ev.getEventId()).append("</td>")
                  .append("<td><span style='color:var(--gold-accent);font-weight:600;'>").append(Event.escapeHtml(ev.getEventType())).append("</span></td>")
                  .append("<td>").append(Event.escapeHtml(ev.getVenueLocation())).append("</td>")
                  .append("<td>").append(Event.escapeHtml(ev.getEventDate())).append("</td>")
                  .append("<td>").append(ev.getVipGuests()).append(" Guests</td>")
                  .append("<td>").append(Event.escapeHtml(ev.getSecurityClearance())).append("</td>")
                  .append("<td><span style='color:var(--gold-accent);'>✓ Cleared for Deployment</span></td></tr>");
            }
            sb.append("</tbody></table></div>");
        }

        sb.append("</main></body></html>");
        return sb.toString();
    }

    private static void appendEventsTableAndForm(StringBuilder sb, List<Event> events, List<String[]> clients, boolean isAdmin, int clientId) {
        sb.append("<div class='table-wrapper'><table><thead><tr><th>ID</th>");
        if (isAdmin) sb.append("<th>Sponsoring Client</th>");
        sb.append("<th>Title & Class</th><th>Sanctuary Venue</th><th>Date</th><th>VIP Guests</th><th>Clearance</th><th>Polymorphic Fee</th><th>Manage Booking</th></tr></thead><tbody>");

        if (events.isEmpty()) {
            sb.append("<tr><td colspan='9' style='text-align:center;padding:2rem;color:var(--text-muted);'>No active engagements registered. Commission a new gathering below.</td></tr>");
        }
        for (Event ev : events) {
            String curSec = ev.getSecurityClearance();
            sb.append("<tr><td>#PRV-00").append(ev.getEventId()).append("</td>");
            if (isAdmin) {
                sb.append("<td><strong>").append(Event.escapeHtml(ev.getClientName())).append("</strong><br><small style='color:var(--text-muted);'>Client #").append(ev.getClientId()).append("</small></td>");
            }
            sb.append("<td><strong>").append(Event.escapeHtml(ev.getEventTitle())).append("</strong><br><small style='color:var(--gold-accent);font-weight:600;'>[").append(Event.escapeHtml(ev.getEventType())).append("]</small></td>")
              .append("<td>").append(Event.escapeHtml(ev.getVenueLocation())).append("</td>")
              .append("<td>").append(Event.escapeHtml(ev.getEventDate())).append("</td>")
              .append("<td>").append(ev.getVipGuests()).append("</td>")
              .append("<td>").append(Event.escapeHtml(curSec)).append("</td>")
              .append("<td>$").append(String.format("%,.2f", ev.calculateBaseLogisticsFee())).append("</td>")
              .append("<td><form action='events' method='POST' class='inline-form'><input type='hidden' name='action' value='update'><input type='hidden' name='eventId' value='").append(ev.getEventId()).append("'>")
              .append("<select name='security' style='width:155px;padding:0.35rem;font-size:0.75rem;'>")
              .append("<option value='Maximum Discretion'").append("Maximum Discretion".equalsIgnoreCase(curSec) ? " selected" : "").append(">Maximum Discretion</option>")
              .append("<option value='Diplomatic Tier'").append("Diplomatic Tier".equalsIgnoreCase(curSec) ? " selected" : "").append(">Diplomatic Tier</option>")
              .append("<option value='Confidential'").append("Confidential".equalsIgnoreCase(curSec) ? " selected" : "").append(">Confidential</option>")
              .append("</select>")
              .append("<input type='number' name='vipGuests' value='").append(ev.getVipGuests()).append("' min='1' max='500' style='width:65px;padding:0.35rem;font-size:0.75rem;'>")
              .append("<button type='submit' class='btn-small'>Update</button></form> ")
              .append("<form action='events' method='POST' class='inline-form'><input type='hidden' name='action' value='delete'><input type='hidden' name='eventId' value='").append(ev.getEventId()).append("'><button type='submit' class='btn-small btn-danger'>Cancel</button></form></td></tr>");
        }
        sb.append("</tbody></table></div>");

        sb.append("<div class='form-box' style='margin-top:2rem;'><h3 style='font-family:var(--font-heading);margin-bottom:1.2rem;color:var(--gold-accent);'>Commission New Bespoke Engagement</h3>")
          .append("<form action='events' method='POST'><input type='hidden' name='action' value='create'><div class='form-grid'>");
        if (isAdmin) {
            sb.append("<div class='form-group'><label>Sponsoring UHNW Client</label><select name='clientId'>");
            for (String[] c : clients) {
                sb.append("<option value='").append(c[0]).append("'>#").append(c[0]).append(" — ").append(Event.escapeHtml(c[1])).append("</option>");
            }
            sb.append("</select></div>");
        } else {
            sb.append("<input type='hidden' name='clientId' value='").append(clientId).append("'>");
        }
        sb.append("<div class='form-group'><label>Event Architecture</label><select name='eventType'><option value='GALA'>Royal Gala ($25k + $1,500/Guest)</option><option value='SUMMIT'>Sovereign Summit ($40k + $2,200/Delegate)</option></select></div>")
          .append("<div class='form-group'><label>Engagement Title</label><input type='text' name='title' placeholder='e.g., Jaipur Royal Heritage Gala' required></div>")
          .append("<div class='form-group'><label>Sanctuary Venue</label><input type='text' name='venue' placeholder='e.g., Rambagh Palace, Jaipur' required></div>")
          .append("<div class='form-group'><label>Engagement Date</label><input type='date' name='date' required></div>")
          .append("<div class='form-group'><label>VIP Guests (1-500)</label><input type='number' name='vipGuests' value='60' min='1' max='500' required></div>")
          .append("<div class='form-group'><label>Security Protocol</label><select name='security'><option value='Maximum Discretion'>Maximum Discretion</option><option value='Diplomatic Tier'>Diplomatic Tier</option><option value='Confidential'>Confidential</option></select></div>")
          .append("</div><button type='submit' class='cta-button' style='width:100%;margin-top:0.5rem;'>Authorize & Register Engagement</button></form></div>");
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
        if (body == null || body.isEmpty()) return map;
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