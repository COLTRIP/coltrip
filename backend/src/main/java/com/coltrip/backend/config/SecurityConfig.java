package com.coltrip.backend.config;

import com.coltrip.backend.auth.jwt.JwtAuthenticationFilter;
import com.coltrip.backend.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, GoogleOAuthProperties.class, InternalApiProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/google", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/internal/**").permitAll()
                        // Swagger UI (개발용 API 문서)
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // 관광지 조회(목록/상세/리뷰목록)는 비로그인 허용 - 지도 둘러보기는 로그인 없이 가능
                        // 좋아요/리뷰 작성 등 쓰기 작업은 아래 authenticated()에 걸림
                        .requestMatchers(HttpMethod.GET, "/api/spots/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
