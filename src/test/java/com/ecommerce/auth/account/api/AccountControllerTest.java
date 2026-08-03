package com.ecommerce.auth.account.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.auth.account.AccountService;
import com.ecommerce.auth.account.exception.DuplicateEmailException;
import com.ecommerce.auth.account.exception.EmailNotVerifiedException;
import com.ecommerce.auth.account.exception.InvalidCredentialsException;
import com.ecommerce.auth.account.exception.WeakPasswordException;
import com.ecommerce.auth.ratelimit.RateLimitService;
import com.ecommerce.auth.ratelimit.exception.AccountBlockedException;
import com.ecommerce.auth.token.dto.TokenPair;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean private AccountService accountService;
    @MockitoBean private RateLimitService rateLimitService;

    @Test
    void signUp_validRequest_returns201WithAccountId() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(accountService.signUp("user@example.com", "Password1")).thenReturn(accountId);

        mockMvc
                .perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new SignUpBody("user@example.com", "Password1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()));
    }

    @Test
    void signUp_duplicateEmail_returns409() throws Exception {
        when(accountService.signUp(anyString(), anyString()))
                .thenThrow(new DuplicateEmailException("dup"));

        mockMvc
                .perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new SignUpBody("user@example.com", "Password1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void signUp_invalidEmailFormat_returns400() throws Exception {
        mockMvc
                .perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(new SignUpBody("not-an-email", "Password1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_weakPassword_returns400() throws Exception {
        when(accountService.signUp(anyString(), anyString())).thenThrow(new WeakPasswordException("weak"));

        mockMvc
                .perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new SignUpBody("user@example.com", "weakpassword"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));
    }

    @Test
    void verifyEmail_validToken_returns204() throws Exception {
        mockMvc
                .perform(
                        post("/api/auth/email/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new TokenBody("verification-token"))))
                .andExpect(status().isNoContent());

        verify(accountService).verifyEmail("verification-token");
    }

    @Test
    void login_validCredentials_returns200WithTokens() throws Exception {
        when(accountService.login("user@example.com", "Password1"))
                .thenReturn(new TokenPair("access", "refresh"));

        mockMvc
                .perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginBody("user@example.com", "Password1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(accountService.login(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException("bad"));

        mockMvc
                .perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginBody("user@example.com", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        verify(rateLimitService).recordLoginFailure(anyString(), eq("user@example.com"));
    }

    @Test
    void login_accountBlockedByRateLimit_returns429() throws Exception {
        doThrow(new AccountBlockedException("locked"))
                .when(rateLimitService)
                .assertAccountNotBlocked("user@example.com");

        mockMvc
                .perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginBody("user@example.com", "Password1"))))
                .andExpect(status().is(429))
                .andExpect(jsonPath("$.code").value("ACCOUNT_TEMPORARILY_LOCKED"));
    }

    @Test
    void login_unverifiedEmail_returns403() throws Exception {
        when(accountService.login(anyString(), anyString()))
                .thenThrow(new EmailNotVerifiedException("unverified"));

        mockMvc
                .perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new LoginBody("user@example.com", "Password1"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void requestPasswordReset_anyEmail_returns204() throws Exception {
        mockMvc
                .perform(
                        post("/api/auth/password-reset/request")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new EmailBody("user@example.com"))))
                .andExpect(status().isNoContent());

        verify(accountService).requestPasswordReset("user@example.com");
    }

    @Test
    void resetPassword_validRequest_returns204() throws Exception {
        mockMvc
                .perform(
                        post("/api/auth/password-reset/execute")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new ResetBody("reset-token", "NewPassword1"))))
                .andExpect(status().isNoContent());

        verify(accountService).resetPassword("reset-token", "NewPassword1");
    }

    private record SignUpBody(String email, String password) {}

    private record TokenBody(String token) {}

    private record LoginBody(String email, String password) {}

    private record EmailBody(String email) {}

    private record ResetBody(String token, String newPassword) {}
}
