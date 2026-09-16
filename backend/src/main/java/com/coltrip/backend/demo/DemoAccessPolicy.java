package com.coltrip.backend.demo;

import com.coltrip.backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoAccessPolicy {
    // 구글 인증 없이 즉시 발급되는 시연 게스트 계정의 googleSub 접두사. DemoGuestAuthService와 공유한다.
    public static final String GUEST_SUB_PREFIX = "demo-guest-";

    private final DemoProperties properties;
    private final UserRepository users;

    public boolean enabled() { return properties.enabled(); }

    public void requireAllowedSubject(String sub) {
        if (enabled() && (sub == null || !properties.allowedGoogleSubs().contains(sub))) {
            throw new DemoAccessDeniedException();
        }
    }

    public boolean allowsUser(Long id) {
        return !enabled() || (id != null && users.findById(id)
                .map(user -> isGuest(user.getGoogleSub()) || properties.allowedGoogleSubs().contains(user.getGoogleSub()))
                .orElse(false));
    }

    public static boolean isGuest(String googleSub) {
        return googleSub != null && googleSub.startsWith(GUEST_SUB_PREFIX);
    }
}
