package com.nourtech.ecareplus.communication.infrastructure.persistence;

import com.nourtech.ecareplus.communication.domain.entity.RendezVous;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RendezVousRepository extends JpaRepository<RendezVous, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<RendezVous> {

    List<RendezVous> findAllByPatientId(UUID patientId);
    @org.springframework.data.jpa.repository.Query("select r.id from RendezVous r join r.rappel p where r.personnel=true and p.statut=:statut and p.dateHeurePrevue<=:maintenant order by p.dateHeurePrevue,r.id")
    List<UUID> echeancesPersonnelles(java.time.Instant maintenant,com.nourtech.ecareplus.communication.domain.enumeration.StatutRappel statut,org.springframework.data.domain.Pageable page);
}
