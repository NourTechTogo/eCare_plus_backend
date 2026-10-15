package com.nourtech.ecareplus.communication.infrastructure.persistence;

import com.nourtech.ecareplus.communication.domain.entity.ContenuEducatif;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutValidationContenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContenuEducatifRepository extends JpaRepository<ContenuEducatif, UUID> {

    List<ContenuEducatif> findAllByStatutValidation(StatutValidationContenu statutValidation);
}
