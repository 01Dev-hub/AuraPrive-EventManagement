package com.auraprive.dao;

import com.auraprive.model.Event;
import com.auraprive.model.GalaEvent;
import com.auraprive.model.SummitEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Data Access Object (DAO) implementing full CRUD operations via parameterized PreparedStatements,
 * plus relational queries across clients and elite_vendors tables.
 */
public class EventDAO {

    private static boolean schemaVerified = false;
    private static final List<Event> fallbackEvents = new ArrayList<>();
    private static final List<String[]> fallbackClients = new ArrayList<>();
    private static final List<String[]> fallbackVendors = new ArrayList<>();

    static {
        fallbackEvents.add(new GalaEvent(1, 1, "Udaipur Royal Solstice Gala", "Taj Lake Palace, Udaipur", "2026-12-31", "Maximum Discretion", 60));
        fallbackEvents.add(new SummitEvent(2, 2, "Jodhpur Sovereign Founders Summit", "Umaid Bhawan Palace, Jodhpur", "2026-11-18", "Diplomatic Tier", 40));

        fallbackClients.add(new String[]{"1", "Maharaja Vikramaditya Rathore", "Sovereign Founders Circle", "rathore@vault.auraprive.in"});
        fallbackClients.add(new String[]{"2", "Arundhati Devi Scindia", "Diplomatic Black Tier", "scindia@vault.auraprive.in"});
        fallbackClients.add(new String[]{"3", "Rajeshwar Singhania", "Apex Syndicate", "singhania@vault.auraprive.in"});

        fallbackVendors.add(new String[]{"1", "Garuda Sovereign Security", "Counter-Surveillance & Airspace Lock", "Maximum Discretion"});
        fallbackVendors.add(new String[]{"2", "Dawat-e-Khas Royal Gastronomy", "Bespoke Heritage Culinary Curation", "Diplomatic Tier"});
        fallbackVendors.add(new String[]{"3", "Vayu Executive Aviation", "Unlisted Charter & Helipad Logistics", "Maximum Discretion"});
    }

    public EventDAO() {
        ensureSchemaUpdated();
    }

    /**
     * Automatically ensures tables, columns, and Indian seed records exist in local MariaDB/MySQL.
     */
    private synchronized void ensureSchemaUpdated() {
        if (schemaVerified) return;
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            try {
                stmt.executeUpdate("ALTER TABLE exclusive_events ADD COLUMN event_type VARCHAR(30) NOT NULL DEFAULT 'GALA'");
            } catch (SQLException ignored) {}
            try {
                stmt.executeUpdate("ALTER TABLE exclusive_events ADD COLUMN vip_guests INT NOT NULL DEFAULT 50");
            } catch (SQLException ignored) {}

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS clients (client_id INT AUTO_INCREMENT PRIMARY KEY, full_name VARCHAR(120) NOT NULL, membership_tier VARCHAR(60) NOT NULL, encrypted_contact VARCHAR(150) NOT NULL)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS elite_vendors (vendor_id INT AUTO_INCREMENT PRIMARY KEY, vendor_name VARCHAR(120) NOT NULL, specialty VARCHAR(100) NOT NULL, security_clearance VARCHAR(60) NOT NULL)");

            stmt.executeUpdate("INSERT IGNORE INTO clients (client_id, full_name, membership_tier, encrypted_contact) VALUES (1, 'Maharaja Vikramaditya Rathore', 'Sovereign Founders Circle', 'rathore@vault.auraprive.in'), (2, 'Arundhati Devi Scindia', 'Diplomatic Black Tier', 'scindia@vault.auraprive.in'), (3, 'Rajeshwar Singhania', 'Apex Syndicate', 'singhania@vault.auraprive.in')");
            stmt.executeUpdate("INSERT IGNORE INTO elite_vendors (vendor_id, vendor_name, specialty, security_clearance) VALUES (1, 'Garuda Sovereign Security', 'Counter-Surveillance & Airspace Lock', 'Maximum Discretion'), (2, 'Dawat-e-Khas Royal Gastronomy', 'Bespoke Heritage Culinary Curation', 'Diplomatic Tier'), (3, 'Vayu Executive Aviation', 'Unlisted Charter & Helipad Logistics', 'Maximum Discretion')");

            // Automatically replace any earlier foreign names in local MariaDB with Indian names
            stmt.executeUpdate("UPDATE clients SET full_name='Maharaja Vikramaditya Rathore', encrypted_contact='rathore@vault.auraprive.in' WHERE client_id=1 AND full_name LIKE '%Julian%'");
            stmt.executeUpdate("UPDATE clients SET full_name='Arundhati Devi Scindia', encrypted_contact='scindia@vault.auraprive.in' WHERE client_id=2 AND full_name LIKE '%Elena%'");
            stmt.executeUpdate("UPDATE clients SET full_name='Rajeshwar Singhania', encrypted_contact='singhania@vault.auraprive.in' WHERE client_id=3 AND full_name LIKE '%Kenji%'");

            stmt.executeUpdate("UPDATE elite_vendors SET vendor_name='Garuda Sovereign Security', specialty='Counter-Surveillance & Airspace Lock' WHERE vendor_id=1 AND vendor_name LIKE '%Aegis%'");
            stmt.executeUpdate("UPDATE elite_vendors SET vendor_name='Dawat-e-Khas Royal Gastronomy', specialty='Bespoke Heritage Culinary Curation' WHERE vendor_id=2 AND vendor_name LIKE '%Maison%'");
            stmt.executeUpdate("UPDATE elite_vendors SET vendor_name='Vayu Executive Aviation', specialty='Unlisted Charter & Helipad Logistics' WHERE vendor_id=3 AND vendor_name LIKE '%Helios%'");

            stmt.executeUpdate("UPDATE exclusive_events SET event_title='Udaipur Royal Solstice Gala', venue_location='Taj Lake Palace, Udaipur' WHERE event_id=1 AND (venue_location LIKE '%Como%' OR venue_location='Lake')");
            stmt.executeUpdate("UPDATE exclusive_events SET event_title='Jodhpur Sovereign Founders Summit', venue_location='Umaid Bhawan Palace, Jodhpur' WHERE event_id=2 AND venue_location LIKE '%Badrutt%'");

            schemaVerified = true;
        } catch (SQLException e) {
            System.err.println("[EventDAO Schema Check] " + e.getMessage());
        }
    }

    // CREATE EVENT
    public boolean insertEvent(Event event) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            int nextId = fallbackEvents.size() + 1;
            event.setEventId(nextId);
            fallbackEvents.add(0, event);
            return true;
        }

        String sql = "INSERT INTO exclusive_events (client_id, event_title, event_type, venue_location, event_date, security_level, vip_guests) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, event.getClientId());
            stmt.setString(2, event.getEventTitle());
            stmt.setString(3, event.getEventType());
            stmt.setString(4, event.getVenueLocation());
            stmt.setString(5, event.getEventDate());
            stmt.setString(6, event.getSecurityLevel());
            stmt.setInt(7, event.getVipGuests());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to insert event: " + e.getMessage());
            return false;
        }
    }

    // READ EVENTS
    public List<Event> getAllEvents() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            return new ArrayList<>(fallbackEvents);
        }

        List<Event> events = new ArrayList<>();
        String sql = "SELECT event_id, client_id, event_title, event_type, venue_location, event_date, security_level, vip_guests FROM exclusive_events ORDER BY event_id DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("event_id");
                int clientId = rs.getInt("client_id");
                String title = rs.getString("event_title");
                String type = rs.getString("event_type");
                String venue = rs.getString("venue_location");
                String date = rs.getString("event_date");
                String security = rs.getString("security_level");
                int guests = rs.getInt("vip_guests");

                if ("SUMMIT".equalsIgnoreCase(type)) {
                    events.add(new SummitEvent(id, clientId, title, venue, date, security, guests));
                } else {
                    events.add(new GalaEvent(id, clientId, title, venue, date, security, guests));
                }
            }
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to fetch events: " + e.getMessage());
            return new ArrayList<>(fallbackEvents);
        }
        return events;
    }

    // UPDATE EVENT
    public boolean updateEvent(int eventId, String newSecurityLevel, int newVipGuests) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            for (Event ev : fallbackEvents) {
                if (ev.getEventId() == eventId) {
                    ev.setSecurityLevel(newSecurityLevel);
                    ev.setVipGuests(newVipGuests);
                    return true;
                }
            }
            return false;
        }

        String sql = "UPDATE exclusive_events SET security_level = ?, vip_guests = ? WHERE event_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newSecurityLevel);
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
            Iterator<Event> it = fallbackEvents.iterator();
            while (it.hasNext()) {
                if (it.next().getEventId() == eventId) {
                    it.remove();
                    return true;
                }
            }
            return false;
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
    public boolean insertClient(String fullName, String membershipTier, String encryptedContact) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            String nextId = String.valueOf(fallbackClients.size() + 1);
            fallbackClients.add(new String[]{nextId, fullName, membershipTier, encryptedContact});
            return true;
        }
        String sql = "INSERT INTO clients (full_name, membership_tier, encrypted_contact) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fullName);
            stmt.setString(2, membershipTier);
            stmt.setString(3, encryptedContact);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to register client: " + e.getMessage());
            return false;
        }
    }

    // CREATE NEW VENDOR
    public boolean insertVendor(String vendorName, String specialty, String clearance) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            String nextId = String.valueOf(fallbackVendors.size() + 1);
            fallbackVendors.add(new String[]{nextId, vendorName, specialty, clearance});
            return true;
        }
        String sql = "INSERT INTO elite_vendors (vendor_name, specialty, security_clearance) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vendorName);
            stmt.setString(2, specialty);
            stmt.setString(3, clearance);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to register vendor: " + e.getMessage());
            return false;
        }
    }

    // READ CLIENTS TABLE
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

    // READ ELITE VENDORS TABLE
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