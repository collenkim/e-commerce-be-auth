package com.ecommerce.auth.token.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.auth.shared.Role;
import com.ecommerce.auth.token.TokenIssuanceService;
import com.ecommerce.auth.token.dto.TokenValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TokenValidationController.class)
@AutoConfigureMockMvc(addFilters = false)
class TokenValidationControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean private TokenIssuanceService tokenIssuanceService;

    @Test
    void validate_validToken_returnsValidResult() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(tokenIssuanceService.validate("good-token"))
                .thenReturn(TokenValidationResult.valid(accountId, Role.SELLER));

        mockMvc
                .perform(
                        post("/internal/tokens/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RequestBody("good-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    void validate_invalidToken_returnsInvalidResult() throws Exception {
        when(tokenIssuanceService.validate("bad-token")).thenReturn(TokenValidationResult.invalid());

        mockMvc
                .perform(
                        post("/internal/tokens/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RequestBody("bad-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    private record RequestBody(String accessToken) {}
}
