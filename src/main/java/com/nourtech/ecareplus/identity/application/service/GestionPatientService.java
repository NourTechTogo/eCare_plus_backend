package com.nourtech.ecareplus.identity.application.service;

import com.nourtech.ecareplus.identity.application.command.CreerPatientCommand;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class GestionPatientService {

    private final PatientRepository patientRepository;
    private final VerificationUniciteCompteService uniciteCompteService;

    public GestionPatientService(
            PatientRepository patientRepository,
            VerificationUniciteCompteService uniciteCompteService
    ) {
        this.patientRepository = patientRepository;
        this.uniciteCompteService = uniciteCompteService;
    }

    public Patient creer(CreerPatientCommand commande) {
        if (patientRepository.existsByNumeroPatient(commande.numeroPatient())) {
            throw new ConflitMetierException("Ce numéro patient existe déjà");
        }
        uniciteCompteService.verifier(commande.email(), commande.telephone());
        return patientRepository.save(new Patient(
                commande.numeroPatient(),
                commande.nom(),
                commande.prenoms(),
                commande.sexe(),
                commande.dateNaissance(),
                commande.telephone(),
                commande.email(),
                commande.motDePasseHash(),
                commande.languePreferee()
        ));
    }

    @Transactional(readOnly = true)
    public Patient consulter(UUID patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new RessourceIntrouvableException("Patient", patientId));
    }
}
