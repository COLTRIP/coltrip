package com.coltrip.backend.visit.controller;

import com.coltrip.backend.visit.dto.VisitCompleteRequest;
import com.coltrip.backend.visit.dto.VisitCompleteResponse;
import com.coltrip.backend.visit.dto.VisitHistoryResponse;
import com.coltrip.backend.visit.dto.VisitStartRequest;
import com.coltrip.backend.visit.dto.VisitStartResponse;
import com.coltrip.backend.visit.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.coltrip.backend.visit.dto.CurrentVisitResponse;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "방문", description = "방문 시작 / 완료 / 이력 조회")
@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @Operation(summary = "현재 방문 조회", description = "현재 로그인한 사용자의 진행 중인 방문을 조회합니다. 방문 중인 장소가 없으면 visit: null을 반환합니다.")
    @GetMapping("/current")
    public ResponseEntity<CurrentVisitResponse> getCurrent(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(visitService.getCurrent(userId));
    }

    @Operation(summary = "방문 완료 이력 조회",
            description = "본인이 COMPLETED한 방문을 완료순(최신 먼저)으로 조회합니다. 마이페이지 '다녀온 곳' 목록용. reviewId가 null이면 아직 리뷰를 작성하지 않은 방문입니다.")
    @GetMapping("/history")
    public ResponseEntity<VisitHistoryResponse> getHistory(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(visitService.getHistory(userId));
    }

    @Operation(summary = "방문 시작", description = "'조용한 여행 시작하기' 버튼. 진행 중인 방문이 있으면 409.")
    @PostMapping("/start")
    public ResponseEntity<VisitStartResponse> start(@AuthenticationPrincipal Long userId,
                                                      @Valid @RequestBody VisitStartRequest request) {
        return ResponseEntity.ok(visitService.start(userId, request));
    }

    @Operation(summary = "방문 완료", description = "목적지 반경(점형 100m/면적형 250m) 진입 + 체류시간 600초 이상이어야 완료됩니다. 미충족 시 400.")
    @PatchMapping("/{visitId}/complete")
    public ResponseEntity<VisitCompleteResponse> complete(@AuthenticationPrincipal Long userId,
                                                            @PathVariable Long visitId,
                                                            @Valid @RequestBody VisitCompleteRequest request) {
        return ResponseEntity.ok(visitService.complete(userId, visitId, request));
    }
}
