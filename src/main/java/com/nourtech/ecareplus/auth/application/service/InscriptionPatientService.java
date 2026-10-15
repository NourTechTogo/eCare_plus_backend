package com.nourtech.ecareplus.auth.application.service;

import com.nourtech.ecareplus.auth.api.dto.InscriptionPatientRequest;
import com.nourtech.ecareplus.identity.application.command.CreerPatientCommand;
import com.nourtech.ecareplus.identity.application.service.GestionPatientService;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class InscriptionPatientService {

    private final GestionPatientService gestionPatientService;
    private final MotDePasseService motDePasseService;
    private final PatientRepository patientRepository;

    public InscriptionPatientService(
            GestionPatientService gestionPatientService,
            MotDePasseService motDePasseService,
            PatientRepository patientRepository
    ) {
        this.gestionPatientService = gestionPatientService;
        this.motDePasseService = motDePasseService;
        this.patientRepository = patientRepository;
    }

    public Patient inscrire(InscriptionPatientRequest request) {
        Patient patient = gestionPatientService.creer(new CreerPatientCommand(
                genererNumeroPatient(),
                request.nom(),
                request.prenoms(),
                Sexe.NON_RENSEIGNE,
                null,
                request.telephone(),
                request.email(),
                motDePasseService.hacher(request.motDePasse()),
                "fr"
        ));
        patient.activerCompte();
        return patient;
    }

    private String genererNumeroPatient() {
        String numero;
        do {
            numero = "PAT-" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase(Locale.ROOT);
        } while (patientRepository.existsByNumeroPatient(numero));
        return numero;
    }
}
