<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, com.auraprive.model.Event" %>
<%
    String role = String.valueOf(session.getAttribute("loggedInRole") != null ? session.getAttribute("loggedInRole") : "ADMIN");
    String userEmail = String.valueOf(session.getAttribute("conciergeUser") != null ? session.getAttribute("conciergeUser") : "Authenticated");
    Integer entityIdObj = (Integer) session.getAttribute("loggedInEntityId");
    int activeClientId = (entityIdObj != null) ? entityIdObj : 1;
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AURA PRIVÉ | <%= Event.escapeHtml(role) %> Portal</title>
    <link rel="stylesheet" href="css/luxury-style.css">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&family=Playfair+Display:ital,wght@0,400;0,600;1,400&display=swap" rel="stylesheet">
</head>
<body>
    <nav class="navbar">
        <div class="brand-logo">AURA PRIVÉ</div>
        <div class="nav-links">
            <a href="index.jsp">Sanctuary Home</a>
            <span style="color:var(--gold-accent);font-size:0.76rem;letter-spacing:0.12em;text-transform:uppercase;"><%= Event.escapeHtml(userEmail) %> (<%= Event.escapeHtml(role) %>)</span>
            <a href="auth?action=logout" style="color:var(--danger-accent);">Sign Out</a>
        </div>
    </nav>

    <main class="dashboard-container">
        <div class="section-header" style="text-align: left; margin-bottom: 1.5rem;">
            <span class="badge-tag"><%= "VENDOR".equalsIgnoreCase(role) ? "Syndicate Logistics Terminal • Read-Only" : "Restricted Concierge & Client Sanctuary" %></span>
            <h2><%= "CLIENT".equalsIgnoreCase(role) ? "My Commissioned Engagements" : ("VENDOR".equalsIgnoreCase(role) ? "Assigned Sanctuary Logistics Briefings" : "Active Unlisted Engagements") %></h2>
        </div>

        <% if (request.getAttribute("errorMessage") != null) { %>
            <div style="padding: 1rem; border: 1px solid var(--danger-accent); color: var(--danger-accent); border-radius: 8px; margin-bottom: 1.5rem;">
                <%= Event.escapeHtml(String.valueOf(request.getAttribute("errorMessage"))) %>
            </div>
        <% } %>

        <div class="table-wrapper">
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <% if (!"VENDOR".equalsIgnoreCase(role)) { %>
                            <th>Sponsoring Client (SQL JOIN)</th>
                        <% } %>
                        <th>Title & Tier</th>
                        <th>Sanctuary Venue</th>
                        <th>Date</th>
                        <th>VIP Guests</th>
                        <th>Security Clearance</th>
                        <% if (!"VENDOR".equalsIgnoreCase(role)) { %>
                            <th>Polymorphic Fee</th>
                            <th>Manage Booking</th>
                        <% } %>
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
                        <% if (!"VENDOR".equalsIgnoreCase(role)) { %>
                            <td>
                                <strong><%= Event.escapeHtml(ev.getClientName()) %></strong><br>
                                <small style="color: var(--text-muted);">Client #<%= ev.getClientId() %></small>
                            </td>
                        <% } %>
                        <td>
                            <strong><%= Event.escapeHtml(ev.getEventTitle()) %></strong><br>
                            <small style="color: var(--gold-accent);">[<%= Event.escapeHtml(ev.getEventType()) %>]</small>
                        </td>
                        <td><%= Event.escapeHtml(ev.getVenueLocation()) %></td>
                        <td><%= Event.escapeHtml(ev.getEventDate()) %></td>
                        <td><%= ev.getVipGuests() %></td>
                        <td><%= Event.escapeHtml(ev.getSecurityClearance()) %></td>
                        <% if (!"VENDOR".equalsIgnoreCase(role)) { %>
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
                                    <button type="submit" class="btn-small btn-danger">Cancel</button>
                                </form>
                            </td>
                        <% } %>
                    </tr>
                    <%
                            }
                        } else {
                    %>
                    <tr>
                        <td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2rem;">No confidential curations registered for this view.</td>
                    </tr>
                    <%
                        }
                    %>
                </tbody>
            </table>
        </div>

        <%
            List<String[]> clients = (List<String[]>) request.getAttribute("clients");
            if (!"VENDOR".equalsIgnoreCase(role)) {
        %>
        <div class="form-box" style="margin-top: 2rem;">
            <h3 style="font-family: var(--font-heading); margin-bottom: 1.5rem; color: var(--gold-accent);">Commission Bespoke Engagement (Create)</h3>
            <form action="events" method="POST">
                <input type="hidden" name="action" value="create">
                <div class="form-grid">
                    <% if ("ADMIN".equalsIgnoreCase(role)) { %>
                        <div class="form-group">
                            <label for="clientId">Sponsoring UHNW Client</label>
                            <select id="clientId" name="clientId" required>
                                <%
                                    if (clients != null) {
                                        for (String[] c : clients) {
                                %>
                                <option value="<%= c[0] %>">#<%= c[0] %> — <%= Event.escapeHtml(c[1]) %> (<%= Event.escapeHtml(c[2]) %>)</option>
                                <%
                                        }
                                    }
                                %>
                            </select>
                        </div>
                    <% } else { %>
                        <input type="hidden" name="clientId" value="<%= activeClientId %>">
                    <% } %>
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
                            <option value="Maximum Discretion">Maximum Discretion</option>
                            <option value="Diplomatic Tier">Diplomatic Tier</option>
                            <option value="Confidential">Confidential</option>
                        </select>
                    </div>
                </div>
                <button type="submit" class="cta-button" style="width: 100%; margin-top: 0.5rem;">Authorize & Register Engagement</button>
            </form>
        </div>
        <% } %>

        <% if ("ADMIN".equalsIgnoreCase(role)) { %>
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
        <% } %>
    </main>
</body>
</html>