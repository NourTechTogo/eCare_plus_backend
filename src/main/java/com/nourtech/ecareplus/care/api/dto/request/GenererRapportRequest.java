package com.nourtech.ecareplus.care.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GenererRapportRequest(
        @NotBlank @Size(max = 200) String titre,
        @NotNull LocalDate dateDebut,
        @NotNull LocalDate dateFin,
        @Size(max = 5000) String resume,
        @NotBlank @Size(max = 20) String format
) {
}
