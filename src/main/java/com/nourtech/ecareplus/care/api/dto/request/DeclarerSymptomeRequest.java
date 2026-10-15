package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record DeclarerSymptomeRequest(
        @NotNull UUID patientId,
        @NotBlank @Size(max = 150) String symptome,
        @NotBlank @Size(max = 30) String intensite,
        @Size(max = 5000) String description,
        @PastOrPresent Instant dateApparition,
        boolean persistant
) {
}
