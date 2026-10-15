package com.nourtech.ecareplus.auth.infrastructure.persistence;
import com.nourtech.ecareplus.auth.domain.entity.DemandeReinitialisation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
public interface DemandeReinitialisationRepository extends JpaRepository<DemandeReinitialisation, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DemandeReinitialisation> findFirstByUtilisateurIdOrderByDateCreationDesc(UUID utilisateurId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DemandeReinitialisation> findFirstByJetonHashAndUtiliseeFalse(String jetonHash);
}
