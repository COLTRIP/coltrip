package com.coltrip.backend.auth.controller;

import com.coltrip.backend.auth.dto.GoogleLoginRequest;
import com.coltrip.backend.auth.dto.JwtTokenResponse;
import com.coltrip.backend.auth.exception.InvalidRefreshTokenException;
import com.coltrip.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "구글 로그인 / 토큰 재발급 / 로그아웃")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "구글 로그인 / 회원가입",
            description = """
                    프론트에서 구글 SDK로 받은 **idToken**을 전달합니다. (accessToken 아님)

                    `intent`로 화면을 구분합니다.
                    - `LOGIN`: 로그인 화면에서 호출
                    - `SIGNUP`: 회원가입 화면에서 호출

                    최초 가입 시 `nickname`은 `null`이므로, 프론트는 `nickname == null`이면
                    닉네임 설정 화면으로 이동시켜야 합니다.
                    """)
    @SecurityRequirements
    @PostMapping("/google")
    public ResponseEntity<JwtTokenResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request.idToken(), request.intent()));
    }

    @Operation(summary = "액세스 토큰 재발급",
            description = """
                    **Authorization 헤더에 리프레시 토큰**을 담아 호출합니다. (액세스 토큰 아님)

                    ⚠️ 리프레시 토큰은 1회용입니다. 호출하면 새 리프레시 토큰이 함께 발급되고
                    이전 토큰은 즉시 무효화되므로, 응답의 `refreshToken`을 반드시 저장해 다음 재발급에 사용하세요.

                    Swagger에서 테스트하려면 우측 상단 Authorize에 **리프레시 토큰**을 넣고 호출한 뒤,
                    응답으로 받은 accessToken으로 다시 Authorize 하세요.
                    """)
    @PostMapping("/refresh")
    public ResponseEntity<JwtTokenResponse> refresh(@RequestHeader("Authorization") String authorizationHeader) {
        String refreshToken = extractToken(authorizationHeader);
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @Operation(summary = "로그아웃", description = "서버에 저장된 리프레시 토큰을 무효화합니다.")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);
        return ResponseEntity.ok("로그아웃 성공");
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new InvalidRefreshTokenException();
        }
        return authorizationHeader.substring(7);
    }
}
