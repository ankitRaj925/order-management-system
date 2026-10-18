package com.ankit.orderservice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminSessionService {
    public static final String AUTH_ATTR = "oms.admin.authenticated";
    public static final String CSRF_ATTR = "oms.admin.csrf";
    public static final String USER_ATTR = "oms.admin.username";
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 15 * 60 * 1000L;
    private final String username;
    private final boolean passwordConfigured;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final String passwordHash;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public AdminSessionService(@Value("${app.admin-username:}") String username,
                               @Value("${app.admin-password:}") String password) {
        this.username = username == null ? "" : username;
        String configuredPassword = password == null ? "" : password;
        // Accept any non-empty password length; do not impose a hard-coded minimum.
        this.passwordConfigured = !configuredPassword.isEmpty();
        this.passwordHash = configuredPassword.isBlank() ? "" : encoder.encode(configuredPassword);
    }

    public String login(String suppliedUsername, String suppliedPassword, HttpServletRequest request) {
        String key = clientKey(request);
        long now = System.currentTimeMillis();
        Attempts state = attempts.compute(key, (k, old) -> old == null || now - old.startedAt > WINDOW_MS
                ? new Attempts(now, 0) : old);
        if (state.count >= MAX_ATTEMPTS) throw new OrderExceptions.RateLimited();
        boolean configured = !username.isBlank() && passwordConfigured;
        boolean valid = configured && suppliedUsername != null && suppliedPassword != null
                && username.equals(suppliedUsername) && encoder.matches(suppliedPassword, passwordHash);
        if (!valid) {
            state.count++;
            if (!configured) throw new OrderExceptions.AdminDisabled();
            throw new OrderExceptions.Unauthorized();
        }
        attempts.remove(key);
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(AUTH_ATTR, Boolean.TRUE);
        session.setAttribute(USER_ATTR, username);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String csrf = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(CSRF_ATTR, csrf);
        return csrf;
    }

    public boolean isAuthenticated(HttpSession session) {
        return session != null && Boolean.TRUE.equals(session.getAttribute(AUTH_ATTR));
    }

    public void requireAdmin(HttpSession session, String csrf) {
        if (!isAuthenticated(session)) throw new OrderExceptions.Unauthorized();
        Object expected = session.getAttribute(CSRF_ATTR);
        if (!(expected instanceof String value) || csrf == null || !java.security.MessageDigest.isEqual(
                value.getBytes(java.nio.charset.StandardCharsets.UTF_8), csrf.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new OrderExceptions.CsrfRejected();
        }
    }

    public void logout(HttpSession session) {
        if (session != null) session.invalidate();
    }

    private String clientKey(HttpServletRequest request) {
        // Do not trust X-Forwarded-For; use the direct peer unless a trusted proxy is configured externally.
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private static final class Attempts {
        private final long startedAt;
        private int count;
        private Attempts(long startedAt, int count) { this.startedAt = startedAt; this.count = count; }
    }
}
