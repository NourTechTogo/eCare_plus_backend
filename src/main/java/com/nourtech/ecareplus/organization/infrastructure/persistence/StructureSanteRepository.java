package com.nourtech.ecareplus.organization.infrastructure.persistence;

import com.nourtech.ecareplus.organization.domain.enumeration.StatutStructure;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StructureSanteRepository extends JpaRepository<StructureSante, UUID> {

    Optional<StructureSante> findByNumeroAutorisation(String numeroAutorisation);

    boolean existsByNumeroAutorisation(String numeroAutorisation);

    List<StructureSante> findAllByStatut(StatutStructure statut);
}
