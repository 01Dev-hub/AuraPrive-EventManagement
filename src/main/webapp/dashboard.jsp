<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, com.auraprive.model.Event" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AURA PRIVÉ | Private Portfolio Dashboard</title>
    <link rel="stylesheet" href="css/luxury-style.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500&family=Playfair+Display:ital,wght@0,400;0,600;1,400&display=swap" rel="stylesheet">
    <style>
        .dashboard-container { padding: 4rem 8%; }
        .table-wrapper { margin-top: 2rem; overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; background-color: var(--bg-card); border: 1px solid var(--border-subtle); }
        th, td { padding: 1.2rem 1.5rem; text-align: left; border-bottom: 1px solid var(--border-subtle); font-size: 0.9rem; }
        th { color: var(--gold-accent); text-transform: uppercase; font-size: 0.75rem; letter-spacing: 0.15em; }
        td { color: var(--text-primary); }
        .form-box { background: var(--bg-card); border: 1px solid var(--border-subtle); padding: 2.5rem; margin-top: 3.5rem; }
        .form-group { margin-bottom: 1.5rem; }
        label { display: block; margin-bottom: 0.5rem; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.15em; color: var(--gold-accent); }
        input, select { width: 100%; padding: 0.85rem; background: var(--bg-primary); border: 1px solid var(--border-subtle); color: var(--text-primary); outline: none; font-family: inherit; }
        input:focus, select:focus { border-color: var(--gold-accent); }
    </style>
</head>
<body>
    <nav class="navbar">
        <div class="brand-logo">AURA PRIVÉ</div>
        <div class="nav-links">
            <a href="index.jsp">Portal Home</a>
            <a href="events">Curations</a>
        </div>
    </nav>

    <main class="dashboard-container">
        <div class="section-header" style="text-align: left; margin-bottom: 2rem;">
            <span class="badge-tag">Restricted Concierge Terminal</span>
            <h2>Active Unlisted Engagements</h2>
        </div>

        <div class="table-wrapper">
            <table>
                <thead>
                    <tr>
                        <th>Registry ID</th>
                        <th>Engagement Title</th>
                        <th>Sanctuary Location</th>
                        <th>Scheduled Date</th>
                        <th>Security Clearance</th>
                        <th>Logistics Allocation</th>
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
                        <td><%= ev.getEventTitle() %></td>
                        <td><%= ev.getVenueLocation() %></td>
                        <td><%= ev.getEventDate() %></td>
                        <td><%= ev.getSecurityClearance() %></td>
                        <td>$<%= String.format("%,.2f", ev.calculateBaseLogisticsFee()) %></td>
                    </tr>
                    <%
                            }
                        } else {
                    %>
                    <tr>
                        <td colspan="6" style="text-align: center; color: var(--text-muted); padding: 2rem;">No confidential curations registered. Commission an engagement below.</td>
                    </tr>
                    <%
                        }
                    %>
                </tbody>
            </table>
        </div>

        <div class="form-box">
            <h3 style="font-family: var(--font-heading); margin-bottom: 1.5rem; color: var(--gold-accent);">Commission Bespoke Engagement</h3>
            <form action="events" method="POST">
                <div class="form-group">
                    <label for="title">Engagement Title</label>
                    <input type="text" id="title" name="title" placeholder="e.g., Solstice Sovereign Gala" required>
                </div>
                <div class="form-group">
                    <label for="venue">Sanctuary Venue</label>
                    <input type="text" id="venue" name="venue" placeholder="e.g., Lake Como Private Villa Compound" required>
                </div>
                <div class="form-group">
                    <label for="date">Engagement Date</label>
                    <input type="date" id="date" name="date" required>
                </div>
                <div class="form-group">
                    <label for="security">Protocol Level</label>
                    <select id="security" name="security">
                        <option value="Maximum Discretion">Maximum Discretion (Zero Airspace Exposure)</option>
                        <option value="Diplomatic Tier">Diplomatic Tier (Escorted Fleet & Screening)</option>
                        <option value="Confidential">Confidential (Strict NDA Enforced)</option>
                    </select>
                </div>
                <button type="submit" class="cta-button" style="width: 100%;">Authorize & Register Engagement</button>
            </form>
        </div>
    </main>
</body>
</html>