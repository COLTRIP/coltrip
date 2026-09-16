package com.coltrip.backend.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

// GlobalExceptionHandler(@RestControllerAdvice)는 DispatcherServlet 이후 단계만 잡는다.
// Spring Security 필터 체인(JwtAuthenticationFilter, DemoAccessFilter 등) 안에서 던져진 예외는
// 그 앞에서 걸러지지 않으면 로그 한 줄 없이 500만 내려가는 사각지대가 있었다(2026-09-16 실사고,
// /api/auth/google 간헐적 500 - 원인 특정 불가). 이 필터를 스프링 시큐리티보다 바깥쪽(가장 먼저)에
// 등록해 그 사각지대까지 포함한 전체 체인을 감싸 어디서 터지든 스택트레이스를 남긴다.
public class UnhandledExceptionLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UnhandledExceptionLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (Throwable t) {
            log.error("필터 체인에서 처리되지 않은 예외: {} {}", request.getMethod(), request.getRequestURI(), t);
            throw t;
        }
    }
}
