package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.RapportSuivi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RapportSuiviRepository extends JpaRepository<RapportSuivi, UUID> {

    List<RapportSuivi> findAllByDossierId(UUID dossierId);
}
