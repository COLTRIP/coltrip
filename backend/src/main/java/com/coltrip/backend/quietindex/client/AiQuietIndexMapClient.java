package com.coltrip.backend.quietindex.client;

import com.coltrip.backend.alternative.exception.AiIntegrationException;
import com.coltrip.backend.config.AiProperties;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AiQuietIndexMapClient {

    private final RestClient restClient;
    private final AiProperties properties;

    public AiQuietIndexMapClient(@Qualifier("aiRestClient") RestClient restClient, AiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public List<Item> fetchMap(int hour, boolean isWeekend) {
        if (!StringUtils.hasText(properties.baseUrl()) || !StringUtils.hasText(properties.apiKey())) {
            throw new AiIntegrationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AiNotConfigured", "AI 서버 주소와 인증키 설정이 필요합니다.");
        }
        try {
            Item[] result = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/quiet-index/map")
                            .queryParam("hour", hour)
                            .queryParam("is_weekend", isWeekend)
                            .build())
                    .header("X-API-Key", properties.apiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Item[].class);
            if (result == null) {
                throw new AiIntegrationException(HttpStatus.BAD_GATEWAY,
                        "AiInvalidResponse", "AI 고요지수 지도 응답 본문이 없습니다.");
            }
            // null 항목도 건별 검증에서 집계할 수 있도록 보존한다.
            return Arrays.asList(result);
        } catch (ResourceAccessException e) {
            for (Throwable cause = e; cause != null; cause = cause.getCause()) {
                if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                    throw new AiIntegrationException(HttpStatus.GATEWAY_TIMEOUT,
                            "AiTimeout", "AI 고요지수 지도 조회 요청 시간이 초과되었습니다.");
                }
            }
            throw new AiIntegrationException(HttpStatus.BAD_GATEWAY,
                    "AiConnectionFailed", "AI 고요지수 지도 서버에 연결할 수 없습니다.");
        } catch (RestClientException e) {
            throw new AiIntegrationException(HttpStatus.BAD_GATEWAY,
                    "AiRequestFailed", "AI 고요지수 지도 서버가 정상적인 응답을 반환하지 않았습니다.");
        }
    }

    public record Item(String poiId, String name, Integer population, Double quietIndex) {
    }
}
