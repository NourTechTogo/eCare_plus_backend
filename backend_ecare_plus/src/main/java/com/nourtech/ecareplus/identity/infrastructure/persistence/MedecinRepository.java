package com.nourtech.ecareplus.identity.infrastructure.persistence;

import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MedecinRepository extends JpaRepository<Medecin, UUID> {

    Optional<Medecin> findByNumeroOrdre(String numeroOrdre);

    boolean existsByNumeroOrdre(String numeroOrdre);
}
