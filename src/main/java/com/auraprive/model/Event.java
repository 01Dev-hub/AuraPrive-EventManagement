package com.auraprive.model;

import com.auraprive.exception.ExclusiveAccessException;

/**
 * Abstract base entity demonstrating OOP Inheritance, Encapsulation, and Polymorphism.
 */
public abstract class Event implements SecuredEntity {
    private int eventId;
    private int clientId;
    private String eventTitle;
    private String venueLocation;
    private String eventDate;
    private String securityLevel;
    private int vipGuests;

    public Event(int eventId, int clientId, String eventTitle, String venueLocation,
                 String eventDate, String securityLevel, int vipGuests) {
        this.eventId = eventId;
        this.clientId = clientId;
        this.eventTitle = eventTitle;
        this.venueLocation = venueLocation;
        this.eventDate = eventDate;
        this.securityLevel = securityLevel;
        this.vipGuests = vipGuests;
    }

    public Event(int clientId, String eventTitle, String venueLocation,
                 String eventDate, String securityLevel, int vipGuests) {
        this(0, clientId, eventTitle, venueLocation, eventDate, securityLevel, vipGuests);
    }

    // Polymorphic methods overridden by specialized subclasses (GalaEvent, SummitEvent)
    public abstract double calculateBaseLogisticsFee();
    public abstract String getEventType();

    /**
     * Validates domain constraints and throws ExclusiveAccessException on policy breaches.
     */
    public void validatePayload() throws ExclusiveAccessException {
        if (eventTitle == null || eventTitle.trim().length() < 3) {
            throw new ExclusiveAccessException("Engagement title must contain at least 3 characters.");
        }
        if (venueLocation == null || venueLocation.trim().isEmpty()) {
            throw new ExclusiveAccessException("Sanctuary venue location cannot be empty.");
        }
        if (vipGuests < 1 || vipGuests > 500) {
            throw new ExclusiveAccessException("VIP guest allocation (" + vipGuests + ") violates sanctuary capacity limits (1-500).");
        }
        if (!verifyAccess()) {
            throw new ExclusiveAccessException("Unrecognized or missing security clearance protocol.");
        }
    }

    @Override
    public String getSecurityClearance() {
        return this.securityLevel;
    }

    @Override
    public boolean verifyAccess() {
        return this.securityLevel != null && !this.securityLevel.trim().isEmpty();
    }

    /**
     * Sanitizes strings before rendering in JSP views to prevent Cross-Site Scripting (XSS).
     */
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
    public String getEventTitle() { return eventTitle; }
    public String getVenueLocation() { return venueLocation; }
    public String getEventDate() { return eventDate; }
    public String getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(String securityLevel) { this.securityLevel = securityLevel; }
    public int getVipGuests() { return vipGuests; }
    public void setVipGuests(int vipGuests) { this.vipGuests = vipGuests; }
}