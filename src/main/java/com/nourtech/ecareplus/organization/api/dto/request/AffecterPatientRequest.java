package com.nourtech.ecareplus.organization.api.dto.request;

import com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.UUID;

public record AffecterPatientRequest(
        @NotNull UUID patientId,
        @NotNull UUID professionnelId,
        @NotNull UUID structureId,
        @NotNull RoleProfessionnel role,
        @NotNull @PastOrPresent LocalDate dateDebut,
        boolean professionnelPrincipal
) {
}
