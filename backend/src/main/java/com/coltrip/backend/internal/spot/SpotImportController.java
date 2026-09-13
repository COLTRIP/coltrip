package com.coltrip.backend.internal.spot;

import com.coltrip.backend.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "내부 (AI 관광지 적재)")
@RestController
@RequestMapping("/api/internal/spots")
@RequiredArgsConstructor
public class SpotImportController {
    private final SpotImportService service;

    @Operation(summary = "관광지 기본정보 및 감성모드 적재", description = """
            AI팀 협의용 push 계약. X-Internal-Api-Key 인증. 최대 100개 원자적 배치.
            tourApiContentId는 실제 TourAPI 숫자 문자열이며 기존 장소 ID와 고요지수는 보존합니다.
            modes는 전체 교체 목록: 중복 제거, []는 모두 해제, null/누락은 오류입니다.
            선택 필드 description/imageUrl/recommendReason은 null/누락 시 기존 값을 비웁니다.
            sourceUpdatedAt이 저장값보다 오래된 항목은 IGNORED_STALE, 같거나 최신이면 정정 적용합니다.
            """)
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<SpotImportResponse> receive(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @Valid @RequestBody SpotImportRequest request) {
        return ResponseEntity.ok(service.receive(apiKey, request));
    }

    @ExceptionHandler(InvalidSpotImportException.class)
    public ResponseEntity<ErrorResponse> invalid(InvalidSpotImportException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("InvalidSpotImport", exception.getMessage()));
    }
}
