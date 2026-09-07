package com.coltrip.backend.internal.controller;

import com.coltrip.backend.internal.dto.QuietIndexPushRequest;
import com.coltrip.backend.internal.dto.QuietIndexPushResponse;
import com.coltrip.backend.internal.service.InternalQuietIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI 배치 서버 전용. JWT 대신 X-Internal-Api-Key 헤더로 인증
@Tag(name = "내부 (AI 연동)", description = "AI 배치 서버 전용. 프론트에서는 사용하지 않습니다.")
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalQuietIndexController {

    private final InternalQuietIndexService internalQuietIndexService;

    @Operation(summary = "고요지수 push (AI → 백엔드)",
            description = """
                    **JWT가 아니라 `X-Internal-Api-Key` 헤더로 인증합니다.** (우측 상단 Authorize와 무관)

                    AI가 배치(1시간 주기)로 계산한 quietScore를 전달합니다.
                    `quiet_index` 이력에 저장하고 `tourist_spot`의 캐시 값을 갱신합니다.
                    스팟 식별은 내부 ID가 아니라 TourAPI 원본 `tourApiContentId`로 합니다.
                    """)
    @SecurityRequirements
    @PostMapping("/quiet-index")
    public ResponseEntity<QuietIndexPushResponse> pushQuietIndex(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @Valid @RequestBody QuietIndexPushRequest request) {
        return ResponseEntity.ok(internalQuietIndexService.push(apiKey, request));
    }
}
