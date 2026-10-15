package com.nourtech.ecareplus.alert.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CloturerInterventionRequest(
        @NotBlank @Size(max = 5000) String resultatFinal
) {
}
