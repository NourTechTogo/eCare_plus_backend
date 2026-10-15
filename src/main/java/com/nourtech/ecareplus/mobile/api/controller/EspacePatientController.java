package com.nourtech.ecareplus.mobile.api.controller;

import com.nourtech.ecareplus.auth.api.dto.ProfilAuthentifieResponse;
import com.nourtech.ecareplus.auth.infrastructure.security.UtilisateurAuthentifie;
import com.nourtech.ecareplus.mobile.api.dto.CreerMesureMobileRequest;
import com.nourtech.ecareplus.mobile.api.dto.EspacePatientResponse;
import com.nourtech.ecareplus.mobile.application.service.EspacePatientService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mobile/me")
public class EspacePatientController {
    private final EspacePatientService service;
    public EspacePatientController(EspacePatientService service) { this.service = service; }

    @GetMapping
    public ProfilAuthentifieResponse profil(JwtAuthenticationToken token) { return service.profil(patient(token).utilisateurId()); }

    @GetMapping("/dossier")
    public EspacePatientResponse.Dossier dossier(JwtAuthenticationToken token) { return service.dossier(patient(token).utilisateurId()); }

    @GetMapping("/mesures")
    public Page<EspacePatientResponse.Mesure> mesures(JwtAuthenticationToken token, Pageable pageable) {
        return service.mesures(patient(token).utilisateurId(), pageable);
    }

    @PostMapping("/mesures")
    @ResponseStatus(HttpStatus.CREATED)
    public EspacePatientResponse.Mesure enregistrer(JwtAuthenticationToken token,
                                                     @Valid @RequestBody CreerMesureMobileRequest request) {
        return service.enregistrer(patient(token).utilisateurId(), request);
    }

    private static UtilisateurAuthentifie patient(JwtAuthenticationToken token) {
        UtilisateurAuthentifie utilisateur = UtilisateurAuthentifie.depuis(token);
        if (!"PATIENT".equals(utilisateur.role())) throw new AccessDeniedException("Espace réservé aux patients");
        return utilisateur;
    }
}
