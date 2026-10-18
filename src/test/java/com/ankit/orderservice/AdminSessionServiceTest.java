package com.ankit.orderservice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AdminSessionServiceTest {
    @Test void loginCreatesSessionAndCsrfToken() {
        AdminSessionService service = new AdminSessionService("admin", "A-long-test-password-123");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(true)).thenReturn(session);
        String token = service.login("admin", "A-long-test-password-123", request);
        assertThat(token).isNotBlank();
        verify(session).setAttribute(AdminSessionService.AUTH_ATTR, Boolean.TRUE);
        verify(session).setAttribute(AdminSessionService.CSRF_ATTR, token);
    }


    @Test void acceptsShortConfiguredPasswordsWithoutHardCodedMinimum() {
        AdminSessionService service = new AdminSessionService("admin", "1234");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(true)).thenReturn(session);

        assertThat(service.login("admin", "1234", request)).isNotBlank();
    }

    @Test void wrongPasswordIsRejected() {
        AdminSessionService service = new AdminSessionService("admin", "A-long-test-password-123");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        assertThatThrownBy(() -> service.login("admin", "wrong-password", request))
                .isInstanceOf(OrderExceptions.Unauthorized.class);
    }

    @Test void adminActionsRequireCorrectCsrfToken() {
        AdminSessionService service = new AdminSessionService("admin", "A-long-test-password-123");
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(AdminSessionService.AUTH_ATTR)).thenReturn(Boolean.TRUE);
        when(session.getAttribute(AdminSessionService.CSRF_ATTR)).thenReturn("expected");
        service.requireAdmin(session, "expected");
        assertThatThrownBy(() -> service.requireAdmin(session, "wrong"))
                .isInstanceOf(OrderExceptions.CsrfRejected.class);
    }
}
