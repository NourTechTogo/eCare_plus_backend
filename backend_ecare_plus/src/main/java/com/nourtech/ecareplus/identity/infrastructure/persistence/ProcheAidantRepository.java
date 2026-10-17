package com.nourtech.ecareplus.identity.infrastructure.persistence;

import com.nourtech.ecareplus.identity.domain.entity.ProcheAidant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcheAidantRepository extends JpaRepository<ProcheAidant, UUID> {
}
