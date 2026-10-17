package com.nourtech.ecareplus.alert.infrastructure.persistence;

import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.domain.enumeration.StatutAlerte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlerteRepository extends JpaRepository<Alerte, UUID> {

    List<Alerte> findAllByPatientIdAndStatutOrderByDateCreationDesc(UUID patientId, StatutAlerte statut);

    List<Alerte> findAllByProfessionnelIdAndStatut(UUID professionnelId, StatutAlerte statut);

    boolean existsByRegleIdAndMesureId(UUID regleId, UUID mesureId);
}
