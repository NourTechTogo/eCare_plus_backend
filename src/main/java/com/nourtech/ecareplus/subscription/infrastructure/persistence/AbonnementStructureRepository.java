package com.nourtech.ecareplus.subscription.infrastructure.persistence;

import com.nourtech.ecareplus.subscription.domain.entity.AbonnementStructure;
import com.nourtech.ecareplus.subscription.domain.enumeration.StatutAbonnement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AbonnementStructureRepository extends JpaRepository<AbonnementStructure, UUID> {

    List<AbonnementStructure> findAllByStructureIdOrderByDateDebutDesc(UUID structureId);

    List<AbonnementStructure> findAllByStatut(StatutAbonnement statut);

    List<AbonnementStructure> findAllByStructureIdAndStatut(UUID structureId, StatutAbonnement statut);
}
