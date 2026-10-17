package com.nourtech.ecareplus.communication.api.dto.response;

import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutEnvoiNotification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID utilisateurId,
        String titre,
        String message,
        String typeNotification,
        CanalNotification canal,
        Instant dateEnvoi,
        Instant dateLecture,
        StatutEnvoiNotification statutEnvoi,
        boolean sensible,
        long version
) {
}
