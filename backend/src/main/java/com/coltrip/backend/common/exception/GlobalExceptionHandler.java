package com.coltrip.backend.common.exception;

import com.coltrip.backend.auth.exception.AlreadyRegisteredUserException;
import com.coltrip.backend.auth.exception.InvalidGoogleTokenException;
import com.coltrip.backend.auth.exception.InvalidRefreshTokenException;
import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.auth.exception.UserNotRegisteredException;
import com.coltrip.backend.internal.exception.InvalidInternalApiKeyException;
import com.coltrip.backend.internal.exception.InvalidObservationTimeException;
import com.coltrip.backend.review.exception.ReviewNotAllowedException;
import com.coltrip.backend.review.exception.ReviewNotFoundException;
import com.coltrip.backend.spot.exception.InvalidBoundingBoxException;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import com.coltrip.backend.spot.exception.InvalidSearchRequestException;
import com.coltrip.backend.visit.exception.AlreadyOngoingVisitException;
import com.coltrip.backend.visit.exception.InvalidVisitStateException;
import com.coltrip.backend.visit.exception.VisitNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({InvalidGoogleTokenException.class, InvalidRefreshTokenException.class,
            UnauthorizedException.class, InvalidInternalApiKeyException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(e));
    }

    @ExceptionHandler({SpotNotFoundException.class, VisitNotFoundException.class, ReviewNotFoundException.class,
            UserNotRegisteredException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(e));
    }

    @ExceptionHandler({AlreadyOngoingVisitException.class, InvalidVisitStateException.class,
            ReviewNotAllowedException.class, AlreadyRegisteredUserException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(e));
    }

    @ExceptionHandler({InvalidBoundingBoxException.class,
            InvalidObservationTimeException.class,
            InvalidSearchRequestException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.of(e));
    }

    // 아래 3개를 명시적으로 처리하지 않으면 Spring이 /error로 내부 포워딩하는데,
    // 그 시점엔 SecurityContext가 비어 있어 인증 실패(401)로 잘못 응답된다.
    // 요청 형식 오류이므로 400으로 내려준다.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "'%s' 파라미터 값이 올바르지 않습니다: %s".formatted(e.getName(), e.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("InvalidParameterException", message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = "필수 파라미터가 없습니다: %s".formatted(e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("MissingParameterException", message));
    }

    // 요청 바디 자체를 못 읽는 경우 (JSON 문법 오류, enum 값 오타 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("InvalidRequestBodyException", "요청 본문을 해석할 수 없습니다. 필드 형식과 enum 값을 확인해주세요."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailed(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> "%s: %s".formatted(error.getField(), error.getDefaultMessage()))
                .findFirst()
                .orElse("요청 값이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("ValidationException", message));
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class,
            NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleProtocolError(Exception exception) {
        var error = (org.springframework.web.ErrorResponse) exception;
        String message = switch (error.getStatusCode().value()) {
            case 405 -> "지원하지 않는 HTTP 메서드입니다.";
            case 415 -> "지원하지 않는 요청 Content-Type입니다.";
            case 406 -> "요청한 Accept 형식으로 응답할 수 없습니다.";
            default -> "요청한 경로를 찾을 수 없습니다.";
        };
        // Allow/Accept 등 Spring이 제공한 헤더를 보존한다. 오류 본문은 Accept와 무관하게 JSON이다.
        return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponse(exception.getClass().getSimpleName(), message));
    }

    // 위에서 처리되지 않은 예외를 그대로 두면 Spring이 /error로 내부 포워딩하는데,
    // 그 시점엔 SecurityContext가 비어 있어 실제로는 500인 오류가 401로 잘못 응답된다(2026-09-13 실사고).
    // 진짜 원인을 알 수 있도록 서버 로그에 스택트레이스를 남기고 500으로 명확히 응답한다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외가 발생했습니다.", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("InternalServerError", "서버 내부 오류가 발생했습니다."));
    }
}
