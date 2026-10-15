package com.nourtech.ecareplus.alert.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloturerAlerteRequest(
        @NotBlank @Size(max = 2000) String motifCloture
) {
}
