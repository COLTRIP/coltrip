package com.coltrip.backend.internal.spot;

import com.coltrip.backend.domain.spot.Category;
import com.coltrip.backend.domain.spot.Mode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SpotImportRequest(
        @NotNull @Size(min = 1, max = 100) List<@NotNull @Valid Item> spots) {
    public record Item(
            @NotBlank @Pattern(regexp = "[0-9]{1,50}") String tourApiContentId,
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 300) String address,
            @NotNull @DecimalMin("-90") @DecimalMax("90") @Digits(integer = 3, fraction = 7) BigDecimal latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") @Digits(integer = 3, fraction = 7) BigDecimal longitude,
            @NotNull Category category,
            @Size(max = 10000) String description,
            @Size(max = 500) String imageUrl,
            @Size(max = 500) String recommendReason,
            @NotNull @Size(max = 20) List<@NotNull Mode> modes,
            @NotNull OffsetDateTime sourceUpdatedAt) { }
}
