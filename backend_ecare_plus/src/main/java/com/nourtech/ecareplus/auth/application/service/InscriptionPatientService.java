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

    @org.springframework.beans.factory.annotation.Autowired
    private com.nourtech.ecareplus.consent.infrastructure.persistence.ConsentementRepository consentements;

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
        com.nourtech.ecareplus.portail.application.PortailService.validerPassword(request.motDePasse());
        if (!request.consentementAccepte() || !request.cguLues() || !request.confidentialiteLue()
                || request.versionDocument() == null || request.versionDocument().isBlank()) {
            throw new IllegalArgumentException("L'inscription exige la lecture et l'acceptation des CGU et de la confidentialité");
        }
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
        if(request.consentementAccepte()) {
            if(!request.cguLues()||!request.confidentialiteLue()||request.versionDocument()==null||request.versionDocument().isBlank())throw new IllegalArgumentException("Lecture des CGU et confidentialité requise");
            var attrs=(org.springframework.web.context.request.ServletRequestAttributes)org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            String ip=attrs==null?"non-disponible":attrs.getRequest().getRemoteAddr();
            consentements.save(new com.nourtech.ecareplus.consent.domain.entity.Consentement(patient,"SUIVI_MEDICAL","CGU et confidentialité lues et acceptées personnellement",request.versionDocument(),ip,java.time.Instant.now()));
        }
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
