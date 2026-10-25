package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.PriseMedicament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PriseMedicamentRepository extends JpaRepository<PriseMedicament, UUID> {
    @org.springframework.data.jpa.repository.Query("select p from PriseMedicament p join fetch p.traitement t join fetch p.medicament where t.dossier.patient.id=:patient order by p.id")
    List<PriseMedicament> planningPatient(UUID patient);
    @org.springframework.data.jpa.repository.Query("select p from PriseMedicament p join fetch p.traitement t join fetch p.medicament where t.dossier.patient.id=:patient and (:structure is null or t.dossier.structure.id=:structure) order by p.id")
    List<PriseMedicament> planningPatientStructure(UUID patient,UUID structure);

    List<PriseMedicament> findAllByTraitementIdAndActif(UUID traitementId, boolean actif);
}
