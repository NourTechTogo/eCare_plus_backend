package com.nourtech.ecareplus.consent.application.service;

import com.nourtech.ecareplus.consent.application.command.AccorderConsentementCommand;
import com.nourtech.ecareplus.consent.application.command.AutoriserProcheCommand;
import com.nourtech.ecareplus.consent.domain.entity.AutorisationPartage;
import com.nourtech.ecareplus.consent.domain.entity.Consentement;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutAutorisation;
import com.nourtech.ecareplus.consent.domain.enumeration.TypeDonneePartage;
import com.nourtech.ecareplus.consent.infrastructure.persistence.AutorisationPartageRepository;
import com.nourtech.ecareplus.consent.infrastructure.persistence.ConsentementRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProcheAidant;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.identity.infrastructure.persistence.ProcheAidantRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class GestionConsentementService {

    private final ConsentementRepository consentementRepository;
    private final AutorisationPartageRepository autorisationRepository;
    private final PatientRepository patientRepository;
    private final ProcheAidantRepository procheAidantRepository;

    public GestionConsentementService(
            ConsentementRepository consentementRepository,
            AutorisationPartageRepository autorisationRepository,
            PatientRepository patientRepository,
            ProcheAidantRepository procheAidantRepository
    ) {
        this.consentementRepository = consentementRepository;
        this.autorisationRepository = autorisationRepository;
        this.patientRepository = patientRepository;
        this.procheAidantRepository = procheAidantRepository;
    }

    public Consentement accorder(
            UUID patientId,
            String type,
            String description,
            String versionDocument,
            String adresseIp,
            Instant dateAcceptation
    ) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Patient", patientId));
        return consentementRepository.save(new Consentement(
                patient,
                type,
                description,
                versionDocument,
                adresseIp,
                dateAcceptation
        ));
    }

    public Consentement accorder(AccorderConsentementCommand command) {
        return accorder(
                command.patientId(), command.typeConsentement(), command.description(),
                command.versionDocument(), command.adresseIp(), command.dateAcceptation()
        );
    }

    public AutorisationPartage autoriserProche(
            UUID patientId,
            UUID procheAidantId,
            UUID consentementId,
            TypeDonneePartage typeDonnee,
            Instant dateDebut,
            Instant dateFin,
            boolean peutConsulter,
            boolean peutRecevoirNotification
    ) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Patient", patientId));
        ProcheAidant proche = procheAidantRepository.findById(procheAidantId)
                .orElseThrow(() -> new RessourceIntrouvableException("ProcheAidant", procheAidantId));
        Consentement consentement = consentementRepository.findById(consentementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Consentement", consentementId));

        AutorisationPartage autorisation = new AutorisationPartage(
                patient,
                proche,
                consentement,
                typeDonnee,
                dateDebut,
                dateFin,
                peutConsulter,
                peutRecevoirNotification
        );
        autorisation.activer(Instant.now());
        return autorisationRepository.save(autorisation);
    }

    public AutorisationPartage autoriserProche(AutoriserProcheCommand command) {
        return autoriserProche(
                command.patientId(), command.procheAidantId(), command.consentementId(),
                command.typeDonnee(), command.dateDebut(), command.dateFin(),
                command.peutConsulter(), command.peutRecevoirNotification()
        );
    }

    public void retirer(UUID consentementId, Instant dateRetrait) {
        Consentement consentement = consentementRepository.findById(consentementId)
                .orElseThrow(() -> new RessourceIntrouvableException("Consentement", consentementId));
        consentement.retirer(dateRetrait);
        autorisationRepository.findAllByConsentementIdAndStatut(
                consentementId,
                StatutAutorisation.ACTIVE
        ).forEach(AutorisationPartage::retirer);
    }
}
