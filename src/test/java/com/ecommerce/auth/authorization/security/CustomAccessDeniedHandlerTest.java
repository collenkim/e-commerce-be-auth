package com.ecommerce.auth.authorization.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class CustomAccessDeniedHandlerTest {

    private final CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler();

    @Test
    void handle_writes403WithJsonBody() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        handler.handle(request, response, new AccessDeniedException("denied"));

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertThat(body.toString()).contains("ACCESS_DENIED");
    }
}
