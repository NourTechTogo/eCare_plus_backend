package com.nourtech.ecareplus.communication.application.command;

import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;

import java.util.UUID;

public record CreerNotificationCommand(
        UUID utilisateurId,
        String titre,
        String message,
        String typeNotification,
        CanalNotification canal,
        boolean sensible
) {
}
