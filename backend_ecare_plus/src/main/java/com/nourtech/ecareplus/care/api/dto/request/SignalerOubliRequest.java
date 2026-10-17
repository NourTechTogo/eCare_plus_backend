package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.Size;

public record SignalerOubliRequest(
        @Size(max = 2000) String commentaire
) {
}
