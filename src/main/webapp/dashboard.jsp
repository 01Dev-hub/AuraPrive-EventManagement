<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, com.auraprive.model.Event" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AURA PRIVÉ | Private Portfolio Dashboard</title>
    <link rel="stylesheet" href="css/luxury-style.css">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500&family=Playfair+Display:ital,wght@0,400;0,600;1,400&display=swap" rel="stylesheet">
</head>
<body>
    <nav class="navbar">
        <div class="brand-logo">AURA PRIVÉ</div>
        <div class="nav-links">
            <a href="index.jsp">Portal Home</a>
            <a href="events">Curations</a>
            <a href="auth?action=logout">Concierge Session (<%= session.getAttribute("conciergeUser") != null ? session.getAttribute("conciergeUser") : "Verified" %>)</a>
        </div>
    </nav>

    <main class="dashboard-container">
        <div class="section-header" style="text-align: left; margin-bottom: 1.5rem;">
            <span class="badge-tag">Restricted Concierge Terminal • Full CRUD & Polymorphic Engine</span>
            <h2>Active Unlisted Engagements</h2>
        </div>

        <% if (request.getAttribute("errorMessage") != null) { %>
            <div style="padding: 1rem; border: 1px solid #d9534f; color: #ff6b6b; margin-bottom: 1.5rem;">
                <%= Event.escapeHtml(String.valueOf(request.getAttribute("errorMessage"))) %>
            </div>
        <% } %>

        <div class="table-wrapper">
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Client ID</th>
                        <th>Title & Tier</th>
                        <th>Sanctuary Venue</th>
                        <th>Date</th>
                        <th>VIP Guests</th>
                        <th>Security Clearance</th>
                        <th>Polymorphic Fee</th>
                        <th>CRUD Operations (Update / Delete)</th>
                    </tr>
                </thead>
                <tbody>
                    <%
                        List<Event> events = (List<Event>) request.getAttribute("events");
                        if (events != null && !events.isEmpty()) {
                            for (Event ev : events) {
                    %>
                    <tr>
                        <td>#PRV-00<%= ev.getEventId() %></td>
                        <td>Client #<%= ev.getClientId() %></td>
                        <td>
                            <strong><%= Event.escapeHtml(ev.getEventTitle()) %></strong><br>
                            <small style="color: var(--gold-accent);">[<%= Event.escapeHtml(ev.getEventType()) %>]</small>
                        </td>
                        <td><%= Event.escapeHtml(ev.getVenueLocation()) %></td>
                        <td><%= Event.escapeHtml(ev.getEventDate()) %></td>
                        <td><%= ev.getVipGuests() %></td>
                        <td><%= Event.escapeHtml(ev.getSecurityClearance()) %></td>
                        <td>$<%= String.format("%,.2f", ev.calculateBaseLogisticsFee()) %></td>
                        <td>
                            <form action="events" method="POST" class="inline-form">
                                <input type="hidden" name="action" value="update">
                                <input type="hidden" name="eventId" value="<%= ev.getEventId() %>">
                                <select name="security" style="width: 145px; padding: 0.35rem; font-size: 0.75rem;">
                                    <option value="Maximum Discretion" <%= "Maximum Discretion".equals(ev.getSecurityClearance()) ? "selected" : "" %>>Maximum Discretion</option>
                                    <option value="Diplomatic Tier" <%= "Diplomatic Tier".equals(ev.getSecurityClearance()) ? "selected" : "" %>>Diplomatic Tier</option>
                                    <option value="Confidential" <%= "Confidential".equals(ev.getSecurityClearance()) ? "selected" : "" %>>Confidential</option>
                                </select>
                                <input type="number" name="vipGuests" value="<%= ev.getVipGuests() %>" min="1" max="500" style="width: 68px; padding: 0.35rem; font-size: 0.75rem;">
                                <button type="submit" class="btn-small">Update</button>
                            </form>
                            <form action="events" method="POST" class="inline-form" style="margin-left: 0.3rem;">
                                <input type="hidden" name="action" value="delete">
                                <input type="hidden" name="eventId" value="<%= ev.getEventId() %>">
                                <button type="submit" class="btn-small btn-danger">Delete</button>
                            </form>
                        </td>
                    </tr>
                    <%
                            }
                        } else {
                    %>
                    <tr>
                        <td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2rem;">No confidential curations registered. Commission an engagement below.</td>
                    </tr>
                    <%
                        }
                    %>
                </tbody>
            </table>
        </div>

        <div class="form-box">
            <h3 style="font-family: var(--font-heading); margin-bottom: 1.5rem; color: var(--gold-accent);">Commission Bespoke Engagement (Create)</h3>
            <form action="events" method="POST">
                <input type="hidden" name="action" value="create">
                <div class="form-grid">
                    <div class="form-group">
                        <label for="clientId">Sponsoring UHNW Client</label>
                        <select id="clientId" name="clientId" required>
                            <%
                                List<String[]> clients = (List<String[]>) request.getAttribute("clients");
                                if (clients != null) {
                                    for (String[] c : clients) {
                            %>
                            <option value="<%= c[0] %>">#<%= c[0] %> — <%= Event.escapeHtml(c[1]) %> (<%= Event.escapeHtml(c[2]) %>)</option>
                            <%
                                    }
                                } else {
                            %>
                            <option value="1">#1 — Maharaja Vikramaditya Rathore (Sovereign Founders Circle)</option>
                            <% } %>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="eventType">Polymorphic Event Class</label>
                        <select id="eventType" name="eventType" required>
                            <option value="GALA">GalaEvent ($25k Base + $1,500/Guest)</option>
                            <option value="SUMMIT">SummitEvent ($40k Base + $2,200/Delegate + Airspace Lock)</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="title">Engagement Title</label>
                        <input type="text" id="title" name="title" placeholder="e.g., Jaipur Royal Heritage Gala" required>
                    </div>
                    <div class="form-group">
                        <label for="venue">Sanctuary Venue</label>
                        <input type="text" id="venue" name="venue" placeholder="e.g., Rambagh Palace, Jaipur" required>
                    </div>
                    <div class="form-group">
                        <label for="date">Engagement Date</label>
                        <input type="date" id="date" name="date" required>
                    </div>
                    <div class="form-group">
                        <label for="vipGuests">Expected VIP Guests (1–500)</label>
                        <input type="number" id="vipGuests" name="vipGuests" value="60" min="1" max="500" required>
                    </div>
                    <div class="form-group">
                        <label for="security">Security Clearance Protocol</label>
                        <select id="security" name="security">
                            <option value="Maximum Discretion">Maximum Discretion (Zero Airspace Exposure)</option>
                            <option value="Diplomatic Tier">Diplomatic Tier (Escorted Fleet & Screening)</option>
                            <option value="Confidential">Confidential (Strict NDA Enforced)</option>
                        </select>
                    </div>
                </div>
                <button type="submit" class="cta-button" style="width: 100%; margin-top: 0.5rem;">Authorize & Register Engagement</button>
            </form>
        </div>

        <!-- Relational Database Tables: Clients & Elite Vendors -->
        <div class="sub-grid">
            <div>
                <h3 style="font-family: var(--font-heading); color: var(--gold-accent); margin-bottom: 0.8rem;">Verified UHNW Clients (<code>clients</code> Table)</h3>
                <table>
                    <thead>
                        <tr><th>ID</th><th>Client Name</th><th>Tier</th><th>Encrypted Contact</th></tr>
                    </thead>
                    <tbody>
                        <%
                            if (clients != null) {
                                for (String[] c : clients) {
                        %>
                        <tr>
                            <td>#<%= Event.escapeHtml(c[0]) %></td>
                            <td><%= Event.escapeHtml(c[1]) %></td>
                            <td><%= Event.escapeHtml(c[2]) %></td>
                            <td><%= Event.escapeHtml(c[3]) %></td>
                        </tr>
                        <%      }
                            }
                        %>
                    </tbody>
                </table>
            </div>
            <div>
                <h3 style="font-family: var(--font-heading); color: var(--gold-accent); margin-bottom: 0.8rem;">Cleared Syndicate Vendors (<code>elite_vendors</code> Table)</h3>
                <table>
                    <thead>
                        <tr><th>ID</th><th>Vendor</th><th>Specialty</th><th>Clearance</th></tr>
                    </thead>
                    <tbody>
                        <%
                            List<String[]> vendors = (List<String[]>) request.getAttribute("vendors");
                            if (vendors != null) {
                                for (String[] v : vendors) {
                        %>
                        <tr>
                            <td>#<%= Event.escapeHtml(v[0]) %></td>
                            <td><%= Event.escapeHtml(v[1]) %></td>
                            <td><%= Event.escapeHtml(v[2]) %></td>
                            <td><%= Event.escapeHtml(v[3]) %></td>
                        </tr>
                        <%      }
                            }
                        %>
                    </tbody>
                </table>
            </div>
        </div>
    </main>
</body>
</html>