package com.auraprive.servlet;

import com.auraprive.dao.EventDAO;
import com.auraprive.model.Event;
import com.auraprive.model.GalaEvent;
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

        // Enforce VIP Registry Generics & Collections handling
        VIPRegistry<Event> registry = new VIPRegistry<>();
        for (Event ev : rawEvents) {
            registry.register(ev);
        }

        request.setAttribute("events", registry.getAll());
        request.getRequestDispatcher("dashboard.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String title = request.getParameter("title");
        String venue = request.getParameter("venue");
        String date = request.getParameter("date");
        String security = request.getParameter("security");

        // Construct polymorphically
        Event newEvent = new GalaEvent(1, title, venue, date, security, 75);
        boolean success = eventDAO.insertEvent(newEvent);

        if (success) {
            // Multithreaded asynchronous dispatch
            DiscreetNotificationService.sendEncryptedDispatch(title, "concierge@auraprive.internal");
        }

        response.sendRedirect(request.getContextPath() + "/events");
    }
}