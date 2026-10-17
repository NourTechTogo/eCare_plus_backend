package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.ObjectifSuivi;
import com.nourtech.ecareplus.care.domain.enumeration.StatutObjectif;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObjectifSuiviRepository extends JpaRepository<ObjectifSuivi, UUID> {

    List<ObjectifSuivi> findAllByDossierIdAndStatut(UUID dossierId, StatutObjectif statut);
}
