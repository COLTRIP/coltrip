package com.coltrip.backend.demo;

import com.coltrip.backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoAccessPolicy {
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
                .map(user -> properties.allowedGoogleSubs().contains(user.getGoogleSub())).orElse(false));
    }
}
