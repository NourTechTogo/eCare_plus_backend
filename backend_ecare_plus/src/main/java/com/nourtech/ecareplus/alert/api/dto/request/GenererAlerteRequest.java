package com.nourtech.ecareplus.alert.api.dto.request;

import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GenererAlerteRequest(
        @NotNull UUID patientId,
        UUID mesureId,
        UUID regleId,
        @NotNull TypeAlerte typeAlerte,
        @NotBlank @Size(max = 200) String titre,
        @Size(max = 5000) String description,
        @NotNull NiveauPriorite niveauPriorite
) {
}
