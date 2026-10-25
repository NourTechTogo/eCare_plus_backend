package com.nourtech.ecareplus.communication.infrastructure.persistence;
import com.nourtech.ecareplus.communication.domain.entity.EnvoiNotification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.*;
public interface EnvoiNotificationRepository extends JpaRepository<EnvoiNotification,UUID>{
    boolean existsByEvenementIdAndCibleIdAndCanal(UUID evenement,UUID cible,String canal);
    @Query("select e.id from EnvoiNotification e where ((e.statut='EN_ATTENTE' or e.statut='A_REESSAYER') and e.prochaineTentative<=:date) or (e.statut='EN_COURS' and e.bailExpireLe<=:date) order by e.prochaineTentative,e.id")
    List<UUID> disponibles(Instant date,Pageable page);
}
