package com.coltrip.backend.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

// Spring Security 필터 체인 안에서 던져진, GlobalExceptionHandler로는 절대 안 잡히는 예외가
// 로그 한 줄 없이 사라지던 사각지대(2026-09-16, /api/auth/google 간헐적 500 원인 미상)를 막기 위한 필터.
class UnhandledExceptionLoggingFilterTest {

    private final UnhandledExceptionLoggingFilter filter = new UnhandledExceptionLoggingFilter();
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(UnhandledExceptionLoggingFilter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void rethrowsAndLogsWithMethodAndUriWhenChainThrows() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/google");
        RuntimeException failure = new IllegalStateException("boom");
        org.mockito.Mockito.doThrow(failure).when(chain).doFilter(request, response);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> filter.doFilterInternal(request, response, chain));

        assertSame(failure, thrown);
        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.getFirst();
        assertEquals(Level.ERROR, event.getLevel());
        assertTrue(event.getFormattedMessage().contains("POST"));
        assertTrue(event.getFormattedMessage().contains("/api/auth/google"));
        assertEquals(failure.getClass().getName(), event.getThrowableProxy().getClassName());
    }

    @Test
    void passesThroughSilentlyWhenChainSucceeds() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(0, appender.list.size());
    }
}
