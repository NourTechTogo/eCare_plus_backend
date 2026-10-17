package com.nourtech.ecareplus.consent.infrastructure.persistence;

import com.nourtech.ecareplus.consent.domain.entity.Consentement;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsentementRepository extends JpaRepository<Consentement, UUID> {

    List<Consentement> findAllByPatientIdAndStatut(UUID patientId, StatutConsentement statut);
}
