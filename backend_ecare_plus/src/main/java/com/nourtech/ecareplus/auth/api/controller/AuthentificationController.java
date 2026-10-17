package com.nourtech.ecareplus.auth.api.controller;

import com.nourtech.ecareplus.auth.api.dto.InscriptionPatientRequest;
import com.nourtech.ecareplus.auth.api.dto.ProfilAuthentifieResponse;
import com.nourtech.ecareplus.auth.api.dto.ConnexionRequest;
import com.nourtech.ecareplus.auth.api.dto.SessionResponse;
import com.nourtech.ecareplus.auth.application.service.AuthentificationService;
import com.nourtech.ecareplus.auth.application.service.InscriptionPatientService;
import com.nourtech.ecareplus.auth.application.service.ReinitialisationMotDePasseService;
import com.nourtech.ecareplus.auth.api.dto.DemanderOtpRequest;
import com.nourtech.ecareplus.auth.api.dto.VerifierOtpRequest;
import com.nourtech.ecareplus.auth.api.dto.ReinitialiserMotDePasseRequest;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthentificationController {

    private final InscriptionPatientService inscriptionPatientService;
    private final AuthentificationService authentificationService;
    private final ReinitialisationMotDePasseService reinitialisation;

    public AuthentificationController(
            InscriptionPatientService inscriptionPatientService,
            AuthentificationService authentificationService,
            ReinitialisationMotDePasseService reinitialisation
    ) {
        this.inscriptionPatientService = inscriptionPatientService;
        this.authentificationService = authentificationService;
        this.reinitialisation = reinitialisation;
    }

    @PostMapping("/inscription")
    public ResponseEntity<ProfilAuthentifieResponse> inscrire(
            @Valid @RequestBody InscriptionPatientRequest request
    ) {
        Patient patient = inscriptionPatientService.inscrire(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/patients/{id}")
                .buildAndExpand(patient.getId())
                .toUri();
        return ResponseEntity.created(location).body(ProfilAuthentifieResponse.depuis(patient));
    }

    @PostMapping("/connexion")
    public SessionResponse connecter(@Valid @RequestBody ConnexionRequest request) {
        return authentificationService.connecter(request);
    }

    @PostMapping("/mot-de-passe/otp")
    public ResponseEntity<Void> demanderOtp(@Valid @RequestBody DemanderOtpRequest request) {
        reinitialisation.demander(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/mot-de-passe/otp/verifier")
    public java.util.Map<String, String> verifierOtp(@Valid @RequestBody VerifierOtpRequest request) {
        return java.util.Map.of("jetonReinitialisation", reinitialisation.verifier(request.email(), request.code()));
    }

    @PostMapping("/mot-de-passe/reinitialiser")
    public ResponseEntity<Void> reinitialiser(@Valid @RequestBody ReinitialiserMotDePasseRequest request) {
        reinitialisation.reinitialiser(request.jeton(), request.nouveauMotDePasse());
        return ResponseEntity.noContent().build();
    }
}
