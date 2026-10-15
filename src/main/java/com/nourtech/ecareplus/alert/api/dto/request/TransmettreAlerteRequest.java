package com.nourtech.ecareplus.alert.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TransmettreAlerteRequest(
        @NotNull UUID professionnelId
) {
}
