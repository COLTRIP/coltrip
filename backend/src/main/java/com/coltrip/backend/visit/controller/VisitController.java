package com.coltrip.backend.visit.controller;

import com.coltrip.backend.visit.dto.VisitCompleteRequest;
import com.coltrip.backend.visit.dto.VisitCompleteResponse;
import com.coltrip.backend.visit.dto.VisitStartRequest;
import com.coltrip.backend.visit.dto.VisitStartResponse;
import com.coltrip.backend.visit.service.VisitService;
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

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @PostMapping("/start")
    public ResponseEntity<VisitStartResponse> start(@AuthenticationPrincipal Long userId,
                                                      @Valid @RequestBody VisitStartRequest request) {
        return ResponseEntity.ok(visitService.start(userId, request));
    }

    @PatchMapping("/{visitId}/complete")
    public ResponseEntity<VisitCompleteResponse> complete(@AuthenticationPrincipal Long userId,
                                                            @PathVariable Long visitId,
                                                            @Valid @RequestBody VisitCompleteRequest request) {
        return ResponseEntity.ok(visitService.complete(userId, visitId, request));
    }
}
