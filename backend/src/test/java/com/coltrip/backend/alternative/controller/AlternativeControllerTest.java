package com.coltrip.backend.alternative.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.coltrip.backend.alternative.dto.AlternativeListResponse;
import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.alternative.service.AlternativeService;
import com.coltrip.backend.common.exception.GlobalExceptionHandler;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AlternativeControllerTest {
    private AlternativeService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(AlternativeService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AlternativeController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void anonymousEmptyResultIsSuccessfulAndNotCacheable() throws Exception {
        when(service.find(null, 1L)).thenReturn(new AlternativeListResponse(false, 80.5, List.of(), "No trigger"));
        mvc.perform(get("/api/spots/1/alternatives"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.triggered").value(false))
                .andExpect(jsonPath("$.targetQuietIndex").value(80.5))
                .andExpect(jsonPath("$.alternatives").isEmpty());
    }

    @Test
    void missingSpotReturns404() throws Exception {
        when(service.find(null, 999L)).thenThrow(new SpotNotFoundException());
        mvc.perform(get("/api/spots/999/alternatives")).andExpect(status().isNotFound());
    }

    @Test
    void aiErrorStatusesArePreserved() throws Exception {
        for (HttpStatus expected : List.of(HttpStatus.BAD_GATEWAY, HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.CONFLICT)) {
            reset(service);
            when(service.find(null, 1L)).thenThrow(new AiIntegrationException(expected, "AiError", "Unavailable"));
            mvc.perform(get("/api/spots/1/alternatives"))
                    .andExpect(status().is(expected.value()))
                    .andExpect(jsonPath("$.code").value("AiError"));
        }
    }
}
