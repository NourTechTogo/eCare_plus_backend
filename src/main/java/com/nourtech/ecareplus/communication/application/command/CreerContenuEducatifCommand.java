package com.nourtech.ecareplus.communication.application.command;

import java.util.UUID;

public record CreerContenuEducatifCommand(
        UUID auteurId,
        String titre,
        String resume,
        String contenu,
        String typeContenu,
        String langue,
        String pathologieCible,
        String urlMedia
) {
}
