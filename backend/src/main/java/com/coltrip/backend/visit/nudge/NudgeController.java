package com.coltrip.backend.visit.nudge;

import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.common.exception.ErrorResponse;
import com.coltrip.backend.visit.dto.VisitStartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "방문 중 대체지 제안")
@RestController
@RequestMapping("/api/visits/{visitId}/alternative-suggestion")
@RequiredArgsConstructor
public class NudgeController {
    private final NudgeService service;
    private final NudgeStore store;

    @Operation(summary = "방문 중 고요지수 평가", description = """
            STARTED 방문에서 현재 40점 미만 또는 유효한 시작 점수 대비 15점 이상 하락하면 평가합니다.
            방문 화면 진입/복귀 및 60초 간격의 순차 호출용. 푸시가 아닙니다.
            OFFERED 응답은 같은 suggestionId로 재전송될 수 있으므로 프론트에서 중복 표시를 막습니다.
            추천 없음은 200 NO_ALTERNATIVES, AI 오류는 502/504 등이며 방문 완료와 무관합니다.
            """)
    @PostMapping("/check")
    public ResponseEntity<NudgeResponse> check(@AuthenticationPrincipal Long userId, @PathVariable Long visitId) {
        return ok(service.check(userId, visitId));
    }

    @Operation(summary = "이 방문의 대체지 제안 닫기", description = "이후 해당 방문에는 다시 제안하지 않습니다.")
    @PostMapping("/dismiss")
    public ResponseEntity<NudgeResponse> dismiss(@AuthenticationPrincipal Long userId, @PathVariable Long visitId) {
        return ok(store.dismiss(userId, visitId));
    }

    @Operation(summary = "제안된 대체지로 방문 전환", description = """
            유효한 제안에 포함된 장소만 선택할 수 있습니다. 현재 방문 취소와 새 방문 시작은 원자적입니다.
            동일 방문에서 동일 목적지로 재요청하면 이미 생성한 방문을 반환합니다.
            만료/닫은 제안, 알림 거부, 완료 방문, 목록 외 장소 선택은 409입니다.
            """)
    @PostMapping("/select")
    public ResponseEntity<VisitStartResponse> select(@AuthenticationPrincipal Long userId,
            @PathVariable Long visitId, @Valid @RequestBody NudgeSelectRequest request) {
        return ok(store.select(userId, visitId, request));
    }

    @ExceptionHandler(AiIntegrationException.class)
    public ResponseEntity<ErrorResponse> aiError(AiIntegrationException exception) {
        return ResponseEntity.status(exception.getStatus()).cacheControl(CacheControl.noStore())
                .body(new ErrorResponse(exception.getCode(), exception.getMessage()));
    }

    private <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
