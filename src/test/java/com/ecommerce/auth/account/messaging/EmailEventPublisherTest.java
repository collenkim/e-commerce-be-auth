package com.ecommerce.auth.account.messaging;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ecommerce.auth.account.event.EmailVerificationRequested;
import com.ecommerce.auth.account.event.PasswordResetRequested;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Account Unit Code Generation 당시 놓쳤던 버그(레코드가 Serializable을 구현하지 않아
 * 기본 SimpleMessageConverter로는 발행 자체가 실패하던 문제, Authorization Unit의 통합 테스트로
 * 발견)에 대한 회귀 테스트를 포함한다.
 */
class EmailEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final EmailEventPublisher publisher = new EmailEventPublisher(rabbitTemplate);

    @Test
    void publish_verificationEvent_sendsToCorrectRoutingKey() {
        var event = new EmailVerificationRequested(UUID.randomUUID(), "user@example.com", "token");

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend("email.events", "email.verification.requested", event);
    }

    @Test
    void publish_passwordResetEvent_sendsToCorrectRoutingKey() {
        var event = new PasswordResetRequested(UUID.randomUUID(), "user@example.com", "token");

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend("email.events", "email.password-reset.requested", event);
    }

    @Test
    void publish_anyRuntimeExceptionFromRabbit_isSwallowed() {
        doThrow(new IllegalArgumentException("SimpleMessageConverter only supports Serializable"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(
                        () ->
                                publisher.publish(
                                        new EmailVerificationRequested(UUID.randomUUID(), "user@example.com", "t")))
                .doesNotThrowAnyException();
    }
}
