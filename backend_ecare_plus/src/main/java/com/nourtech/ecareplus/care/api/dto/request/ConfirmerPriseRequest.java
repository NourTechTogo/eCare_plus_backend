package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.Size;

public record ConfirmerPriseRequest(
        @Size(max = 2000) String commentaire
) {
}
