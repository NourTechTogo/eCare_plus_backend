package com.nourtech.ecareplus.identity.infrastructure.persistence;

import com.nourtech.ecareplus.identity.domain.entity.AdministrateurStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdministrateurStructureRepository extends JpaRepository<AdministrateurStructure, UUID> {

    List<AdministrateurStructure> findAllByStructureId(UUID structureId);
}
