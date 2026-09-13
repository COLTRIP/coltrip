package com.coltrip.backend.alternative.client;

import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.config.AiProperties;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AiAlternativeClient {

    private final RestClient restClient;
    private final AiProperties properties;

    public AiAlternativeClient(@Qualifier("aiRestClient") RestClient restClient,
                               AiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public Result fetch(Request request) {
        if (!StringUtils.hasText(properties.baseUrl()) || !StringUtils.hasText(properties.apiKey())) {
            throw new AiIntegrationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AiNotConfigured", "AI 서버 주소와 인증키 설정이 필요합니다.");
        }
        try {
            Result result = restClient.post()
                    .uri("/alternative")
                    .header("X-API-Key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Result.class);
            validate(result);
            return result;
        } catch (ResourceAccessException e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                    throw new AiIntegrationException(HttpStatus.GATEWAY_TIMEOUT,
                            "AiTimeout", "AI 추천 요청 시간이 초과되었습니다.");
                }
            }
            throw new AiIntegrationException(HttpStatus.BAD_GATEWAY,
                    "AiConnectionFailed", "AI 추천 서버에 연결할 수 없습니다.");
        } catch (RestClientException e) {
            throw new AiIntegrationException(HttpStatus.BAD_GATEWAY,
                    "AiRequestFailed", "AI 추천 서버가 정상적인 응답을 반환하지 않았습니다.");
        }
    }

    private void validate(Result result) {
        if (result == null || result.triggered() == null
                || !inRange(result.targetQuietIndex(), 0, 100) || result.alternatives() == null) {
            throw invalidResponse();
        }
        if (!result.triggered() && !result.alternatives().isEmpty()) {
            throw invalidResponse();
        }
        for (Candidate candidate : result.alternatives()) {
            if (candidate == null || !StringUtils.hasText(candidate.poiId())
                    || !inRange(candidate.quietIndex(), 0, 100)
                    || !inRange(candidate.score(), 0, 1)
                    || candidate.distanceKm() == null || !Double.isFinite(candidate.distanceKm())
                    || candidate.distanceKm() < 0 || candidate.recommendReason() == null) {
                throw invalidResponse();
            }
        }
    }

    private boolean inRange(Double value, double min, double max) {
        return value != null && Double.isFinite(value) && value >= min && value <= max;
    }

    private AiIntegrationException invalidResponse() {
        return new AiIntegrationException(HttpStatus.BAD_GATEWAY,
                "AiInvalidResponse", "AI 추천 응답의 필수값 또는 점수 범위가 올바르지 않습니다.");
    }

    public record Request(String poiId, int hour, boolean isWeekend, Double baselineQuietIndex) {
    }

    public record Result(Boolean triggered, Double targetQuietIndex, List<Candidate> alternatives) {
    }

    public record Candidate(String poiId, String name, Double quietIndex,
                            Double distanceKm, Double score, String recommendReason) {
    }
}
