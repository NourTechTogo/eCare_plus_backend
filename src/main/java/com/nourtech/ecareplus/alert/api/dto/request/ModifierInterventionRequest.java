package com.nourtech.ecareplus.alert.api.dto.request;

import jakarta.validation.constraints.Size;

public record ModifierInterventionRequest(
        @Size(max = 5000) String description,
        @Size(max = 5000) String resultat
) {
}
