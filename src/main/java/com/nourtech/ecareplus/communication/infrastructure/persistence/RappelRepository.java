package com.nourtech.ecareplus.communication.infrastructure.persistence;

import com.nourtech.ecareplus.communication.domain.entity.Rappel;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutRappel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RappelRepository extends JpaRepository<Rappel, UUID> {

    List<Rappel> findAllByPatientIdAndStatutOrderByDateHeurePrevueAsc(UUID patientId, StatutRappel statut);

    Page<Rappel> findAllByPatientId(UUID patientId, Pageable pageable);

    List<Rappel> findAllByStatutAndDateHeurePrevueLessThanEqualOrderByDateHeurePrevueAsc(
            StatutRappel statut,
            Instant jusqua
    );
}
