package com.coltrip.backend.common.exception;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.hamcrest.Matchers.containsString;

import com.coltrip.backend.auth.dto.JwtTokenResponse;
import com.coltrip.backend.auth.jwt.JwtProvider;
import com.coltrip.backend.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class ProtocolErrorIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired JwtProvider jwt;
    @MockitoBean AuthService auth;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void wrongMethodReturns405AndAllowHeader() throws Exception {
        mvc.perform(get("/api/auth/refresh"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("HttpRequestMethodNotSupportedException"))
                .andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(auth);
    }

    @Test
    void unsupportedBodyTypeReturns415() throws Exception {
        mvc.perform(post("/api/auth/google").contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("Accept", containsString("application/json")))
                .andExpect(jsonPath("$.code").value("HttpMediaTypeNotSupportedException"))
                .andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(auth);
    }

    @Test
    void unsupportedAcceptReturns406WithJsonError() throws Exception {
        when(auth.refresh("test-refresh")).thenReturn(JwtTokenResponse.of("access", "refresh", false, null));
        mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer test-refresh")
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("HttpMediaTypeNotAcceptableException"));
    }

    @Test
    void unknownPublicPathReturns404() throws Exception {
        mvc.perform(get("/api/spots/not/a/real/endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NoResourceFoundException"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void authenticatedUnknownPathReturns404() throws Exception {
        mvc.perform(get("/api/missing-endpoint").header("Authorization", "Bearer " + jwt.createAccessToken(1L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NoResourceFoundException"));
    }

    @Test
    void protectedPathStillRequiresAuthenticationBeforeRouting() throws Exception {
        mvc.perform(get("/api/missing-endpoint"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users/me").contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedWrongMethodReturns405() throws Exception {
        mvc.perform(post("/api/users/me").header("Authorization", "Bearer " + jwt.createAccessToken(1L)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.code").value("HttpRequestMethodNotSupportedException"));
    }

    @Test
    void malformedJsonStillReturns400() throws Exception {
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("InvalidRequestBodyException"));
    }

    @Test
    void internalFailuresRemain500WithoutLeakingDetails() throws Exception {
        when(auth.refresh("test-refresh")).thenThrow(new IllegalStateException("internal-test-detail"));
        mvc.perform(post("/api/auth/refresh").header("Authorization", "Bearer test-refresh"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("InternalServerError"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("internal-test-detail"))));
    }
}
