package com.nourtech.ecareplus.communication.infrastructure.persistence;

import com.nourtech.ecareplus.communication.domain.entity.RendezVous;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RendezVousRepository extends JpaRepository<RendezVous, UUID> {

    List<RendezVous> findAllByPatientId(UUID patientId);
}
