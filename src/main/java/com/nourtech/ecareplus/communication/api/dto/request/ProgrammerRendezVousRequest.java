package com.nourtech.ecareplus.communication.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record ProgrammerRendezVousRequest(
        @NotNull UUID patientId,
        @NotBlank @Size(max = 200) String motif,
        @NotNull Instant dateHeure,
        @Size(max = 300) String lieu,
        @NotBlank @Size(max = 60) String typeRendezVous,
        @Size(max = 2000) String commentaire
) {
}
