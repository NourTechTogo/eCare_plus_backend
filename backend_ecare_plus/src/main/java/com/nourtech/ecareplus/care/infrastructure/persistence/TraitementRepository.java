package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.Traitement;
import com.nourtech.ecareplus.care.domain.enumeration.StatutTraitement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TraitementRepository extends JpaRepository<Traitement, UUID> {

    List<Traitement> findAllByDossierIdAndStatut(UUID dossierId, StatutTraitement statut);
}
