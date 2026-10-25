package com.auraprive.model;

import com.auraprive.exception.ExclusiveAccessException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * Abstract base entity demonstrating OOP Inheritance, Encapsulation, and Polymorphism.
 */
public abstract class Event implements SecuredEntity {
    private static final Set<String> VALID_CLEARANCES = Set.of(
        "Maximum Discretion",
        "Diplomatic Tier",
        "Confidential"
    );

    private int eventId;
    private int clientId;
    private String clientName;
    private String eventTitle;
    private String venueLocation;
    private String eventDate;
    private String securityLevel;
    private int vipGuests;

    public Event(int eventId, int clientId, String clientName, String eventTitle, String venueLocation,
                 String eventDate, String securityLevel, int vipGuests) {
        this.eventId = eventId;
        this.clientId = clientId;
        this.clientName = (clientName == null || clientName.isBlank()) ? ("Client #" + clientId) : clientName;
        this.eventTitle = eventTitle;
        this.venueLocation = venueLocation;
        this.eventDate = eventDate;
        this.securityLevel = securityLevel;
        this.vipGuests = vipGuests;
    }

    public Event(int eventId, int clientId, String eventTitle, String venueLocation,
                 String eventDate, String securityLevel, int vipGuests) {
        this(eventId, clientId, "Client #" + clientId, eventTitle, venueLocation, eventDate, securityLevel, vipGuests);
    }

    public Event(int clientId, String eventTitle, String venueLocation,
                 String eventDate, String securityLevel, int vipGuests) {
        this(0, clientId, "Client #" + clientId, eventTitle, venueLocation, eventDate, securityLevel, vipGuests);
    }

    public abstract double calculateBaseLogisticsFee();
    public abstract String getEventType();

    /**
     * Centralized server-side validation for title, venue, future/present date, clearance allow-list, and guest count.
     */
    public void validatePayload() throws ExclusiveAccessException {
        if (clientId < 1) {
            throw new ExclusiveAccessException("Invalid sponsoring client ID.");
        }
        if (eventTitle == null || eventTitle.trim().length() < 3) {
            throw new ExclusiveAccessException("Engagement title must contain at least 3 characters.");
        }
        if (venueLocation == null || venueLocation.trim().isEmpty()) {
            throw new ExclusiveAccessException("Sanctuary venue location cannot be empty.");
        }
        if (eventDate == null || eventDate.trim().isEmpty()) {
            throw new ExclusiveAccessException("Engagement date is required.");
        }
        try {
            LocalDate parsedDate = LocalDate.parse(eventDate.trim());
            if (parsedDate.isBefore(LocalDate.now())) {
                throw new ExclusiveAccessException("Engagement date (" + eventDate + ") cannot be in the past.");
            }
        } catch (DateTimeParseException e) {
            throw new ExclusiveAccessException("Invalid date format (" + eventDate + "). Expected YYYY-MM-DD.");
        }
        if (vipGuests < 1 || vipGuests > 500) {
            throw new ExclusiveAccessException("VIP guest allocation (" + vipGuests + ") violates sanctuary capacity limits (1-500).");
        }
        if (!verifyAccess()) {
            throw new ExclusiveAccessException("Unauthorized security clearance '" + securityLevel
                + "'. Allowed: Maximum Discretion, Diplomatic Tier, Confidential.");
        }
    }

    @Override
    public String getSecurityClearance() {
        return this.securityLevel;
    }

    @Override
    public boolean verifyAccess() {
        return this.securityLevel != null && VALID_CLEARANCES.contains(this.securityLevel.trim());
    }

    public static boolean isValidClearance(String clearance) {
        return clearance != null && VALID_CLEARANCES.contains(clearance.trim());
    }

    public static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public int getClientId() { return clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getEventTitle() { return eventTitle; }
    public String getVenueLocation() { return venueLocation; }
    public String getEventDate() { return eventDate; }
    public String getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }
    public int getVipGuests() { return vipGuests; }
    public void setVipGuests(int vipGuests) { this.vipGuests = vipGuests; }
}