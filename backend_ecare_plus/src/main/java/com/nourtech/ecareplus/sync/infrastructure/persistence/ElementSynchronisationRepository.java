package com.nourtech.ecareplus.sync.infrastructure.persistence;

import com.nourtech.ecareplus.sync.domain.entity.ElementSynchronisation;
import com.nourtech.ecareplus.sync.domain.enumeration.StatutSynchronisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElementSynchronisationRepository extends JpaRepository<ElementSynchronisation, UUID> {

    Optional<ElementSynchronisation> findByUtilisateurIdAndIdentifiantAppareilAndIdentifiantLocal(
            UUID utilisateurId,
            String identifiantAppareil,
            String identifiantLocal
    );

    List<ElementSynchronisation> findAllByStatutOrderByDateCreationLocaleAsc(StatutSynchronisation statut);
}
