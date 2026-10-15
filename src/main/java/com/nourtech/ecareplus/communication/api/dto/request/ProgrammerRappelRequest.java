package com.nourtech.ecareplus.communication.api.dto.request;

import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.domain.enumeration.RecurrenceRappel;
import com.nourtech.ecareplus.communication.domain.enumeration.TypeRappel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record ProgrammerRappelRequest(
        @NotNull UUID patientId,
        UUID priseMedicamentId,
        @NotNull TypeRappel typeRappel,
        @NotBlank @Size(max = 150) String titre,
        @NotBlank @Size(max = 500) String message,
        @NotNull Instant dateHeurePrevue,
        @NotNull RecurrenceRappel frequence,
        @NotNull CanalNotification canal,
        Instant dateFin
) {
}
