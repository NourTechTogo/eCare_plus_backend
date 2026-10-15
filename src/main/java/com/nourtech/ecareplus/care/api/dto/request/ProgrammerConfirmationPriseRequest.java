package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ProgrammerConfirmationPriseRequest(
        @NotNull Instant datePrevue
) {
}
