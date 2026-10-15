package com.nourtech.ecareplus.alert.api.dto.request;

import jakarta.validation.constraints.Size;

import java.time.Instant;

public record DefinirActionSuivanteRequest(
        @Size(max = 2000) String actionSuivante,
        Instant dateProchaineAction
) {
}
