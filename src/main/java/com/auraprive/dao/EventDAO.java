package com.auraprive.dao;

import com.auraprive.model.Event;
import com.auraprive.model.GalaEvent;
import com.auraprive.model.SummitEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe Data Access Object implementing full CRUD via PreparedStatements,
 * relational JOIN queries across clients/exclusive_events, and concurrent fallback collections.
 */
public class EventDAO {

    private static boolean schemaVerified = false;

    private static final List<Event> fallbackEvents = new CopyOnWriteArrayList<>();
    private static final List<String[]> fallbackClients = new CopyOnWriteArrayList<>();
    private static final List<String[]> fallbackVendors = new CopyOnWriteArrayList<>();

    private static final AtomicInteger nextEventId = new AtomicInteger(3);
    private static final AtomicInteger nextClientId = new AtomicInteger(4);
    private static final AtomicInteger nextVendorId = new AtomicInteger(4);

    // Shared thread-safe credential store: email -> [sha256Password, ROLE, entityId]
    private static final Map<String, String[]> ACCOUNTS = new ConcurrentHashMap<>();

    // Thread-safe tracking of Vendor Logistics Status and Exclusive Vendor Ownership per event_id
    private static final Map<Integer, String> VENDOR_STATUS_MAP = new ConcurrentHashMap<>();
    private static final Map<Integer, String> VENDOR_ASSIGNEE_MAP = new ConcurrentHashMap<>();
    
    // One static lock shared by every EventDAO instance (each servlet creates its own)
    private static final Object BOOKING_LOCK = new Object();

    static {
        fallbackClients.add(new String[]{"1", "Maharaja Vikramaditya Rathore", "Sovereign Founders Circle", "rathore@vault.auraprive.in"});
        fallbackClients.add(new String[]{"2", "Arundhati Devi Scindia", "Diplomatic Black Tier", "scindia@vault.auraprive.in"});
        fallbackClients.add(new String[]{"3", "Rajeshwar Singhania", "Apex Syndicate", "client@pro.abc"});

        fallbackVendors.add(new String[]{"1", "Garuda Sovereign Security", "Counter-Surveillance & Airspace Lock", "Maximum Discretion"});
        fallbackVendors.add(new String[]{"2", "Dawat-e-Khas Royal Gastronomy", "Bespoke Heritage Culinary Curation", "Diplomatic Tier"});
        fallbackVendors.add(new String[]{"3", "Vayu Executive Aviation", "Unlisted Charter & Helipad Logistics", "Maximum Discretion"});

        fallbackEvents.add(new GalaEvent(1, 1, "Maharaja Vikramaditya Rathore", "Udaipur Royal Solstice Gala", "Taj Lake Palace, Udaipur", "2026-12-31", "Maximum Discretion", 60));
        fallbackEvents.add(new SummitEvent(2, 2, "Arundhati Devi Scindia", "Jodhpur Sovereign Founders Summit", "Umaid Bhawan Palace, Jodhpur", "2026-11-18", "Diplomatic Tier", 40));

        // Both initial events start unclaimed so the first vendor can accept or decline them
        VENDOR_STATUS_MAP.put(1, "Pending Vendor Acceptance");
        VENDOR_STATUS_MAP.put(2, "Pending Vendor Acceptance");

        String defaultHash = sha256("123456");
        ACCOUNTS.put("admin@pro.abc", new String[]{defaultHash, "ADMIN", "0"});
        // Mapped to Client #3 so client@pro.abc only sees events created by client@pro.abc
        ACCOUNTS.put("client@pro.abc", new String[]{defaultHash, "CLIENT", "3"});
        ACCOUNTS.put("ven@pro.abc", new String[]{defaultHash, "VENDOR", "1"});
    }

    public EventDAO() {
        ensureSchemaUpdated();
    }

    public static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String[] authenticateAccount(String email, String rawPassword) {
        if (email == null || rawPassword == null) return null;
        String[] acc = ACCOUNTS.get(email.trim().toLowerCase());
        if (acc != null && acc[0].equals(sha256(rawPassword))) {
            return acc;
        }
        return null;
    }

    public static boolean accountExists(String email) {
        return email != null && ACCOUNTS.containsKey(email.trim().toLowerCase());
    }

    public static void registerAccountCredentials(String email, String rawPassword, String role, int entityId) {
        String safeRole = "VENDOR".equalsIgnoreCase(role) ? "VENDOR" : "CLIENT";
        ACCOUNTS.put(email.trim().toLowerCase(), new String[]{sha256(rawPassword), safeRole, String.valueOf(entityId)});
    }

    public static boolean updatePassword(String email, String oldPassword, String newPassword) {
        if (email == null || oldPassword == null || newPassword == null || newPassword.length() < 6) {
            return false;
        }
        String key = email.trim().toLowerCase();
        String[] acc = ACCOUNTS.get(key);
        if (acc != null && acc[0].equals(sha256(oldPassword))) {
            acc[0] = sha256(newPassword);
            return true;
        }
        return false;
    }

    private synchronized void ensureSchemaUpdated() {
        if (schemaVerified) return;
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS clients (client_id INT AUTO_INCREMENT PRIMARY KEY, full_name VARCHAR(120) NOT NULL, membership_tier VARCHAR(60) NOT NULL, encrypted_contact VARCHAR(150) NOT NULL)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS elite_vendors (vendor_id INT AUTO_INCREMENT PRIMARY KEY, vendor_name VARCHAR(120) NOT NULL, specialty VARCHAR(100) NOT NULL, security_clearance VARCHAR(60) NOT NULL)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS exclusive_events (event_id INT AUTO_INCREMENT PRIMARY KEY, client_id INT NOT NULL, event_title VARCHAR(150) NOT NULL, event_type VARCHAR(30) NOT NULL DEFAULT 'GALA', venue_location VARCHAR(150) NOT NULL, event_date DATE NOT NULL, security_level VARCHAR(60) NOT NULL, vip_guests INT NOT NULL DEFAULT 50, FOREIGN KEY (client_id) REFERENCES clients(client_id) ON DELETE CASCADE)");

            try {
                stmt.executeUpdate("ALTER TABLE exclusive_events ADD COLUMN event_type VARCHAR(30) NOT NULL DEFAULT 'GALA'");
            } catch (SQLException e) {
                if (e.getErrorCode() != 1060) throw e;
            }
            try {
                stmt.executeUpdate("ALTER TABLE exclusive_events ADD COLUMN vip_guests INT NOT NULL DEFAULT 50");
            } catch (SQLException e) {
                if (e.getErrorCode() != 1060) throw e;
            }

            stmt.executeUpdate("INSERT IGNORE INTO clients (client_id, full_name, membership_tier, encrypted_contact) VALUES (1, 'Maharaja Vikramaditya Rathore', 'Sovereign Founders Circle', 'rathore@vault.auraprive.in'), (2, 'Arundhati Devi Scindia', 'Diplomatic Black Tier', 'scindia@vault.auraprive.in'), (3, 'Rajeshwar Singhania', 'Apex Syndicate', 'singhania@vault.auraprive.in')");
            stmt.executeUpdate("INSERT IGNORE INTO elite_vendors (vendor_id, vendor_name, specialty, security_clearance) VALUES (1, 'Garuda Sovereign Security', 'Counter-Surveillance & Airspace Lock', 'Maximum Discretion'), (2, 'Dawat-e-Khas Royal Gastronomy', 'Bespoke Heritage Culinary Curation', 'Diplomatic Tier'), (3, 'Vayu Executive Aviation', 'Unlisted Charter & Helipad Logistics', 'Maximum Discretion')");
            schemaVerified = true;
        } catch (SQLException e) {
            System.err.println("[EventDAO Schema Check] " + e.getMessage());
        }
    }

    public boolean clientExists(int clientId) {
        for (String[] c : getAllClients()) {
            if (String.valueOf(clientId).equals(c[0])) {
                return true;
            }
        }
        return false;
    }

    public String resolveClientName(int clientId) {
        for (String[] c : getAllClients()) {
            if (String.valueOf(clientId).equals(c[0])) {
                return c[1];
            }
        }
        return "Client #" + clientId;
    }

    public Event getEventById(int eventId) {
        for (Event ev : getAllEvents()) {
            if (ev.getEventId() == eventId) {
                return ev;
            }
        }
        return null;
    }

    /**
     * Enforces Pillar 1 (Sanctuary Buyouts): Ensures a venue is exclusively isolated on a given date.
     */
    public boolean isVenueAvailable(String venueLocation, String eventDate) {
        if (venueLocation == null || eventDate == null) return true;
        for (Event ev : getAllEvents()) {
            if (ev.getVenueLocation().trim().equalsIgnoreCase(venueLocation.trim())
                    && ev.getEventDate().trim().equals(eventDate.trim())) {
                return false;
            }
        }
        return true;
    }
    /** Checks venue availability and inserts as ONE atomic step, so two requests cannot double-book. */
    public boolean insertEventIfVenueFree(Event event) {
        synchronized (BOOKING_LOCK) {
            return isVenueAvailable(event.getVenueLocation(), event.getEventDate()) && insertEvent(event);
        }
    }

    /** Vendor claim is check-then-act, so it runs under the shared lock. */
    public boolean updateVendorStatus(int eventId, String newStatus, String vendorEmail) {
        synchronized (BOOKING_LOCK) {
            return updateVendorStatusLocked(eventId, newStatus, vendorEmail);
        }
    }

    private boolean updateVendorStatusLocked(int eventId, String newStatus, String vendorEmail) {
        if (newStatus == null || newStatus.isBlank() || vendorEmail == null || getEventById(eventId) == null) {
            return false;
        }
        String cleanVendor = vendorEmail.trim().toLowerCase();
        String currentOwner = VENDOR_ASSIGNEE_MAP.getOrDefault(eventId, "");

        // Block another vendor if this event is already accepted/claimed by a different vendor
        if (!currentOwner.isEmpty() && !currentOwner.equalsIgnoreCase(cleanVendor)) {
            return false;
        }

        String trimmedStatus = newStatus.trim();
        // If declined or reset to pending, release ownership so other vendors can accept it
        if ("Declined by Vendor".equalsIgnoreCase(trimmedStatus)
                || "Pending Vendor Acceptance".equalsIgnoreCase(trimmedStatus)) {
            VENDOR_ASSIGNEE_MAP.remove(eventId);
            VENDOR_STATUS_MAP.put(eventId, trimmedStatus);
        } else {
            // Lock ownership of this event to the accepting vendor
            VENDOR_ASSIGNEE_MAP.put(eventId, cleanVendor);
            VENDOR_STATUS_MAP.put(eventId, trimmedStatus);
        }

        for (Event ev : fallbackEvents) {
            if (ev.getEventId() == eventId) {
                ev.setVendorStatus(VENDOR_STATUS_MAP.get(eventId));
                ev.setAssignedVendor(VENDOR_ASSIGNEE_MAP.getOrDefault(eventId, ""));
            }
        }
        return true;
    }

    // CREATE EVENT
    public boolean insertEvent(Event event) {
        if (!clientExists(event.getClientId())) {
            return false;
        }
        event.setClientName(resolveClientName(event.getClientId()));

        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            int id = nextEventId.getAndIncrement();
            event.setEventId(id);
            VENDOR_STATUS_MAP.put(id, "Pending Vendor Acceptance");
            VENDOR_ASSIGNEE_MAP.remove(id);
            fallbackEvents.add(0, event);
            return true;
        }

        String sql = "INSERT INTO exclusive_events (client_id, event_title, event_type, venue_location, event_date, security_level, vip_guests) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, event.getClientId());
            stmt.setString(2, event.getEventTitle());
            stmt.setString(3, event.getEventType());
            stmt.setString(4, event.getVenueLocation());
            stmt.setString(5, event.getEventDate());
            stmt.setString(6, event.getSecurityLevel());
            stmt.setInt(7, event.getVipGuests());
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        event.setEventId(id);
                        VENDOR_STATUS_MAP.put(id, "Pending Vendor Acceptance");
                        VENDOR_ASSIGNEE_MAP.remove(id);
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to insert event: " + e.getMessage());
            return false;
        }
    }

    // READ EVENTS WITH RELATIONAL SQL LEFT JOIN ON CLIENTS TABLE
    public List<Event> getAllEvents() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            for (Event ev : fallbackEvents) {
                ev.setClientName(resolveClientName(ev.getClientId()));
                ev.setVendorStatus(VENDOR_STATUS_MAP.getOrDefault(ev.getEventId(), "Pending Vendor Acceptance"));
                ev.setAssignedVendor(VENDOR_ASSIGNEE_MAP.getOrDefault(ev.getEventId(), ""));
            }
            return new ArrayList<>(fallbackEvents);
        }

        List<Event> events = new ArrayList<>();
        String sql = "SELECT e.event_id, e.client_id, COALESCE(c.full_name, CONCAT('Client #', e.client_id)) AS client_name, "
                   + "e.event_title, e.event_type, e.venue_location, e.event_date, e.security_level, e.vip_guests "
                   + "FROM exclusive_events e "
                   + "LEFT JOIN clients c ON e.client_id = c.client_id "
                   + "ORDER BY e.event_id DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("event_id");
                int clientId = rs.getInt("client_id");
                String clientName = rs.getString("client_name");
                String title = rs.getString("event_title");
                String type = rs.getString("event_type");
                String venue = rs.getString("venue_location");
                String date = rs.getString("event_date");
                String security = rs.getString("security_level");
                int guests = rs.getInt("vip_guests");

                Event ev = "SUMMIT".equalsIgnoreCase(type)
                        ? new SummitEvent(id, clientId, clientName, title, venue, date, security, guests)
                        : new GalaEvent(id, clientId, clientName, title, venue, date, security, guests);
                ev.setVendorStatus(VENDOR_STATUS_MAP.getOrDefault(id, "Pending Vendor Acceptance"));
                ev.setAssignedVendor(VENDOR_ASSIGNEE_MAP.getOrDefault(id, ""));
                events.add(ev);
            }
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to fetch joined events: " + e.getMessage());
            return new ArrayList<>(fallbackEvents);
        }
        return events;
    }

    // UPDATE EVENT
    public boolean updateEvent(int eventId, String newSecurityLevel, int newVipGuests) {
        if (!Event.isValidClearance(newSecurityLevel) || newVipGuests < 1 || newVipGuests > 500) {
            return false;
        }
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            for (Event ev : fallbackEvents) {
                if (ev.getEventId() == eventId) {
                    ev.setSecurityLevel(newSecurityLevel.trim());
                    ev.setVipGuests(newVipGuests);
                    return true;
                }
            }
            return false;
        }

        String sql = "UPDATE exclusive_events SET security_level = ?, vip_guests = ? WHERE event_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newSecurityLevel.trim());
            stmt.setInt(2, newVipGuests);
            stmt.setInt(3, eventId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to update event: " + e.getMessage());
            return false;
        }
    }

    // DELETE EVENT
    public boolean deleteEvent(int eventId) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            return fallbackEvents.removeIf(ev -> ev.getEventId() == eventId);
        }

        String sql = "DELETE FROM exclusive_events WHERE event_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to delete event: " + e.getMessage());
            return false;
        }
    }

    // CREATE NEW CLIENT
    public int insertClient(String fullName, String membershipTier, String encryptedContact) {
        if (fullName == null || fullName.isBlank() || encryptedContact == null || encryptedContact.isBlank()) {
            return -1;
        }
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            String nextId = String.valueOf(nextClientId.getAndIncrement());
            fallbackClients.add(new String[]{nextId, fullName.trim(), membershipTier.trim(), encryptedContact.trim()});
            return Integer.parseInt(nextId);
        }
        String sql = "INSERT INTO clients (full_name, membership_tier, encrypted_contact) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, fullName.trim());
            stmt.setString(2, membershipTier.trim());
            stmt.setString(3, encryptedContact.trim());
            if (stmt.executeUpdate() > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
                return nextClientId.getAndIncrement();
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to register client: " + e.getMessage());
            return -1;
        }
    }

    // CREATE NEW VENDOR
    public int insertVendor(String vendorName, String specialty, String clearance) {
        if (vendorName == null || vendorName.isBlank() || !Event.isValidClearance(clearance)) {
            return -1;
        }
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            String nextId = String.valueOf(nextVendorId.getAndIncrement());
            fallbackVendors.add(new String[]{nextId, vendorName.trim(), specialty.trim(), clearance.trim()});
            return Integer.parseInt(nextId);
        }
        String sql = "INSERT INTO elite_vendors (vendor_name, specialty, security_clearance) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, vendorName.trim());
            stmt.setString(2, specialty.trim());
            stmt.setString(3, clearance.trim());
            if (stmt.executeUpdate() > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
                return nextVendorId.getAndIncrement();
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to register vendor: " + e.getMessage());
            return -1;
        }
    }

    public List<String[]> getAllClients() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            List<String[]> clients = new ArrayList<>();
            String sql = "SELECT client_id, full_name, membership_tier, encrypted_contact FROM clients ORDER BY client_id ASC";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    clients.add(new String[]{
                        String.valueOf(rs.getInt("client_id")),
                        rs.getString("full_name"),
                        rs.getString("membership_tier"),
                        rs.getString("encrypted_contact")
                    });
                }
                if (!clients.isEmpty()) return clients;
            } catch (SQLException e) {
                System.err.println("[EventDAO Error] Failed to fetch clients: " + e.getMessage());
            }
        }
        return new ArrayList<>(fallbackClients);
    }

    public List<String[]> getAllVendors() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            List<String[]> vendors = new ArrayList<>();
            String sql = "SELECT vendor_id, vendor_name, specialty, security_clearance FROM elite_vendors ORDER BY vendor_id ASC";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    vendors.add(new String[]{
                        String.valueOf(rs.getInt("vendor_id")),
                        rs.getString("vendor_name"),
                        rs.getString("specialty"),
                        rs.getString("security_clearance")
                    });
                }
                if (!vendors.isEmpty()) return vendors;
            } catch (SQLException e) {
                System.err.println("[EventDAO Error] Failed to fetch vendors: " + e.getMessage());
            }
        }
        return new ArrayList<>(fallbackVendors);
    }
}