package com.coltrip.backend.forecast;

import com.coltrip.backend.common.exception.ErrorResponse;
import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.Mode;
import com.coltrip.backend.forecast.ForecastResponses.Recommendations;
import com.coltrip.backend.forecast.ForecastResponses.Timeline;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "날짜별 예측 추천")
@RestController
@RequiredArgsConstructor
public class ForecastController {
    private final ForecastQueryService query;
    private final ForecastIngestService ingest;

    @Operation(summary = "날짜/시간대별 추천", description = """
            한국 시간 date와 hour로 예측값이 있는 장소만 조회합니다. 현재 시간 슬롯부터 7일 이내.
            위치기반서비스사업자 등록 이슈로 서버는 위치를 받지 않으며, category/mode 필터에 맞는 전체 장소를 대상으로 합니다.
            limit 1~50(기본 20). 예측 점수 내림차순, 장소 ID 오름차순.
            결과가 없으면 spots=[]와 안내 문구이며 현재 점수로 대체하지 않습니다.
            """)
    @SecurityRequirements
    @GetMapping("/api/spots/recommendations")
    public ResponseEntity<Recommendations> recommend(@AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer hour,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Mode mode,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
                query.recommend(userId, date, hour, category, mode, limit));
    }

    @Operation(summary = "예측 고요지수 24시간 타임라인", description = """
            date/hour부터 24개 시간 슬롯을 반환합니다. 마지막 슬롯까지 현재 시간 슬롯 기준 7일 이내여야 합니다.
            관측 타임라인과 별도이며 값이 없으면 quietIndex/generatedAt/validUntil/source/modelVersion은 null입니다.
            """)
    @SecurityRequirements
    @GetMapping("/api/spots/{spotId}/quiet-index/forecast")
    public ResponseEntity<Timeline> timeline(@PathVariable Long spotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer hour) {
        return ResponseEntity.ok().body(query.timeline(spotId, date, hour));
    }

    @Operation(summary = "예측 고요지수 주간(7일) 타임라인", description = """
            date/hour부터 하루씩 밀며 총 7개 슬롯(같은 시각 기준 오늘부터 6일 뒤까지)을 반환합니다.
            마지막 슬롯까지 현재 시간 슬롯 기준 7일 이내여야 합니다. /recommendations는 상위 limit개만
            반환하는 순위 목록이라 이 장소가 특정 날짜에 순위 밖으로 밀리면 응답에서 빠지는데, 이 API는
            순위와 무관하게 이 장소의 날짜별 예측만 직접 조회합니다. 값이 없으면
            quietIndex/generatedAt/validUntil/source/modelVersion은 null입니다.
            """)
    @SecurityRequirements
    @GetMapping("/api/spots/{spotId}/quiet-index/forecast/week")
    public ResponseEntity<Timeline> weeklyTimeline(@PathVariable Long spotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer hour) {
        return ResponseEntity.ok().body(query.weeklyTimeline(spotId, date, hour));
    }

    @Operation(summary = "예측 배치 수신 (AI팀 협의용 신규 계약)", description = """
            AI가 아직 이 계약으로 전송하는 것은 아닙니다. X-Internal-Api-Key 인증, TourAPI contentId 기준.
            시간 필드는 UTC offset 필수. 생성 시각/대상 시각/유효기간/모델 출처를 보존합니다.
            같은 장소/대상/출처/생성 시각은 정정, 새 생성 시각은 새 버전.
            최대 1000개 원자적 배치이며 한 건이라도 미등록 장소면 전체 롤백.
            현재 점수나 관측 이력을 덮어쓰지 않습니다.
            """)
    @SecurityRequirements
    @PostMapping("/api/internal/quiet-index/forecasts")
    public ResponseEntity<ForecastIngestService.PushResult> receive(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @Valid @RequestBody ForecastPushRequest request) {
        return ResponseEntity.ok(ingest.receive(apiKey, request));
    }

    @ExceptionHandler(InvalidForecastRequestException.class)
    public ResponseEntity<ErrorResponse> invalid(InvalidForecastRequestException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("InvalidForecastRequest", exception.getMessage()));
    }
}
