package com.ecommerce.auth.shared;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 테스트에서 시간을 고정할 수 있도록 시스템 시계를 Bean으로 노출한다. */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
