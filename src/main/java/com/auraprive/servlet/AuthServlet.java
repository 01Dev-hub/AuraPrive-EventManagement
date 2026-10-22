package com.auraprive.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Authentication controller managing concierge sessions and SHA-256 credential verification.
 */
@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

    // SHA-256 hash of default concierge passcode: "Sovereign@2026"
    private static final String CONCIERGE_EMAIL = "concierge@auraprive.internal";
    private static final String PASSCODE_SHA256 = sha256("Sovereign@2026");

    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : encoded) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 initialization error", e);
        }
    }

    public static boolean verifyCredentials(String email, String password) {
        if (email == null || password == null) return false;
        return CONCIERGE_EMAIL.equalsIgnoreCase(email.trim())
                && PASSCODE_SHA256.equals(sha256(password));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("logout".equalsIgnoreCase(action)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/login.jsp?loggedOut=true");
            return;
        }
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (verifyCredentials(email, password)) {
            HttpSession session = request.getSession(true);
            session.setAttribute("conciergeUser", CONCIERGE_EMAIL);
            session.setAttribute("clearanceRole", "SOVEREIGN_CONCIERGE");
            response.sendRedirect(request.getContextPath() + "/events");
        } else {
            request.setAttribute("authError", "Invalid Concierge Credentials. Access Denied.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }
}