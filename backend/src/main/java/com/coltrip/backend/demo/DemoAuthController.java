package com.coltrip.backend.demo;

import com.coltrip.backend.auth.dto.JwtTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "시연 게스트 로그인", description = "demo.enabled=true에서만 존재. 운영 환경에는 이 경로 자체가 없음")
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "demo.enabled", havingValue = "true")
public class DemoAuthController {

    private final DemoGuestAuthService service;

    @Operation(summary = "시연 게스트 세션 발급", description = """
            구글 로그인/회원가입 없이 호출 즉시 새 게스트 계정을 만들고 토큰을 발급합니다.
            사전에 계정을 등록받을 필요가 없으며, 호출마다 서로 다른 게스트 계정이 발급됩니다.
            demo 프로필이 아니면 이 엔드포인트 자체가 등록되지 않아 404입니다.
            """)
    @SecurityRequirements
    @PostMapping("/guest-session")
    public ResponseEntity<JwtTokenResponse> guestSession() {
        return ResponseEntity.ok(service.createGuestSession());
    }
}
