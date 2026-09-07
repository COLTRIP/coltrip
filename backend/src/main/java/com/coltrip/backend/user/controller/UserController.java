package com.coltrip.backend.user.controller;

import com.coltrip.backend.auth.dto.UserResponse;
import com.coltrip.backend.user.dto.UpdateNicknameRequest;
import com.coltrip.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "내 정보 조회 / 닉네임 설정 / 회원 탈퇴")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @Operation(summary = "닉네임 설정/수정", description = "로그인 직후 필수 설정과 마이페이지 수정에 공용. 1~20자, 중복 허용.")
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateNickname(@AuthenticationPrincipal Long userId,
                                                         @Valid @RequestBody UpdateNicknameRequest request) {
        return ResponseEntity.ok(userService.updateNickname(userId, request.nickname()));
    }

    @Operation(summary = "회원 탈퇴", description = "⚠️ 하드 삭제입니다. 계정과 방문/좋아요/리뷰가 모두 즉시 삭제되며 복구할 수 없습니다.")
    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMe(@AuthenticationPrincipal Long userId) {
        userService.deleteMe(userId);
        return ResponseEntity.ok("회원 탈퇴 완료");
    }
}
