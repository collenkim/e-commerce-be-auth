package com.ecommerce.auth.token.api;

import com.ecommerce.auth.token.TokenIssuanceService;
import com.ecommerce.auth.token.api.dto.ValidateTokenRequest;
import com.ecommerce.auth.token.api.dto.ValidateTokenResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-07 — 별도 API 게이트웨이/다른 백엔드 서비스가 이 서비스가 발급한 토큰을 검증하기 위해 호출하는 내부 API.
 * 별도 인증 없이 내부망에서만 접근 가능하다는 전제(Q4:A) — 실제 네트워크 격리는 Infrastructure Design 책임.
 */
@RestController
public class TokenValidationController {

    private final TokenIssuanceService tokenIssuanceService;

    public TokenValidationController(TokenIssuanceService tokenIssuanceService) {
        this.tokenIssuanceService = tokenIssuanceService;
    }

    @PostMapping("/internal/tokens/validate")
    public ValidateTokenResponse validate(@Valid @RequestBody ValidateTokenRequest request) {
        return ValidateTokenResponse.from(tokenIssuanceService.validate(request.accessToken()));
    }
}
