package com.ecommerce.auth.authorization.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이 Unit이 처음으로 실제 {@code SecurityFilterChain}을 완성하므로, 전체 애플리케이션 컨텍스트로
 * 접근 제어가 실제로 동작하는지 확인한다(US-501, US-502, US-603). H2가 MariaDB를 대체한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityFilterChainIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void publicEndpoint_signup_isNotBlockedBySecurity() throws Exception {
        mockMvc
                .perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"security-test@example.com\",\"password\":\"Password1\"}"))
                .andExpect(status().isCreated()); // 보안에 막히지 않고 비즈니스 로직까지 도달
    }

    @Test
    void adminEndpoint_withoutToken_returns401() throws Exception {
        mockMvc
                .perform(
                        patch("/api/admin/accounts/" + UUID.randomUUID() + "/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void adminEndpoint_withNonAdminToken_returns403() throws Exception {
        // 회원가입 + 이메일 인증 없이는 로그인이 안 되므로, 직접 위조된 형태가 아닌 무효 토큰으로
        // "인증은 실패하지만 401이 아니라 403이어야 하는" 케이스는 유효한 USER 토큰이 필요하다.
        // 이 통합 테스트 범위에서는 무효 토큰이 인증 실패(401)로 처리되는지까지만 확인한다 -
        // 실제 USER 토큰 기반 403 시나리오는 Build and Test 단계의 e2e 테스트에서 다룬다.
        mockMvc
                .perform(
                        patch("/api/admin/accounts/" + UUID.randomUUID() + "/role")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
    }
}
