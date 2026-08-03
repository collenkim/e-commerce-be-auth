package com.ecommerce.auth.sociallogin.api;

import com.ecommerce.auth.sociallogin.SocialLoginService;
import com.ecommerce.auth.sociallogin.api.dto.LinkConfirmRequest;
import com.ecommerce.auth.token.api.dto.TokenPairResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 계정 연동 확인 (US-201/202/203 AC2, Application Design Q2:C). */
@RestController
public class LinkConfirmController {

    private final SocialLoginService socialLoginService;

    public LinkConfirmController(SocialLoginService socialLoginService) {
        this.socialLoginService = socialLoginService;
    }

    @PostMapping("/api/auth/social/link/confirm")
    public TokenPairResponse confirmLink(@Valid @RequestBody LinkConfirmRequest request) {
        return TokenPairResponse.from(
                socialLoginService.confirmLink(request.linkToken(), request.existingPassword()));
    }
}
