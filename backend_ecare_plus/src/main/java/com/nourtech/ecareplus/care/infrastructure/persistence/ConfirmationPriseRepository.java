package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.ConfirmationPrise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfirmationPriseRepository extends JpaRepository<ConfirmationPrise, UUID> {
    Optional<ConfirmationPrise> findByPriseMedicamentIdAndDateInitiale(UUID priseMedicamentId,Instant dateInitiale);
    @org.springframework.data.jpa.repository.Query("select c from ConfirmationPrise c where c.priseMedicament.traitement.dossier.patient.id=:patient and c.dateInitiale>=:debut and c.dateInitiale<:fin order by c.dateInitiale,c.id")
    List<ConfirmationPrise> periode(UUID patient,Instant debut,Instant fin);
    @org.springframework.data.jpa.repository.Query("select c from ConfirmationPrise c where c.priseMedicament.traitement.dossier.patient.id=:patient and (:structure is null or c.priseMedicament.traitement.dossier.structure.id=:structure) and c.dateInitiale>=:debut and c.dateInitiale<:fin order by c.dateInitiale,c.id")
    List<ConfirmationPrise> periodeStructure(UUID patient,UUID structure,Instant debut,Instant fin);
    long countByPriseMedicamentId(UUID prise);

    List<ConfirmationPrise> findAllByPriseMedicamentId(UUID priseMedicamentId);

    Optional<ConfirmationPrise> findByPriseMedicamentIdAndDatePrevue(UUID priseMedicamentId, Instant datePrevue);
}
