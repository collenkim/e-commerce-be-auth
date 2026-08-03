package com.ecommerce.auth.authorization.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ecommerce.auth.account.AccountService;
import com.ecommerce.auth.account.exception.AccountNotFoundException;
import com.ecommerce.auth.shared.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminAccountController.class)
@AutoConfigureMockMvc(addFilters = false) // 이 테스트는 컨트롤러 로직만 검증 - 인가 자체는 AuthorizationSecurityConfig 통합 시점에 검증
class AdminAccountControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean private AccountService accountService;

    @Test
    void changeRole_validRequest_returns204() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc
                .perform(
                        patch("/api/admin/accounts/" + accountId + "/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RoleBody("SELLER"))))
                .andExpect(status().isNoContent());

        verify(accountService).changeRole(accountId, Role.SELLER);
    }

    @Test
    void changeRole_accountNotFound_returns404() throws Exception {
        UUID accountId = UUID.randomUUID();
        doThrow(new AccountNotFoundException("not found"))
                .when(accountService)
                .changeRole(accountId, Role.ADMIN);

        mockMvc
                .perform(
                        patch("/api/admin/accounts/" + accountId + "/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RoleBody("ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeRole_invalidRoleValue_returns400() throws Exception {
        mockMvc
                .perform(
                        patch("/api/admin/accounts/" + UUID.randomUUID() + "/role")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"SUPERUSER\"}"))
                .andExpect(status().isBadRequest());
    }

    private record RoleBody(String role) {}
}
