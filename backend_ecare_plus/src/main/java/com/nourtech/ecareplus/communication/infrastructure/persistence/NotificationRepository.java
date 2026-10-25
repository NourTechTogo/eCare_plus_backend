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
    Page<Notification> findAllByUtilisateurIdAndDateSuppressionIsNullAndDateArchivageIsNull(UUID utilisateurId, Pageable pageable);
    Page<Notification> findAllByUtilisateurIdAndDateSuppressionIsNullAndDateArchivageIsNotNull(UUID utilisateurId, Pageable pageable);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select n from Notification n where n.id = :id")
    java.util.Optional<Notification> verrouiller(@org.springframework.data.repository.query.Param("id") UUID id);

}
