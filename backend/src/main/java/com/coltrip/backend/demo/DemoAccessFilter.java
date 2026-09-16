package com.coltrip.backend.demo;

import com.coltrip.backend.common.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

public class DemoAccessFilter extends OncePerRequestFilter {
    private final DemoAccessPolicy policy;
    private final ObjectMapper mapper = new ObjectMapper();

    public DemoAccessFilter(DemoAccessPolicy policy) { this.policy = policy; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!policy.enabled()) {
            chain.doFilter(request, response);
            return;
        }
        String path = request.getServletPath();
        if (path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
        // 로그인/재발급은 검증된 Google sub로 서비스 내부에서 허용 여부를 판정한다.
        // 게스트 세션 발급은 인증 자체가 없는 진입점이라 필터 통과 후 서비스에서 새 계정을 만든다.
        if ("POST".equals(request.getMethod())
                && ("/api/auth/google".equals(path) || "/api/auth/refresh".equals(path)
                    || "/api/demo/guest-session".equals(path))) {
            chain.doFilter(request, response);
            return;
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long id ? id : null;
        if (!path.startsWith("/api/internal/") && !"/api/internal".equals(path) && policy.allowsUser(userId)) {
            chain.doFilter(request, response);
            return;
        }
        boolean anonymous = userId == null && !path.startsWith("/api/internal");
        response.setStatus(anonymous ? 401 : 403);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(mapper.writeValueAsString(new ErrorResponse(
                anonymous ? "UnauthorizedException" : "DemoAccessDenied",
                anonymous ? "인증이 필요합니다." : "시연 환경 접근이 허용되지 않았습니다.")));
    }
}
