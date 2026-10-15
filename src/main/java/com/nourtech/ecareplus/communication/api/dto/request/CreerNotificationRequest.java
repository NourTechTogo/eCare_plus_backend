package com.nourtech.ecareplus.communication.api.dto.request;

import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreerNotificationRequest(
        @NotNull UUID utilisateurId,
        @NotBlank @Size(max = 150) String titre,
        @NotBlank @Size(max = 500) String message,
        @NotBlank @Size(max = 50) String typeNotification,
        @NotNull CanalNotification canal,
        boolean sensible
) {
}
