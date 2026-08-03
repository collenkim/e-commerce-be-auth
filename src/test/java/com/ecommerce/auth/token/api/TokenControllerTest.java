package com.ecommerce.auth.token.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.auth.token.TokenIssuanceService;
import com.ecommerce.auth.token.dto.TokenPair;
import com.ecommerce.auth.token.exception.InvalidTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TokenController.class)
@AutoConfigureMockMvc(addFilters = false) // 보안 필터 체인 구성은 Authorization Unit 책임 (범위 밖)
class TokenControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean private TokenIssuanceService tokenIssuanceService;

    @Test
    void refresh_validRequest_returnsTokenPair() throws Exception {
        when(tokenIssuanceService.refresh("plain-refresh-token"))
                .thenReturn(new TokenPair("new-access", "new-refresh"));

        mockMvc
                .perform(
                        post("/api/auth/token/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RequestBody("plain-refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void refresh_blankToken_returns400() throws Exception {
        mockMvc
                .perform(
                        post("/api/auth/token/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RequestBody(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        when(tokenIssuanceService.refresh(anyString()))
                .thenThrow(new InvalidTokenException("not found"));

        mockMvc
                .perform(
                        post("/api/auth/token/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RequestBody("unknown"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void logout_validRequest_returns204AndInvokesService() throws Exception {
        mockMvc
                .perform(
                        post("/api/auth/logout")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer some-access-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RequestBody("plain-refresh-token"))))
                .andExpect(status().isNoContent());

        verify(tokenIssuanceService).logout("some-access-token", "plain-refresh-token");
    }

    private record RequestBody(String refreshToken) {}
}
