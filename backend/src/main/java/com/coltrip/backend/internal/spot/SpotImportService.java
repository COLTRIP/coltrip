package com.coltrip.backend.internal.spot;

import com.coltrip.backend.config.InternalApiProperties;
import com.coltrip.backend.domain.spot.Mode;
import com.coltrip.backend.domain.spot.SpotMode;
import com.coltrip.backend.domain.spot.SpotModeRepository;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.internal.exception.InvalidInternalApiKeyException;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class SpotImportService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private final TouristSpotRepository spots;
    private final SpotModeRepository modes;
    private final InternalApiProperties internal;
    private final Clock clock;

    @Autowired
    public SpotImportService(TouristSpotRepository spots, SpotModeRepository modes, InternalApiProperties internal) {
        this(spots, modes, internal, Clock.system(ZONE));
    }

    SpotImportService(TouristSpotRepository spots, SpotModeRepository modes, InternalApiProperties internal, Clock clock) {
        this.spots = spots;
        this.modes = modes;
        this.internal = internal;
        this.clock = clock;
    }

    public SpotImportResponse receive(String apiKey, SpotImportRequest request) {
        if (!StringUtils.hasText(internal.apiKey()) || !internal.apiKey().equals(apiKey)) {
            throw new InvalidInternalApiKeyException();
        }
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        Set<String> contentIds = new HashSet<>();
        for (var item : request.spots()) {
            if (!contentIds.add(item.tourApiContentId())) {
                throw new InvalidSpotImportException("동일 배치의 tourApiContentId 중복은 허용하지 않습니다.");
            }
            if (sourceTime(item).isAfter(now)) {
                throw new InvalidSpotImportException("sourceUpdatedAt은 미래 시각일 수 없습니다.");
            }
            validateImageUrl(item.imageUrl());
        }
        List<SpotImportResponse.Result> results = new ArrayList<>();
        int applied = 0;
        int ignored = 0;
        // 신규 행 동시 생성은 DB 유니크 키로 처리하고, 배치 잠금 순서는 ID 기준으로 고정한다.
        for (var item : request.spots().stream().sorted(Comparator.comparing(SpotImportRequest.Item::tourApiContentId)).toList()) {
            spots.ensureImportRow(item.tourApiContentId(), item.name().strip(), item.address().strip(),
                    item.latitude(), item.longitude(), item.category().name(), item.description(),
                    item.imageUrl(), item.recommendReason(), now);
            var spot = spots.findByTourApiContentIdForUpdate(item.tourApiContentId()).orElseThrow();
            LocalDateTime incoming = sourceTime(item);
            if (spot.getSourceUpdatedAt() != null && incoming.isBefore(spot.getSourceUpdatedAt())) {
                ignored++;
                results.add(new SpotImportResponse.Result(item.tourApiContentId(), spot.getId(), "IGNORED_STALE",
                        spot.getSourceUpdatedAt().atZone(ZONE).toOffsetDateTime()));
                continue;
            }
            spot.updateBasicInfo(item.name().strip(), item.address().strip(), item.latitude(), item.longitude(),
                    item.category(), item.description(), item.imageUrl(), item.recommendReason(), incoming);

            Set<Mode> desired = new HashSet<>(item.modes());
            List<SpotMode> existing = modes.findBySpot_Id(spot.getId());
            Set<Mode> existingModes = new HashSet<>();
            existing.forEach(mapping -> existingModes.add(mapping.getMode()));
            modes.deleteAll(existing.stream().filter(mapping -> !desired.contains(mapping.getMode())).toList());
            modes.saveAll(desired.stream().sorted().filter(mode -> !existingModes.contains(mode))
                    .map(mode -> SpotMode.builder().spot(spot).mode(mode).build()).toList());
            applied++;
            results.add(new SpotImportResponse.Result(item.tourApiContentId(), spot.getId(), "APPLIED",
                    incoming.atZone(ZONE).toOffsetDateTime()));
        }
        return new SpotImportResponse(applied, ignored, results);
    }

    private LocalDateTime sourceTime(SpotImportRequest.Item item) {
        return item.sourceUpdatedAt().atZoneSameInstant(ZONE).toLocalDateTime().truncatedTo(ChronoUnit.MICROS);
    }

    private void validateImageUrl(String value) {
        if (value == null) {
            return;
        }
        try {
            URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new InvalidSpotImportException("imageUrl은 정상적인 HTTP(S) 주소이거나 null이어야 합니다.");
        }
    }
}
