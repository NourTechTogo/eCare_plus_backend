package com.nourtech.ecareplus.alert.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AffecterAlerteRequest(
        @NotNull UUID professionnelId
) {
}
