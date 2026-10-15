package com.nourtech.ecareplus.alert.infrastructure.persistence;

import com.nourtech.ecareplus.alert.domain.entity.InterventionSuivi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterventionSuiviRepository extends JpaRepository<InterventionSuivi, UUID> {

    List<InterventionSuivi> findAllByAlerteId(UUID alerteId);
}
