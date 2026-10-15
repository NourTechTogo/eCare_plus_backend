package com.nourtech.ecareplus.organization.application.command;

import com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel;

import java.time.LocalDate;
import java.util.UUID;

public record AffecterPatientCommand(
        UUID patientId,
        UUID professionnelId,
        UUID structureId,
        RoleProfessionnel role,
        LocalDate dateDebut,
        boolean professionnelPrincipal
) {
}
