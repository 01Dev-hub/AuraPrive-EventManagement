package com.auraprive.model;

/**
 * Specialized event demonstrating method overriding and polymorphic logistics pricing.
 */
public class GalaEvent extends Event {
    private int expectedVipGuests;

    public GalaEvent(int eventId, int clientId, String eventTitle, String venueLocation, String eventDate, String securityLevel, int expectedVipGuests) {
        super(eventId, clientId, eventTitle, venueLocation, eventDate, securityLevel);
        this.expectedVipGuests = expectedVipGuests;
    }

    public GalaEvent(int clientId, String eventTitle, String venueLocation, String eventDate, String securityLevel, int expectedVipGuests) {
        super(clientId, eventTitle, venueLocation, eventDate, securityLevel);
        this.expectedVipGuests = expectedVipGuests;
    }

    @Override
    public double calculateBaseLogisticsFee() {
        // Base concierge setup: $25,000 + $1,500 per VIP guest
        return 25000.0 + (expectedVipGuests * 1500.0);
    }

    public int getExpectedVipGuests() { return expectedVipGuests; }
}