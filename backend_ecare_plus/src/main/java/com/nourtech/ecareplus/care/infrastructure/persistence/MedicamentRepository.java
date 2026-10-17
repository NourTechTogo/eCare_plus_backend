package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.Medicament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MedicamentRepository extends JpaRepository<Medicament, UUID> {
}
