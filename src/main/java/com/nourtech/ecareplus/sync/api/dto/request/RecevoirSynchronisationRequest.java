package com.nourtech.ecareplus.sync.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RecevoirSynchronisationRequest(
        @NotNull UUID utilisateurId,
        @NotBlank @Size(max = 80) String typeRessource,
        @NotBlank @Size(max = 100) String identifiantAppareil,
        @NotBlank @Size(max = 100) String identifiantLocal,
        @NotEmpty Map<String, Object> contenu,
        @NotNull Instant dateCreationLocale
) {
}
