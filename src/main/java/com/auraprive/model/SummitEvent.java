package com.auraprive.model;

/**
 * Second specialized polymorphic subclass for Sovereign & Diplomatic Summits.
 */
public class SummitEvent extends Event {

    public SummitEvent(int eventId, int clientId, String clientName, String eventTitle, String venueLocation,
                       String eventDate, String securityLevel, int expectedDelegates) {
        super(eventId, clientId, clientName, eventTitle, venueLocation, eventDate, securityLevel, expectedDelegates);
    }

    public SummitEvent(int eventId, int clientId, String eventTitle, String venueLocation,
                       String eventDate, String securityLevel, int expectedDelegates) {
        super(eventId, clientId, eventTitle, venueLocation, eventDate, securityLevel, expectedDelegates);
    }

    public SummitEvent(int clientId, String eventTitle, String venueLocation,
                       String eventDate, String securityLevel, int expectedDelegates) {
        super(clientId, eventTitle, venueLocation, eventDate, securityLevel, expectedDelegates);
    }

    @Override
    public double calculateBaseLogisticsFee() {
        double base = 40000.0 + (getVipGuests() * 2200.0);
        if ("Maximum Discretion".equalsIgnoreCase(getSecurityLevel())) {
            base += 15000.0;
        }
        return base;
    }

    @Override
    public String getEventType() {
        return "SUMMIT";
    }
}