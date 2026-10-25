<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    boolean isLoggedIn = (session != null && session.getAttribute("loggedInRole") != null);
    String loggedInEmail = isLoggedIn ? String.valueOf(session.getAttribute("conciergeUser")) : "";
    String loggedInRole = isLoggedIn ? String.valueOf(session.getAttribute("loggedInRole")) : "";
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AURA PRIVÉ | Sovereign Event Architecture</title>
    <link rel="stylesheet" href="css/luxury-style.css">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&family=Playfair+Display:ital,wght@0,400;0,600;1,400&display=swap" rel="stylesheet">
    <style>
        .power-logout-btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 34px;
            height: 34px;
            border-radius: 50%;
            border: 1px solid var(--danger-accent);
            color: var(--danger-accent) !important;
            transition: all 0.25s ease;
        }
        .power-logout-btn:hover {
            background: var(--danger-accent);
            color: oklch(97% 0.01 85) !important;
            box-shadow: 0 0 14px var(--danger-accent);
        }
        .pillar-action {
            display: inline-block;
            margin-top: 1.2rem;
            font-size: 0.74rem;
            text-transform: uppercase;
            letter-spacing: 0.15em;
            color: var(--gold-accent);
            text-decoration: none;
            border-bottom: 1px solid var(--border-subtle);
            padding-bottom: 0.2rem;
        }
        .pillar-action:hover {
            color: var(--gold-hover);
            border-bottom-color: var(--gold-hover);
        }
    </style>
</head>
<body>
    <nav class="navbar">
        <div class="brand-logo">AURA PRIVÉ</div>
        <div class="nav-links" style="align-items: center;" id="landingNavLinks">
            <a href="#services">Bespoke Pillars</a>
            <% if (isLoggedIn) { %>
                <a href="events" style="color: var(--gold-accent);">My Active Portal (<%= loggedInRole %>)</a>
                <a href="auth?action=logout" class="power-logout-btn" title="End Session" aria-label="End Session">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M18.36 6.64a9 9 0 1 1-12.73 0"></path>
                        <line x1="12" y1="2" x2="12" y2="12"></line>
                    </svg>
                </a>
            <% } else { %>
                <a href="events">Client / Vendor Portal</a>
            <% } %>
        </div>
    </nav>

    <header class="hero">
        <div class="hero-content">
            <span class="badge-tag">By Sovereign Invitation Only</span>
            <h1>Architecting the Unseen.</h1>
            <p>From zero-airspace heritage palace buyouts in Udaipur to closed-door diplomatic summits in Jodhpur, Aura Privé orchestrates gatherings beyond the public ledger.</p>
            <a href="events" class="cta-button">Enter Sovereign Portal</a>
        </div>
    </header>

    <section id="services" class="services-section">
        <div class="section-header">
            <span class="badge-tag">Operational Capabilities</span>
            <h2>The Three Pillars of Discretion</h2>
        </div>
        <div class="cards-grid">
            <div class="service-card">
                <span class="card-number">01 / SANCTUARY BUYOUTS</span>
                <h3>Royal Palace Enclaves</h3>
                <p>Complete acoustic, maritime, and airspace isolation across heritage Indian palaces and private island estates with single-client date locking.</p>
                <a href="events" class="pillar-action">Commission Sanctuary Buyout &rarr;</a>
            </div>
            <div class="service-card">
                <span class="card-number">02 / SOVEREIGN LOGISTICS</span>
                <h3>Polymorphic Curation</h3>
                <p>Dynamic logistics modeling tailored to Royal Galas ($25k base) and Diplomatic Summits ($40k base + airspace lock) with real-time clearance verification.</p>
                <a href="events" class="pillar-action">Launch Logistics Engine &rarr;</a>
            </div>
            <div class="service-card">
                <span class="card-number">03 / ZERO-TRUST TELEMETRY</span>
                <h3>Encrypted Dispatch</h3>
                <p>Asynchronous thread-pooled concierge notifications and role-isolated portals for UHNW Clients and Syndicate Vendors.</p>
                <a href="events" class="pillar-action">Open Role-Isolated Terminal &rarr;</a>
            </div>
        </div>
    </section>

    <footer>
        <p>&copy; 2026 AURA PRIVÉ SOVEREIGN CONCIERGE. ALL RIGHTS RESERVED.</p>
    </footer>
</body>
</html>