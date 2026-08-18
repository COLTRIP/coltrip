package com.coltrip.backend.internal.controller;

import com.coltrip.backend.internal.dto.QuietIndexPushRequest;
import com.coltrip.backend.internal.dto.QuietIndexPushResponse;
import com.coltrip.backend.internal.service.InternalQuietIndexService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// AI 배치 서버 전용. JWT 대신 X-Internal-Api-Key 헤더로 인증
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalQuietIndexController {

    private final InternalQuietIndexService internalQuietIndexService;

    @PostMapping("/quiet-index")
    public ResponseEntity<QuietIndexPushResponse> pushQuietIndex(
            @RequestHeader("X-Internal-Api-Key") String apiKey,
            @Valid @RequestBody QuietIndexPushRequest request) {
        return ResponseEntity.ok(internalQuietIndexService.push(apiKey, request));
    }
}
