package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.Instant;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MesureSanteRepository extends JpaRepository<MesureSante, UUID> {

    List<MesureSante> findAllByPatientIdOrderByDateMesureDesc(UUID patientId);

    Page<MesureSante> findAllByPatientId(UUID patientId, Pageable pageable);

    List<MesureSante> findAllByDossierIdOrderByDateMesureDesc(UUID dossierId);

    List<MesureSante> findAllByPatientIdAndTypeMesureAndDateMesureBetween(
            UUID patientId,
            TypeMesure typeMesure,
            Instant debut,
            Instant fin
    );
}
