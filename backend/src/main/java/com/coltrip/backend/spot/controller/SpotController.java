package com.coltrip.backend.spot.controller;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.Mode;
import com.coltrip.backend.spot.dto.SpotDetailResponse;
import com.coltrip.backend.spot.dto.SpotListResponse;
import com.coltrip.backend.spot.service.SpotService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;

    @GetMapping
    public ResponseEntity<SpotListResponse> findInBounds(@RequestParam BigDecimal swLat,
                                                           @RequestParam BigDecimal swLng,
                                                           @RequestParam BigDecimal neLat,
                                                           @RequestParam BigDecimal neLng,
                                                           @RequestParam(required = false) Category category,
                                                           @RequestParam(required = false) Mode mode) {
        return ResponseEntity.ok(spotService.findInBounds(swLat, swLng, neLat, neLng, category, mode));
    }

    @GetMapping("/{spotId}")
    public ResponseEntity<SpotDetailResponse> findById(@PathVariable Long spotId) {
        return ResponseEntity.ok(spotService.findById(spotId));
    }
}
