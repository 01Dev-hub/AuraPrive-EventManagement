package com.auraprive.dao;

import com.auraprive.model.Event;
import com.auraprive.model.GalaEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages database operations (CRUD) for exclusive event entities.
 * Automatically falls back to in-memory storage if MariaDB/MySQL is not installed on the host device.
 */
public class EventDAO {

    // In-memory fallback list for devices without MariaDB installed
    private static final List<Event> fallbackEvents = new ArrayList<>();
    static {
        fallbackEvents.add(new GalaEvent(1, 1, "Solstice Sovereign Gala", "Lake Como Private Villa", "2026-12-31", "Maximum Discretion", 50));
    }

    public boolean insertEvent(Event event) {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            int nextId = fallbackEvents.size() + 1;
            event.setEventId(nextId);
            fallbackEvents.add(0, event);
            System.out.println("[EventDAO Fallback] Saved event in memory (MariaDB not detected on this device).");
            return true;
        }

        String sql = "INSERT INTO exclusive_events (client_id, event_title, venue_location, event_date, security_level) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, event.getClientId());
            stmt.setString(2, event.getEventTitle());
            stmt.setString(3, event.getVenueLocation());
            stmt.setString(4, event.getEventDate());
            stmt.setString(5, event.getSecurityLevel());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to insert event: " + e.getMessage());
            return false;
        }
    }

    public List<Event> getAllEvents() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn == null) {
            return new ArrayList<>(fallbackEvents);
        }

        List<Event> events = new ArrayList<>();
        String sql = "SELECT event_id, client_id, event_title, venue_location, event_date, security_level FROM exclusive_events ORDER BY event_id DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                events.add(new GalaEvent(
                    rs.getInt("event_id"),
                    rs.getInt("client_id"),
                    rs.getString("event_title"),
                    rs.getString("venue_location"),
                    rs.getString("event_date"),
                    rs.getString("security_level"),
                    50
                ));
            }
        } catch (SQLException e) {
            System.err.println("[EventDAO Error] Failed to fetch events: " + e.getMessage());
            return new ArrayList<>(fallbackEvents);
        }
        return events;
    }
}