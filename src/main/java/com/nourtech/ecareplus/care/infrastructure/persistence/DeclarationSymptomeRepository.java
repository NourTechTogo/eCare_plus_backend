package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.DeclarationSymptome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeclarationSymptomeRepository extends JpaRepository<DeclarationSymptome, UUID> {

    List<DeclarationSymptome> findAllByPatientId(UUID patientId);
}
