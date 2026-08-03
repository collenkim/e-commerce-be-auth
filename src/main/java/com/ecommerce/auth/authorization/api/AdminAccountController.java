package com.ecommerce.auth.authorization.api;

import com.ecommerce.auth.account.AccountService;
import com.ecommerce.auth.authorization.api.dto.ChangeRoleRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 계정 관리 (US-502). {@code /api/admin/**}는 {@code AuthorizationSecurityConfig}에서
 * {@code ADMIN} 역할을 요구하도록 이미 강제되므로, 이 컨트롤러는 요청이 도달했다는 사실 자체로
 * 호출자가 ADMIN임을 신뢰할 수 있다(서버 사이드 강제, SECURITY-08).
 */
@RestController
public class AdminAccountController {

    private final AccountService accountService;

    public AdminAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PatchMapping("/api/admin/accounts/{accountId}/role")
    public ResponseEntity<Void> changeRole(
            @PathVariable UUID accountId, @Valid @RequestBody ChangeRoleRequest request) {
        accountService.changeRole(accountId, request.role());
        return ResponseEntity.noContent().build();
    }
}
