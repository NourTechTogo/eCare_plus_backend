package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.ObservationSuivi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObservationSuiviRepository extends JpaRepository<ObservationSuivi, UUID> {
    org.springframework.data.domain.Page<ObservationSuivi> findAllByDossierIdAndArchiveeFalse(UUID dossier,org.springframework.data.domain.Pageable page);

    List<ObservationSuivi> findAllByDossierId(UUID dossierId);
}
