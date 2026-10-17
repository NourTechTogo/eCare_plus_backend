package com.nourtech.ecareplus.audit.infrastructure.persistence;

import com.nourtech.ecareplus.audit.domain.entity.JournalActivite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JournalActiviteRepository extends JpaRepository<JournalActivite, UUID> {

    Page<JournalActivite> findAllByUtilisateurId(UUID utilisateurId, Pageable pageable);

    Page<JournalActivite> findAllByRessourceAndRessourceId(
            String ressource,
            UUID ressourceId,
            Pageable pageable
    );
}
