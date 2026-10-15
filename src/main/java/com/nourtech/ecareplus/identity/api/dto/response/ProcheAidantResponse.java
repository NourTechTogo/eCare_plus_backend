package com.nourtech.ecareplus.identity.api.dto.response;

public record ProcheAidantResponse(
        UtilisateurResponse utilisateur,
        String lienAvecPatient,
        String telephoneSecondaire,
        String statutAutorisation
) {
}
