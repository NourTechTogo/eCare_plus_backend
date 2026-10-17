package com.nourtech.ecareplus.sync.application.command;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RecevoirSynchronisationCommand(
        UUID utilisateurId,
        String typeRessource,
        String identifiantAppareil,
        String identifiantLocal,
        Map<String, Object> contenu,
        Instant dateCreationLocale
) {
}
