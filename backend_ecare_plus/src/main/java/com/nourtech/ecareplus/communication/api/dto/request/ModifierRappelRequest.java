package com.nourtech.ecareplus.communication.api.dto.request;

import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.domain.enumeration.RecurrenceRappel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ModifierRappelRequest(
        @NotBlank @Size(max = 150) String titre,
        @NotBlank @Size(max = 500) String message,
        @NotNull Instant dateHeurePrevue,
        @NotNull RecurrenceRappel frequence,
        @NotNull CanalNotification canal,
        Instant dateFin
) {
}
