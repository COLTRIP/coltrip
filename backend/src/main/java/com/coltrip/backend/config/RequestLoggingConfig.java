package com.coltrip.backend.config;

import com.coltrip.backend.common.logging.UnhandledExceptionLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class RequestLoggingConfig {

    // Spring Security의 필터 체인(springSecurityFilterChain)보다 먼저 실행되도록 최우선 순위로 등록한다.
    // SecurityConfig 안의 addFilterBefore/After는 시큐리티 체인 "내부" 순서일 뿐이라, 그 체인 자체를
    // 감싸려면 서블릿 컨테이너 레벨의 별도 필터로 최우선 등록해야 한다.
    @Bean
    public FilterRegistrationBean<UnhandledExceptionLoggingFilter> unhandledExceptionLoggingFilter() {
        FilterRegistrationBean<UnhandledExceptionLoggingFilter> registration =
                new FilterRegistrationBean<>(new UnhandledExceptionLoggingFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
