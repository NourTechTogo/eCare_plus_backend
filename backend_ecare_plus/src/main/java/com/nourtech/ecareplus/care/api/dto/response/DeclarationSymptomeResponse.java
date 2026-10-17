package com.nourtech.ecareplus.care.api.dto.response;

import com.nourtech.ecareplus.care.domain.enumeration.StatutEvaluationSymptome;

import java.time.Instant;
import java.util.UUID;

public record DeclarationSymptomeResponse(
        UUID id,
        UUID patientId,
        String symptome,
        String intensite,
        String description,
        Instant dateApparition,
        Instant dateDeclaration,
        boolean persistant,
        StatutEvaluationSymptome statutEvaluation,
        long version
) {
}
