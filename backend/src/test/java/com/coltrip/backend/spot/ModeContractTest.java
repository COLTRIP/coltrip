package com.coltrip.backend.spot;

import com.coltrip.backend.common.exception.GlobalExceptionHandler;
import com.coltrip.backend.domain.spot.Mode;
import com.coltrip.backend.internal.spot.SpotImportController;
import com.coltrip.backend.internal.spot.SpotImportRequest;
import com.coltrip.backend.internal.spot.SpotImportResponse;
import com.coltrip.backend.internal.spot.SpotImportService;
import com.coltrip.backend.spot.controller.SpotController;
import com.coltrip.backend.spot.dto.SpotListResponse;
import com.coltrip.backend.spot.service.SpotService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ModeContractTest {
    @Test void labelsMatchTheEightEmotionCategories() {
        assertEquals(List.of("COZY", "NATURAL", "URBAN", "VINTAGE", "EXOTIC", "VIBRANT", "SENSORY", "TRANQUIL"),
                java.util.Arrays.stream(Mode.values()).map(Enum::name).toList());
        assertEquals(List.of("아늑", "자연", "도시", "빈티지", "이국", "활기", "감각", "고요"),
                java.util.Arrays.stream(Mode.values()).map(Mode::getLabel).toList());
    }

    @ParameterizedTest @EnumSource(Mode.class)
    void acceptsEachEmotionInQueryAndImport(Mode mode) throws Exception {
        var spots = mock(SpotService.class);
        when(spots.findInBounds(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new SpotListResponse(List.of()));
        mvc(new SpotController(spots)).perform(get("/api/spots")
                        .param("swLat", "35").param("swLng", "129")
                        .param("neLat", "36").param("neLng", "130").param("mode", mode.name()))
                .andExpect(status().isOk());
        verify(spots).findInBounds(any(), any(), any(), any(), any(), any(), eq(mode));

        var imports = mock(SpotImportService.class);
        when(imports.receive(any(), any())).thenReturn(new SpotImportResponse(1, 0, List.of()));
        mvc(new SpotImportController(imports)).perform(post("/api/internal/spots")
                        .header("X-Internal-Api-Key", "test-key").contentType("application/json").content(body(mode.name())))
                .andExpect(status().isOk());
        var captured = ArgumentCaptor.forClass(SpotImportRequest.class);
        verify(imports).receive(eq("test-key"), captured.capture());
        assertEquals(List.of(mode), captured.getValue().spots().getFirst().modes());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WALK", "CONTEMPLATION", "SCENERY", "WATER_GAZING", "CULTURE", "UNKNOWN"})
    void rejectsRetiredAndUnknownLabels(String mode) throws Exception {
        var spots = mock(SpotService.class);
        mvc(new SpotController(spots)).perform(get("/api/spots")
                        .param("swLat", "35").param("swLng", "129")
                        .param("neLat", "36").param("neLng", "130").param("mode", mode))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("InvalidParameterException"));
        var imports = mock(SpotImportService.class);
        mvc(new SpotImportController(imports)).perform(post("/api/internal/spots")
                        .contentType("application/json").content(body(mode)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("InvalidRequestBodyException"));
        verifyNoInteractions(spots, imports);
    }

    private MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private String body(String mode) {
        return """
                {"spots":[{"tourApiContentId":"123","name":"Place","address":"Busan",
                  "latitude":35,"longitude":129,"category":"PARK","modes":["%s"],
                  "sourceUpdatedAt":"2026-09-12T12:00:00+09:00"}]}
                """.formatted(mode);
    }
}
