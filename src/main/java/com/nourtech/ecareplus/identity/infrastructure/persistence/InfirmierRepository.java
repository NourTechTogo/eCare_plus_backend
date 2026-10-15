package com.nourtech.ecareplus.identity.infrastructure.persistence;

import com.nourtech.ecareplus.identity.domain.entity.Infirmier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InfirmierRepository extends JpaRepository<Infirmier, UUID> {

    List<Infirmier> findAllByStructureId(UUID structureId);
}
