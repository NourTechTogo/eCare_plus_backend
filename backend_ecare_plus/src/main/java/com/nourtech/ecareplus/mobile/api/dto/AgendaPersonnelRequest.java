package com.nourtech.ecareplus.mobile.api.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

/** Calendar entry declared by its owner; never a medical booking confirmation. */
public record AgendaPersonnelRequest(
        @NotNull UUID sourceRef,
        @NotBlank @Size(max=200) String motif,
        @NotNull Instant dateHeure,
        @NotBlank @Size(max=80) String fuseau,
        @Size(max=300) String lieu,
        @Size(max=200) String professionnelNom,
        @Size(max=2000) String commentaire,
        @Min(0) @Max(10080) Integer rappelMinutes,
        @PositiveOrZero Long version
) {}
