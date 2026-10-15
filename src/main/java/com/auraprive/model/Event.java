package com.auraprive.model;

/**
 * Abstract entity demonstrating OOP Inheritance, Encapsulation, and Polymorphism.
 */
public abstract class Event implements SecuredEntity {
    private int eventId;
    private int clientId;
    private String eventTitle;
    private String venueLocation;
    private String eventDate;
    private String securityLevel;

    public Event(int eventId, int clientId, String eventTitle, String venueLocation, String eventDate, String securityLevel) {
        this.eventId = eventId;
        this.clientId = clientId;
        this.eventTitle = eventTitle;
        this.venueLocation = venueLocation;
        this.eventDate = eventDate;
        this.securityLevel = securityLevel;
    }

    public Event(int clientId, String eventTitle, String venueLocation, String eventDate, String securityLevel) {
        this(0, clientId, eventTitle, venueLocation, eventDate, securityLevel);
    }

    // Polymorphic method overridden by specialized event classes
    public abstract double calculateBaseLogisticsFee();

    @Override
    public String getSecurityClearance() {
        return this.securityLevel;
    }

    @Override
    public boolean verifyAccess() {
        return this.securityLevel != null && !this.securityLevel.trim().isEmpty();
    }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
    public int getClientId() { return clientId; }
    public String getEventTitle() { return eventTitle; }
    public String getVenueLocation() { return venueLocation; }
    public String getEventDate() { return eventDate; }
    public String getSecurityLevel() { return securityLevel; }
}