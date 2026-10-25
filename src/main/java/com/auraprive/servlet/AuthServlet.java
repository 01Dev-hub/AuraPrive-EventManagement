package com.auraprive.servlet;

import com.auraprive.dao.EventDAO;
import com.auraprive.service.DiscreetNotificationService;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Authentication servlet handling multi-role Login (Admin, Client, Vendor),
 * dynamic Client/Vendor Sign-Up, and verified Password Changes.
 */
@WebServlet("/auth")
public class AuthServlet extends HttpServlet {
    private EventDAO eventDAO;

    @Override
    public void init() {
        eventDAO = new EventDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        HttpSession session = request.getSession(false);
        if ("logout".equalsIgnoreCase(action)) {
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login.jsp?msg=Session+signed+out+cleanly.");
            return;
        }
        if (session != null && session.getAttribute("loggedInRole") != null) {
            response.sendRedirect(request.getContextPath() + "/events");
            return;
        }
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        String email = request.getParameter("email");
        if (email != null) email = email.trim().toLowerCase();

        // 1. Dynamic Client / Vendor Sign-Up
        if ("signup".equalsIgnoreCase(action)) {
            String role = request.getParameter("role");
            role = "VENDOR".equalsIgnoreCase(role) ? "VENDOR" : "CLIENT";
            String fullName = request.getParameter("fullName");
            String password = request.getParameter("password");
            String confirm = request.getParameter("confirmPassword");

            if (email == null || email.isEmpty() || fullName == null || fullName.isBlank()
                    || password == null || password.length() < 6 || !password.equals(confirm)) {
                response.sendRedirect(request.getContextPath() + "/login.jsp?err=Sign+up+failed:+Verify+name,+email,+and+matching+6-character+passcode.");
                return;
            }
            if (EventDAO.accountExists(email)) {
                response.sendRedirect(request.getContextPath() + "/login.jsp?err=Account+already+exists+for+" + email + ".+Please+Log+In.");
                return;
            }

            int entityId;
            if ("VENDOR".equals(role)) {
                String specialty = request.getParameter("specialty");
                if (specialty == null || specialty.isBlank()) specialty = "Bespoke Sovereign Logistics";
                entityId = eventDAO.insertVendor(fullName, specialty, "Maximum Discretion");
            } else {
                String tier = request.getParameter("membershipTier");
                if (tier == null || tier.isBlank()) tier = "Sovereign Founders Circle";
                entityId = eventDAO.insertClient(fullName, tier, email);
            }

            if (entityId < 1) {
                response.sendRedirect(request.getContextPath() + "/login.jsp?err=Database+error+while+creating+account.");
                return;
            }

            EventDAO.registerAccountCredentials(email, password, role, entityId);
            HttpSession session = request.getSession(true);
            request.changeSessionId(); // new session id after login (prevents session fixation)
            session.setAttribute("conciergeUser", email);
            session.setAttribute("loggedInRole", role);
            session.setAttribute("loggedInEntityId", entityId);
            DiscreetNotificationService.sendEncryptedDispatch("New " + role + " Account Registered", email);
            response.sendRedirect(request.getContextPath() + "/events");
            return;
        }

        // 2. Verified Password Change (Requires valid current password — no forged token bypass)
        if ("resetPassword".equalsIgnoreCase(action)) {
            String oldPass = request.getParameter("oldPassword");
            String newPass = request.getParameter("newPassword");
            String confirmPass = request.getParameter("confirmPassword");

            if (newPass != null && newPass.equals(confirmPass)
                    && EventDAO.updatePassword(email, oldPass, newPass)) {
                DiscreetNotificationService.sendEncryptedDispatch("Password Updated", email);
                response.sendRedirect(request.getContextPath() + "/login.jsp?msg=Password+updated+successfully.+Please+Log+In.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login.jsp?err=Password+change+failed:+Invalid+email+or+current+password.");
            }
            return;
        }

        // 3. Standard Multi-Role Login (Admin, Client, Vendor)
        String password = request.getParameter("password");
        String[] account = EventDAO.authenticateAccount(email, password);
        if (account != null) {
            HttpSession session = request.getSession(true); 
            request.changeSessionId();
            session.setAttribute("conciergeUser", email);
            session.setAttribute("loggedInRole", account[1]);
            session.setAttribute("loggedInEntityId", Integer.parseInt(account[2]));
            response.sendRedirect(request.getContextPath() + "/events");
        } else {
            response.sendRedirect(request.getContextPath() + "/login.jsp?err=Invalid+Email+or+Passcode.+Access+Denied.");
        }
    }
}