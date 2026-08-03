package com.ecommerce.auth.sociallogin.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.auth.sociallogin.SocialLoginService;
import com.ecommerce.auth.sociallogin.exception.InvalidLinkTokenException;
import com.ecommerce.auth.sociallogin.exception.LinkPasswordMismatchException;
import com.ecommerce.auth.token.dto.TokenPair;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LinkConfirmController.class)
@AutoConfigureMockMvc(addFilters = false)
class LinkConfirmControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean private SocialLoginService socialLoginService;

    @Test
    void confirmLink_validRequest_returns200WithTokens() throws Exception {
        when(socialLoginService.confirmLink("link-token", "correct-password"))
                .thenReturn(new TokenPair("access", "refresh"));

        mockMvc
                .perform(
                        post("/api/auth/social/link/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new RequestBody("link-token", "correct-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    void confirmLink_invalidToken_returns400() throws Exception {
        when(socialLoginService.confirmLink(anyString(), anyString()))
                .thenThrow(new InvalidLinkTokenException("expired"));

        mockMvc
                .perform(
                        post("/api/auth/social/link/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RequestBody("bad-token", "pw"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LINK_TOKEN"));
    }

    @Test
    void confirmLink_wrongPassword_returns401() throws Exception {
        when(socialLoginService.confirmLink(anyString(), anyString()))
                .thenThrow(new LinkPasswordMismatchException("mismatch"));

        mockMvc
                .perform(
                        post("/api/auth/social/link/confirm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RequestBody("token", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LINK_PASSWORD_MISMATCH"));
    }

    private record RequestBody(String linkToken, String existingPassword) {}
}
