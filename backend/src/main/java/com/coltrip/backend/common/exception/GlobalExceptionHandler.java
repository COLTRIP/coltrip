package com.coltrip.backend.common.exception;

import com.coltrip.backend.auth.exception.AlreadyRegisteredUserException;
import com.coltrip.backend.auth.exception.InvalidGoogleTokenException;
import com.coltrip.backend.auth.exception.InvalidRefreshTokenException;
import com.coltrip.backend.auth.exception.UnauthorizedException;
import com.coltrip.backend.auth.exception.UserNotRegisteredException;
import com.coltrip.backend.internal.exception.InvalidInternalApiKeyException;
import com.coltrip.backend.review.exception.ReviewNotAllowedException;
import com.coltrip.backend.review.exception.ReviewNotFoundException;
import com.coltrip.backend.spot.exception.InvalidBoundingBoxException;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import com.coltrip.backend.visit.exception.AlreadyOngoingVisitException;
import com.coltrip.backend.visit.exception.InvalidVisitStateException;
import com.coltrip.backend.visit.exception.VisitConditionNotMetException;
import com.coltrip.backend.visit.exception.VisitNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler({VisitConditionNotMetException.class, InvalidBoundingBoxException.class})
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
}
