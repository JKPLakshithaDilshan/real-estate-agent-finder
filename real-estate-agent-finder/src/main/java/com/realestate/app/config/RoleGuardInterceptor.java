package com.realestate.app.config;

import com.realestate.app.auth.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleGuardInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        HttpSession session = request.getSession(false);
        String role = SessionUtils.getLoggedInRole(session);

        if (path.startsWith("/admin/") && !"/admin/login".equals(path)) {
            if (role == null) {
                response.sendRedirect("/admin/login");
                return false;
            }
            if (!SessionUtils.ROLE_ADMIN.equals(role)) {
                response.sendRedirect("/");
                return false;
            }
        }

        if (path.equals("/account") || path.startsWith("/account/")) {
            if (role == null) {
                response.sendRedirect("/login");
                return false;
            }
            if (!SessionUtils.ROLE_USER.equals(role)) {
                response.sendRedirect(SessionUtils.ROLE_ADMIN.equals(role) ? "/admin/dashboard" : "/");
                return false;
            }
        }

        if (path.startsWith("/agent/")) {
            if (role == null) {
                response.sendRedirect("/login");
                return false;
            }
            if (!SessionUtils.ROLE_AGENT.equals(role)) {
                response.sendRedirect(SessionUtils.ROLE_ADMIN.equals(role) ? "/admin/dashboard" : "/");
                return false;
            }
        }

        return true;
    }
}
