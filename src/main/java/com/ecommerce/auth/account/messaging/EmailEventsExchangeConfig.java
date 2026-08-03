package com.ecommerce.auth.account.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code email.events} topic exchange 선언. Consumer(큐/바인딩)는 이 프로젝트 범위 밖이므로
 * 큐는 생성하지 않는다 — 발행 채널만 마련한다 (Application Design Q5:B, shared-infrastructure.md).
 *
 * <p>{@link JacksonJsonMessageConverter}를 사용한다 — 기본 {@code SimpleMessageConverter}는
 * {@code Serializable}만 지원하는데 이벤트 레코드가 이를 구현하지 않아 발행이 실패했었다(실제
 * 통합 테스트로 발견된 버그). JSON 직렬화가 향후 Consumer와의 상호운용성 측면에서도 더 적절하다.
 */
@Configuration
public class EmailEventsExchangeConfig {

    @Bean
    public TopicExchange emailEventsExchange() {
        return new TopicExchange("email.events");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
