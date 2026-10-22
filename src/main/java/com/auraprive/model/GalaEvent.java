package com.auraprive.model;

/**
 * Specialized polymorphic subclass for Ultra-Luxury Galas.
 */
public class GalaEvent extends Event {

    public GalaEvent(int eventId, int clientId, String eventTitle, String venueLocation,
                     String eventDate, String securityLevel, int expectedVipGuests) {
        super(eventId, clientId, eventTitle, venueLocation, eventDate, securityLevel, expectedVipGuests);
    }

    public GalaEvent(int clientId, String eventTitle, String venueLocation,
                     String eventDate, String securityLevel, int expectedVipGuests) {
        super(clientId, eventTitle, venueLocation, eventDate, securityLevel, expectedVipGuests);
    }

    @Override
    public double calculateBaseLogisticsFee() {
        // Base gala estate buyout: $25,000 + $1,500 per VIP guest
        return 25000.0 + (getVipGuests() * 1500.0);
    }

    @Override
    public String getEventType() {
        return "GALA";
    }
}