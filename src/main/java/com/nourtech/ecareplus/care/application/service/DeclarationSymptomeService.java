package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.application.command.DeclarerSymptomeCommand;
import com.nourtech.ecareplus.care.domain.entity.DeclarationSymptome;
import com.nourtech.ecareplus.care.infrastructure.persistence.DeclarationSymptomeRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class DeclarationSymptomeService {

    private final DeclarationSymptomeRepository declarationRepository;
    private final PatientRepository patientRepository;

    public DeclarationSymptomeService(
            DeclarationSymptomeRepository declarationRepository,
            PatientRepository patientRepository
    ) {
        this.declarationRepository = declarationRepository;
        this.patientRepository = patientRepository;
    }

    public DeclarationSymptome declarer(DeclarerSymptomeCommand commande) {
        Patient patient = patientRepository.findById(commande.patientId())
                .orElseThrow(() -> new RessourceIntrouvableException("Patient", commande.patientId()));

        return declarationRepository.save(new DeclarationSymptome(
                patient,
                commande.symptome(),
                commande.intensite(),
                commande.description(),
                commande.dateApparition(),
                commande.persistant()
        ));
    }

    public DeclarationSymptome modifier(UUID declarationId, String symptome, String intensite, String description, boolean persistant) {
        DeclarationSymptome declaration = charger(declarationId);
        declaration.modifierDeclaration(symptome, intensite, description, persistant);
        return declaration;
    }

    public DeclarationSymptome transmettre(UUID declarationId) {
        DeclarationSymptome declaration = charger(declarationId);
        declaration.transmettreAuProfessionnel();
        return declaration;
    }

    public DeclarationSymptome marquerCommeEvalue(UUID declarationId) {
        DeclarationSymptome declaration = charger(declarationId);
        declaration.marquerCommeEvalue();
        return declaration;
    }

    public DeclarationSymptome consulter(UUID declarationId) {
        return charger(declarationId);
    }

    private DeclarationSymptome charger(UUID declarationId) {
        return declarationRepository.findById(declarationId)
                .orElseThrow(() -> new RessourceIntrouvableException("DeclarationSymptome", declarationId));
    }
}
