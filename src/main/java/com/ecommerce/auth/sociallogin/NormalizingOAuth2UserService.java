package com.ecommerce.auth.sociallogin;

import com.ecommerce.auth.sociallogin.domain.SocialProvider;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * provider별 원시 사용자 정보 응답에서 {@code providerUserId}/{@code email}만 추출해 정규화한다
 * (수집 정보 최소화 원칙, business-rules.md). 실제 HTTP 호출은 {@link DefaultOAuth2UserService}에
 * 위임하고, attribute 파싱만 이 클래스가 담당한다 — 파싱 로직은 순수 함수로 분리해 단위 테스트 가능하게 한다.
 */
@Service
public class NormalizingOAuth2UserService extends DefaultOAuth2UserService {

    public static final String PROVIDER_USER_ID_KEY = "providerUserId";
    public static final String EMAIL_KEY = "email";

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User rawUser = super.loadUser(userRequest);
        SocialProvider provider =
                SocialProvider.fromRegistrationId(userRequest.getClientRegistration().getRegistrationId());

        String providerUserId = extractProviderUserId(provider, rawUser.getAttributes());
        String email = extractEmail(provider, rawUser.getAttributes());

        return new DefaultOAuth2User(
                rawUser.getAuthorities(),
                Map.of(PROVIDER_USER_ID_KEY, providerUserId, EMAIL_KEY, email),
                PROVIDER_USER_ID_KEY);
    }

    /** provider별 사용자 ID 필드 위치가 달라 여기서 흡수한다. 순수 함수 — 테스트 용이. */
    @SuppressWarnings("unchecked")
    public static String extractProviderUserId(SocialProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> String.valueOf(attributes.get("sub"));
            case KAKAO -> String.valueOf(attributes.get("id"));
            case NAVER -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                yield String.valueOf(response.get("id"));
            }
        };
    }

    /** provider별 이메일 필드 위치가 달라 여기서 흡수한다. 순수 함수 — 테스트 용이. */
    @SuppressWarnings("unchecked")
    public static String extractEmail(SocialProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("email");
            case KAKAO -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                yield (String) kakaoAccount.get("email");
            }
            case NAVER -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                yield (String) response.get("email");
            }
        };
    }
}
