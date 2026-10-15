package com.nourtech.ecareplus.auth.infrastructure.security;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public record UtilisateurAuthentifie(UUID utilisateurId, String role) {

    public static UtilisateurAuthentifie depuis(JwtAuthenticationToken authentication) {
        return new UtilisateurAuthentifie(
                UUID.fromString(authentication.getToken().getSubject()),
                authentication.getToken().getClaimAsString("role")
        );
    }
}
