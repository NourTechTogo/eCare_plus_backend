package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.PriseMedicament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PriseMedicamentRepository extends JpaRepository<PriseMedicament, UUID> {

    List<PriseMedicament> findAllByTraitementIdAndActif(UUID traitementId, boolean actif);
}
