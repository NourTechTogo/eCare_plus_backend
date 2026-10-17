package com.nourtech.ecareplus.organization.application.service;

import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.identity.infrastructure.persistence.ProfessionnelSanteRepository;
import com.nourtech.ecareplus.organization.application.command.AffecterPatientCommand;
import com.nourtech.ecareplus.organization.domain.entity.AffectationPatient;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel;
import com.nourtech.ecareplus.organization.domain.enumeration.StatutAffectation;
import com.nourtech.ecareplus.organization.infrastructure.persistence.AffectationPatientRepository;
import com.nourtech.ecareplus.organization.infrastructure.persistence.StructureSanteRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class AffectationPatientService {

    private final AffectationPatientRepository affectationRepository;
    private final PatientRepository patientRepository;
    private final ProfessionnelSanteRepository professionnelRepository;
    private final StructureSanteRepository structureRepository;

    public AffectationPatientService(
            AffectationPatientRepository affectationRepository,
            PatientRepository patientRepository,
            ProfessionnelSanteRepository professionnelRepository,
            StructureSanteRepository structureRepository
    ) {
        this.affectationRepository = affectationRepository;
        this.patientRepository = patientRepository;
        this.professionnelRepository = professionnelRepository;
        this.structureRepository = structureRepository;
    }

    public AffectationPatient affecter(
            UUID patientId,
            UUID professionnelId,
            UUID structureId,
            RoleProfessionnel role,
            LocalDate dateDebut,
            boolean principal
    ) {
        if (affectationRepository.existsByPatientIdAndProfessionnelIdAndStatut(
                patientId,
                professionnelId,
                StatutAffectation.ACTIVE
        )) {
            throw new ConflitMetierException("Une affectation active existe déjà pour ce patient et ce professionnel");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Patient", patientId));
        ProfessionnelSante professionnel = professionnelRepository.findById(professionnelId)
                .orElseThrow(() -> new RessourceIntrouvableException("ProfessionnelSante", professionnelId));
        StructureSante structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new RessourceIntrouvableException("StructureSante", structureId));

        if (principal) {
            affectationRepository.findAllByPatientIdAndStructureIdAndRoleProfessionnelAndStatut(
                    patientId,
                    structureId,
                    role,
                    StatutAffectation.ACTIVE
            ).stream()
                    .filter(AffectationPatient::isProfessionnelPrincipal)
                    .forEach(affectation -> affectation.definirProfessionnelPrincipal(false));
            // Force les mises à jour avant l'insertion protégée par l'index unique partiel.
            affectationRepository.flush();
        }

        return affectationRepository.save(new AffectationPatient(
                patient,
                professionnel,
                structure,
                role,
                dateDebut,
                principal
        ));
    }

    public AffectationPatient affecter(AffecterPatientCommand commande) {
        return affecter(
                commande.patientId(),
                commande.professionnelId(),
                commande.structureId(),
                commande.role(),
                commande.dateDebut(),
                commande.professionnelPrincipal()
        );
    }

    public void terminer(UUID affectationId, LocalDate dateFin) {
        AffectationPatient affectation = affectationRepository.findById(affectationId)
                .orElseThrow(() -> new RessourceIntrouvableException("AffectationPatient", affectationId));
        affectation.terminer(dateFin);
    }
}
