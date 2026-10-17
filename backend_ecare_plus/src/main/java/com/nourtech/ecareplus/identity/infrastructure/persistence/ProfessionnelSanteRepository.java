package com.nourtech.ecareplus.identity.infrastructure.persistence;

import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionnelSanteRepository extends JpaRepository<ProfessionnelSante, UUID> {

    Optional<ProfessionnelSante> findByNumeroProfessionnel(String numeroProfessionnel);

    boolean existsByNumeroProfessionnel(String numeroProfessionnel);

    List<ProfessionnelSante> findAllByStructureId(UUID structureId);
}
