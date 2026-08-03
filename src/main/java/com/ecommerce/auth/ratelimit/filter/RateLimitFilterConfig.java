package com.ecommerce.auth.ratelimit.filter;

import com.ecommerce.auth.ratelimit.RateLimitService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** {@link RateLimitFilter}를 signup/login 경로에만 등록한다 (SecurityFilterChain과 독립적). */
@Configuration
public class RateLimitFilterConfig {

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitService rateLimitService) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(rateLimitService));
        registration.addUrlPatterns("/api/auth/signup", "/api/auth/login");
        registration.setName("rateLimitFilter");
        return registration;
    }
}
