package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReporterConfirmationRequest(
        @NotNull Instant nouvelleDatePrevue
) {
}
