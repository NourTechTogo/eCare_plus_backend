package com.nourtech.ecareplus.organization.api.dto.response;

import com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel;
import com.nourtech.ecareplus.organization.domain.enumeration.StatutAffectation;

import java.time.LocalDate;
import java.util.UUID;

public record AffectationPatientResponse(
        UUID id,
        UUID patientId,
        UUID professionnelId,
        UUID structureId,
        LocalDate dateDebut,
        LocalDate dateFin,
        RoleProfessionnel roleProfessionnel,
        StatutAffectation statut,
        boolean professionnelPrincipal,
        long version
) {
}
