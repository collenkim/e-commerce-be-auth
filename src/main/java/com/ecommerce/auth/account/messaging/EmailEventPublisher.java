package com.ecommerce.auth.account.messaging;

import com.ecommerce.auth.account.event.EmailVerificationRequested;
import com.ecommerce.auth.account.event.PasswordResetRequested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 이메일 발송 이벤트를 RabbitMQ({@code email.events} topic exchange)에 발행한다.
 * 발행 실패는 non-blocking으로 처리한다 — 로깅만 하고 예외를 전파하지 않는다
 * (nfr-requirements.md: 이메일 발송은 핵심 트랜잭션이 아님, SECURITY-15 fail-closed의 명시적 예외).
 * {@code AmqpException}뿐 아니라 메시지 변환 오류 등 예기치 못한 실패까지 폭넓게 흡수한다 —
 * 이 규칙의 의도는 "발행과 관련된 어떤 실패도 가입/재설정 자체를 막지 않는다"이다.
 */
@Component
public class EmailEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmailEventPublisher.class);
    private static final String EXCHANGE = "email.events";

    private final RabbitTemplate rabbitTemplate;

    public EmailEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(EmailVerificationRequested event) {
        publish("email.verification.requested", event);
    }

    public void publish(PasswordResetRequested event) {
        publish("email.password-reset.requested", event);
    }

    private void publish(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
        } catch (RuntimeException ex) {
            log.warn("Failed to publish email event (routingKey={}), continuing without it", routingKey, ex);
        }
    }
}
