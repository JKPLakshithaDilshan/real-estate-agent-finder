package com.realestate.app.auth;

import com.realestate.app.model.Admin;
import com.realestate.app.model.Agent;
import com.realestate.app.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class SessionUtils {
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_AGENT = "AGENT";
    public static final String SESSION_USER = "sessionUser";
    public static final String LOGGED_IN_USER_ID = "loggedInUserId";
    public static final String LOGGED_IN_USER_NAME = "loggedInUserName";
    public static final String LOGGED_IN_ROLE = "loggedInRole";

    private SessionUtils() {
    }

    public static HttpSession startUserSession(HttpServletRequest request, User user) {
        return startSession(request, new SessionUser(user.getId(), user.getUsername(), user.getEmail(), ROLE_USER));
    }

    public static HttpSession startAdminSession(HttpServletRequest request, Admin admin) {
        return startSession(request, new SessionUser(admin.getId(), admin.getName(), admin.getEmail(), ROLE_ADMIN));
    }

    public static HttpSession startAgentSession(HttpServletRequest request, Agent agent) {
        return startSession(request, new SessionUser(agent.getId(), agent.getName(), agent.getEmail(), ROLE_AGENT));
    }

    public static void refreshUserSession(HttpSession session, User user) {
        if (session == null) {
            return;
        }
        writeSessionUser(session, new SessionUser(user.getId(), user.getUsername(), user.getEmail(), ROLE_USER));
    }

    public static SessionUser getSessionUser(HttpSession session) {
        if (session == null) {
            return null;
        }

        Object sessionUser = session.getAttribute(SESSION_USER);
        if (sessionUser instanceof SessionUser user) {
            return user;
        }

        String id = (String) session.getAttribute(LOGGED_IN_USER_ID);
        String name = (String) session.getAttribute(LOGGED_IN_USER_NAME);
        String role = (String) session.getAttribute(LOGGED_IN_ROLE);
        if (id == null || role == null) {
            return null;
        }

        return new SessionUser(id, name, null, role);
    }

    public static String getLoggedInUserId(HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        return sessionUser == null ? null : sessionUser.getId();
    }

    public static String getLoggedInRole(HttpSession session) {
        SessionUser sessionUser = getSessionUser(session);
        return sessionUser == null ? null : sessionUser.getRole();
    }

    public static boolean isUser(HttpSession session) {
        return ROLE_USER.equals(getLoggedInRole(session));
    }

    public static boolean isAdmin(HttpSession session) {
        return ROLE_ADMIN.equals(getLoggedInRole(session));
    }

    public static boolean isAgent(HttpSession session) {
        return ROLE_AGENT.equals(getLoggedInRole(session));
    }

    private static HttpSession startSession(HttpServletRequest request, SessionUser sessionUser) {
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }

        HttpSession newSession = request.getSession(true);
        writeSessionUser(newSession, sessionUser);
        return newSession;
    }

    private static void writeSessionUser(HttpSession session, SessionUser sessionUser) {
        session.setAttribute(SESSION_USER, sessionUser);
        session.setAttribute(LOGGED_IN_USER_ID, sessionUser.getId());
        session.setAttribute(LOGGED_IN_USER_NAME, sessionUser.getDisplayName());
        session.setAttribute(LOGGED_IN_ROLE, sessionUser.getRole());
    }
}