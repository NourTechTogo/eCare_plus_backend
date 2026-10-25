package com.nourtech.ecareplus.communication.infrastructure.persistence;

import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutEnvoiNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    java.util.Optional<Notification> findByUtilisateurIdAndSourceRef(UUID utilisateur,String sourceRef);

    List<Notification> findAllByUtilisateurIdAndStatutEnvoi(UUID utilisateurId, StatutEnvoiNotification statutEnvoi);

    Page<Notification> findAllByUtilisateurId(UUID utilisateurId, Pageable pageable);
}
