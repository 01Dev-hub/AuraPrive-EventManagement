package com.auraprive.servlet;

import com.auraprive.dao.EventDAO;
import com.auraprive.exception.ExclusiveAccessException;
import com.auraprive.model.Event;
import com.auraprive.model.GalaEvent;
import com.auraprive.model.SummitEvent;
import com.auraprive.model.VIPRegistry;
import com.auraprive.service.DiscreetNotificationService;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/events")
public class EventServlet extends HttpServlet {
    private EventDAO eventDAO;

    @Override
    public void init() {
        eventDAO = new EventDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInRole") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?err=Please+sign+in+to+access+the+portal.");
            return;
        }

        String role = String.valueOf(session.getAttribute("loggedInRole"));
        Integer entityIdObj = (Integer) session.getAttribute("loggedInEntityId");
        int entityId = (entityIdObj != null) ? entityIdObj : 1;

        List<Event> rawEvents = eventDAO.getAllEvents();
        VIPRegistry<Event> registry = new VIPRegistry<>();

        for (Event ev : rawEvents) {
            if ("CLIENT".equalsIgnoreCase(role) && ev.getClientId() != entityId) {
                continue;
            }
            try {
                registry.register(ev);
            } catch (ExclusiveAccessException e) {
                System.err.println("[EventServlet Filter] " + e.getMessage());
            }
        }

        if (request.getParameter("err") != null) {
            request.setAttribute("errorMessage", request.getParameter("err"));
        }
        if (request.getParameter("msg") != null) {
            request.setAttribute("successMessage", request.getParameter("msg"));
        }

        request.setAttribute("events", registry.getAll());
        request.setAttribute("clients", eventDAO.getAllClients());
        request.setAttribute("vendors", eventDAO.getAllVendors());
        request.getRequestDispatcher("dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInRole") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?err=Session+expired.+Please+sign+in.");
            return;
        }

        String role = String.valueOf(session.getAttribute("loggedInRole"));
        Integer entityIdObj = (Integer) session.getAttribute("loggedInEntityId");
        int sessionClientId = (entityIdObj != null) ? entityIdObj : 1;
        String action = request.getParameter("action");

       if ("VENDOR".equalsIgnoreCase(role)) {
            if ("updateVendorStatus".equalsIgnoreCase(action)) {
                try {
                    int eventId = Integer.parseInt(request.getParameter("eventId"));
                    String vendorStatus = request.getParameter("vendorStatus");
                    String vendorEmail = String.valueOf(session.getAttribute("conciergeUser"));
                    if (!eventDAO.updateVendorStatus(eventId, vendorStatus, vendorEmail)) {
                        throw new ExclusiveAccessException("This engagement is already accepted and locked by another vendor.");
                    }
                    DiscreetNotificationService.sendEncryptedDispatch("Vendor Status -> " + vendorStatus, vendorEmail);
                    response.sendRedirect(request.getContextPath() + "/events");
                } catch (Exception e) {
                    response.sendRedirect(request.getContextPath() + "/events?err=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
                }
                return;
            }
            response.sendRedirect(request.getContextPath() + "/events?err=Vendors+may+only+update+logistics+fulfillment+status.");
            return;
        }
        try {
            if ("delete".equalsIgnoreCase(action)) {
                int eventId = Integer.parseInt(request.getParameter("eventId"));
               requireOwnership(role, sessionClientId, eventId);
if (!eventDAO.deleteEvent(eventId)) {
                    throw new ExclusiveAccessException("Unable to delete engagement #" + eventId + " (record not found).");
                }
                response.sendRedirect(request.getContextPath() + "/events");
                return;
            }

            if ("update".equalsIgnoreCase(action)) {
                int eventId = Integer.parseInt(request.getParameter("eventId"));
                String security = request.getParameter("security");
                int guests = Integer.parseInt(request.getParameter("vipGuests"));

requireOwnership(role, sessionClientId, eventId);
if (!Event.isValidClearance(security)) { 
                    throw new ExclusiveAccessException("Invalid security clearance level specified.");
                }
                if (guests < 1 || guests > 500) {
                    throw new ExclusiveAccessException("Updated VIP guest count must be between 1 and 500.");
                }
                if (!eventDAO.updateEvent(eventId, security, guests)) {
                    throw new ExclusiveAccessException("Database failed to update engagement #" + eventId + ".");
                }
                response.sendRedirect(request.getContextPath() + "/events");
                return;
            }

            if ("registerClient".equalsIgnoreCase(action) && "ADMIN".equalsIgnoreCase(role)) {
                String fullName = request.getParameter("fullName");
                String tier = request.getParameter("membershipTier");
                String contact = request.getParameter("encryptedContact");
                int newId = eventDAO.insertClient(fullName, tier, contact);
                if (newId < 1) {
                    throw new ExclusiveAccessException("Failed to onboard client. Check all required fields.");
                }
                EventDAO.registerAccountCredentials(contact, "123456", "CLIENT", newId);
                response.sendRedirect(request.getContextPath() + "/events");
                return;
            }

            if ("registerVendor".equalsIgnoreCase(action) && "ADMIN".equalsIgnoreCase(role)) {
                String vendorName = request.getParameter("vendorName");
                String specialty = request.getParameter("specialty");
                String clearance = request.getParameter("securityClearance");
                if (eventDAO.insertVendor(vendorName, specialty, clearance) < 1) {
                    throw new ExclusiveAccessException("Failed to accredit vendor. Verify clearance protocol.");
                }
                response.sendRedirect(request.getContextPath() + "/events");
                return;
            }

            // Default: CREATE new engagement
            int clientId = "CLIENT".equalsIgnoreCase(role)
                    ? sessionClientId
                    : Integer.parseInt(request.getParameter("clientId"));

            if (!eventDAO.clientExists(clientId)) {
                throw new ExclusiveAccessException("Sponsoring Client ID #" + clientId + " does not exist in the clients table.");
            }

            String clientName = eventDAO.resolveClientName(clientId);
            String title = request.getParameter("title");
            String eventType = request.getParameter("eventType");
            String venue = request.getParameter("venue");
            String date = request.getParameter("date");
            String security = request.getParameter("security");
            int vipGuests = Integer.parseInt(request.getParameter("vipGuests"));

            Event newEvent = "SUMMIT".equalsIgnoreCase(eventType)
                    ? new SummitEvent(0, clientId, clientName, title, venue, date, security, vipGuests)
                    : new GalaEvent(0, clientId, clientName, title, venue, date, security, vipGuests);

            VIPRegistry<Event> validationRegistry = new VIPRegistry<>();
           validationRegistry.register(newEvent);
newEvent.validateNewBooking();

            if (!eventDAO.isVenueAvailable(venue, date)) {
                throw new ExclusiveAccessException("Sanctuary Buyout Conflict: '" + venue + "' is already reserved for an exclusive buyout on " + date + ".");
            }
            if (!eventDAO.insertEventIfVenueFree(newEvent)) {
              throw new ExclusiveAccessException("Engagement could not be saved: the sanctuary was just reserved by someone else, or the database rejected it.");
            }

            DiscreetNotificationService.sendEncryptedDispatch(title, String.valueOf(session.getAttribute("conciergeUser")));
            response.sendRedirect(request.getContextPath() + "/events");

        } catch (ExclusiveAccessException | NumberFormatException e) {
            String encodedErr = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/events?err=" + encodedErr);
        }
    }
        /** CLIENT accounts may only change or cancel their own engagements (ADMIN may touch any). */
    private void requireOwnership(String role, int sessionClientId, int eventId) throws ExclusiveAccessException {
        Event existing = eventDAO.getEventById(eventId);
        if (existing == null) {
            throw new ExclusiveAccessException("Engagement #" + eventId + " not found.");
        }
        if ("CLIENT".equalsIgnoreCase(role) && existing.getClientId() != sessionClientId) {
            throw new ExclusiveAccessException("Access denied: You can only modify your own engagements.");
        }
    }
}