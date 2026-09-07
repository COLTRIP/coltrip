package com.coltrip.backend.spot.service;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.Mode;
import com.coltrip.backend.domain.spot.TouristSpot;
import com.coltrip.backend.domain.spot.TouristSpotRepository;
import com.coltrip.backend.spot.dto.SpotDetailResponse;
import com.coltrip.backend.spot.dto.SpotListResponse;
import com.coltrip.backend.spot.exception.InvalidBoundingBoxException;
import com.coltrip.backend.spot.exception.SpotNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotService {

    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    private final TouristSpotRepository touristSpotRepository;

    public SpotListResponse findInBounds(BigDecimal swLat, BigDecimal swLng,
                                          BigDecimal neLat, BigDecimal neLng,
                                          Category category, Mode mode) {
        validateBounds(swLat, swLng, neLat, neLng);
        List<TouristSpot> spots = touristSpotRepository.findInBounds(swLat, neLat, swLng, neLng, category, mode);
        return SpotListResponse.from(spots);
    }

    public SpotDetailResponse findById(Long spotId) {
        return touristSpotRepository.findByIdWithModes(spotId)
                .map(SpotDetailResponse::from)
                .orElseThrow(SpotNotFoundException::new);
    }

    private void validateBounds(BigDecimal swLat, BigDecimal swLng, BigDecimal neLat, BigDecimal neLng) {
        if (isOutOfRange(swLat, MIN_LATITUDE, MAX_LATITUDE) || isOutOfRange(neLat, MIN_LATITUDE, MAX_LATITUDE)) {
            throw new InvalidBoundingBoxException("위도는 -90 ~ 90 사이여야 합니다.");
        }
        if (isOutOfRange(swLng, MIN_LONGITUDE, MAX_LONGITUDE) || isOutOfRange(neLng, MIN_LONGITUDE, MAX_LONGITUDE)) {
            throw new InvalidBoundingBoxException("경도는 -180 ~ 180 사이여야 합니다.");
        }
        if (swLat.compareTo(neLat) > 0) {
            throw new InvalidBoundingBoxException("swLat은 neLat보다 클 수 없습니다.");
        }
        if (swLng.compareTo(neLng) > 0) {
            throw new InvalidBoundingBoxException("swLng은 neLng보다 클 수 없습니다.");
        }
    }

    private boolean isOutOfRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) < 0 || value.compareTo(max) > 0;
    }
}
