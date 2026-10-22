package com.auraprive.servlet;

import com.auraprive.dao.EventDAO;
import com.auraprive.exception.ExclusiveAccessException;
import com.auraprive.model.Event;
import com.auraprive.model.GalaEvent;
import com.auraprive.model.SummitEvent;
import com.auraprive.model.VIPRegistry;
import com.auraprive.service.DiscreetNotificationService;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
        List<Event> rawEvents = eventDAO.getAllEvents();
        VIPRegistry<Event> registry = new VIPRegistry<>();

        for (Event ev : rawEvents) {
            try {
                registry.register(ev);
            } catch (ExclusiveAccessException e) {
                System.err.println(e.getMessage());
            }
        }

        request.setAttribute("events", registry.getAll());
        request.setAttribute("clients", eventDAO.getAllClients());
        request.setAttribute("vendors", eventDAO.getAllVendors());
        request.getRequestDispatcher("dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        try {
            if ("delete".equalsIgnoreCase(action)) {
                int eventId = Integer.parseInt(request.getParameter("eventId"));
                eventDAO.deleteEvent(eventId);
                response.sendRedirect(request.getContextPath() + "/events");
                return;
            }

            if ("update".equalsIgnoreCase(action)) {
                int eventId = Integer.parseInt(request.getParameter("eventId"));
                String security = request.getParameter("security");
                int guests = Integer.parseInt(request.getParameter("vipGuests"));
                if (guests < 1 || guests > 500) {
                    throw new ExclusiveAccessException("Updated guest count must be between 1 and 500.");
                }
                eventDAO.updateEvent(eventId, security, guests);
                response.sendRedirect(request.getContextPath() + "/events");
                return;
            }

            // Default action: CREATE new engagement
            int clientId = Integer.parseInt(request.getParameter("clientId"));
            String title = request.getParameter("title");
            String eventType = request.getParameter("eventType");
            String venue = request.getParameter("venue");
            String date = request.getParameter("date");
            String security = request.getParameter("security");
            int vipGuests = Integer.parseInt(request.getParameter("vipGuests"));

            Event newEvent;
            if ("SUMMIT".equalsIgnoreCase(eventType)) {
                newEvent = new SummitEvent(clientId, title, venue, date, security, vipGuests);
            } else {
                newEvent = new GalaEvent(clientId, title, venue, date, security, vipGuests);
            }

            // Validate via VIPRegistry & ExclusiveAccessException before DB insert
            VIPRegistry<Event> validationRegistry = new VIPRegistry<>();
            validationRegistry.register(newEvent);

            if (eventDAO.insertEvent(newEvent)) {
                DiscreetNotificationService.sendEncryptedDispatch(title, "concierge@auraprive.internal");
            }
            response.sendRedirect(request.getContextPath() + "/events");

        } catch (ExclusiveAccessException | NumberFormatException e) {
            request.setAttribute("errorMessage", e.getMessage());
            doGet(request, response);
        }
    }
}