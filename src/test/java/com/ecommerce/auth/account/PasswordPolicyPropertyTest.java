package com.ecommerce.auth.account;

import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * PBT-03 (Invariant Properties, Partial 모드 강제 대상): 비즈니스 규칙 불변식 —
 * "정책을 통과하는 비밀번호는 항상 8자 이상이며 문자와 숫자를 모두 포함한다"와
 * "8자 미만 비밀번호는 어떤 내용이든 항상 거부된다"를 검증한다. 도메인 전용 생성기 사용(PBT-07).
 */
class PasswordPolicyPropertyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Property
    void anyPasswordMeetingConstraints_isAlwaysValid(@ForAll("validShapedPasswords") String password) {
        assertThat(policy.isValid(password)).isTrue();
    }

    @Property
    void anyPasswordShorterThanMinLength_isAlwaysInvalid(@ForAll("tooShortPasswords") String password) {
        assertThat(policy.isValid(password)).isFalse();
    }

    @Provide
    Arbitrary<String> validShapedPasswords() {
        Arbitrary<String> letters = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(4).ofMaxLength(20);
        Arbitrary<String> digits = Arbitraries.strings().numeric().ofMinLength(4).ofMaxLength(20);
        return Combinators.combine(letters, digits).as((l, d) -> l + d);
    }

    @Provide
    Arbitrary<String> tooShortPasswords() {
        return Arbitraries.strings().ofMinLength(0).ofMaxLength(7);
    }
}
