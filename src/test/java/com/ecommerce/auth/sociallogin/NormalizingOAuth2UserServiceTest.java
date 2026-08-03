package com.ecommerce.auth.sociallogin;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.auth.sociallogin.domain.SocialProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NormalizingOAuth2UserServiceTest {

    @Test
    void extractProviderUserId_google_readsSubClaim() {
        Map<String, Object> attributes = Map.of("sub", "google-user-1", "email", "a@example.com");

        assertThat(NormalizingOAuth2UserService.extractProviderUserId(SocialProvider.GOOGLE, attributes))
                .isEqualTo("google-user-1");
    }

    @Test
    void extractEmail_google_readsEmailClaim() {
        Map<String, Object> attributes = Map.of("sub", "google-user-1", "email", "a@example.com");

        assertThat(NormalizingOAuth2UserService.extractEmail(SocialProvider.GOOGLE, attributes))
                .isEqualTo("a@example.com");
    }

    @Test
    void extractProviderUserId_kakao_readsTopLevelId() {
        Map<String, Object> attributes =
                Map.of("id", 123456789L, "kakao_account", Map.of("email", "k@example.com"));

        assertThat(NormalizingOAuth2UserService.extractProviderUserId(SocialProvider.KAKAO, attributes))
                .isEqualTo("123456789");
    }

    @Test
    void extractEmail_kakao_readsNestedKakaoAccountEmail() {
        Map<String, Object> attributes =
                Map.of("id", 123456789L, "kakao_account", Map.of("email", "k@example.com"));

        assertThat(NormalizingOAuth2UserService.extractEmail(SocialProvider.KAKAO, attributes))
                .isEqualTo("k@example.com");
    }

    @Test
    void extractProviderUserId_naver_readsNestedResponseId() {
        Map<String, Object> attributes = Map.of("response", Map.of("id", "naver-1", "email", "n@example.com"));

        assertThat(NormalizingOAuth2UserService.extractProviderUserId(SocialProvider.NAVER, attributes))
                .isEqualTo("naver-1");
    }

    @Test
    void extractEmail_naver_readsNestedResponseEmail() {
        Map<String, Object> attributes = Map.of("response", Map.of("id", "naver-1", "email", "n@example.com"));

        assertThat(NormalizingOAuth2UserService.extractEmail(SocialProvider.NAVER, attributes))
                .isEqualTo("n@example.com");
    }
}
