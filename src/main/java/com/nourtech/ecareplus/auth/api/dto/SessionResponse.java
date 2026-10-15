package com.nourtech.ecareplus.auth.api.dto;

import java.time.Instant;

public record SessionResponse(
        String jeton,
        String type,
        Instant expireLe,
        ProfilAuthentifieResponse profil
) {
}
