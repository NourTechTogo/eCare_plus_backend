package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.Pathologie;
import com.nourtech.ecareplus.care.domain.enumeration.StatutPathologie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PathologieRepository extends JpaRepository<Pathologie, UUID> {

    List<Pathologie> findAllByDossierIdAndStatut(UUID dossierId, StatutPathologie statut);
}
