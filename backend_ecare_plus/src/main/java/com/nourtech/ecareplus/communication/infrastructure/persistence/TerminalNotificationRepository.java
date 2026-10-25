package com.nourtech.ecareplus.communication.infrastructure.persistence;
import com.nourtech.ecareplus.communication.domain.entity.TerminalNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TerminalNotificationRepository extends JpaRepository<TerminalNotification,UUID>{
    Optional<TerminalNotification> findByInstallationId(String installation);
    List<TerminalNotification> findAllByUtilisateurId(UUID utilisateur);
    List<TerminalNotification> findAllByUtilisateurIdAndActifTrue(UUID utilisateur);
    List<TerminalNotification> findAllByUtilisateurIdAndSessionRef(UUID utilisateur,String session);
}
