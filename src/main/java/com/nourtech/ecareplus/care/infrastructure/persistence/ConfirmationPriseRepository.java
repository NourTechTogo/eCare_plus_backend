package com.nourtech.ecareplus.care.infrastructure.persistence;

import com.nourtech.ecareplus.care.domain.entity.ConfirmationPrise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConfirmationPriseRepository extends JpaRepository<ConfirmationPrise, UUID> {

    List<ConfirmationPrise> findAllByPriseMedicamentId(UUID priseMedicamentId);

    Optional<ConfirmationPrise> findByPriseMedicamentIdAndDatePrevue(UUID priseMedicamentId, Instant datePrevue);
}
