package com.coltrip.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("coltrip API")
                        .version("v1")
                        .description("""
                                부산 저밀도 정적 관광 추천 서비스 API

                                **인증 방법**
                                1. `POST /api/auth/refresh`에 리프레시 토큰을 넣어 accessToken을 발급받습니다.
                                2. 우측 상단 **Authorize** 버튼에 accessToken을 붙여넣습니다. (`Bearer ` 접두어 없이 토큰만)
                                3. 이후 자물쇠 표시가 있는 API를 호출할 수 있습니다.

                                관광지 조회(GET)는 인증 없이 호출 가능합니다.
                                """))
                // Authorize에 한 번 넣으면 모든 요청에 Authorization 헤더가 붙는다
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME))
                .components(new Components().addSecuritySchemes(JWT_SCHEME,
                        new SecurityScheme()
                                .name(JWT_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("accessToken 값만 입력 (Bearer 접두어 불필요)")));
    }
}
