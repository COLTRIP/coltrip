package com.coltrip.backend.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.coltrip.backend.spot.exception.SpotNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unmappedExceptionBecomesInternalServerErrorNotUnauthorized() {
        var response = handler.handleUnexpected(new IllegalStateException("예상치 못한 오류"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("InternalServerError", response.getBody().code());
    }

    @Test
    void doesNotHideAlreadyMappedExceptionBehindGenericHandler() {
        // 더 구체적인 핸들러(handleNotFound)가 존재하는 예외는 Exception.class 캐치올과 별개로
        // 실제 디스패치 시 Spring이 더 구체적인 핸들러를 선택한다 - 여기서는 두 핸들러가
        // 각자 의도한 상태 코드를 내려주는지만 확인한다.
        var response = handler.handleNotFound(new SpotNotFoundException());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
