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
    @org.springframework.beans.factory.annotation.Autowired
    private com.nourtech.ecareplus.portail.application.PortailService portail;
    public EspacePatientController(EspacePatientService service) { this.service = service; }

    @GetMapping
    public java.util.Map<String,Object> profil(JwtAuthenticationToken token) { service.assurerDossier(patient(token).utilisateurId()); return portail.vue(portail.soi(patient(token).utilisateurId(),false),true); }

    @PatchMapping
    public java.util.Map<String,Object> modifier(JwtAuthenticationToken token,@RequestBody java.util.Map<String,Object> r){return portail.modifierSoi(patient(token).utilisateurId(),r);}

    @PostMapping("/consentement")
    public java.util.Map<String,Object> consentement(JwtAuthenticationToken token,@Valid @RequestBody ConsentementRequest r,jakarta.servlet.http.HttpServletRequest req){return portail.consentir(patient(token).utilisateurId(),r.versionDocument(),Boolean.TRUE.equals(r.cguLues()),Boolean.TRUE.equals(r.confidentialiteLue()),Boolean.TRUE.equals(r.accepte()),req.getRemoteAddr());}

    @GetMapping("/professionnels")
    public java.util.List<java.util.Map<String,Object>> professionnels(JwtAuthenticationToken token){return portail.professionnelsSoi(patient(token).utilisateurId());}

    @GetMapping("/alertes")
    public Page<java.util.Map<String,Object>> alertes(JwtAuthenticationToken token,Pageable p){return portail.alertes(patient(token).utilisateurId(),p);}

    public record ConsentementRequest(@jakarta.validation.constraints.Size(max=40) String versionDocument,Boolean cguLues,Boolean confidentialiteLue,@jakarta.validation.constraints.NotNull Boolean accepte){}

    @GetMapping("/dossier")
    public EspacePatientResponse.Dossier dossier(JwtAuthenticationToken token) { return service.dossier(patient(token).utilisateurId()); }

    @GetMapping("/mesures")
    public Page<java.util.Map<String,Object>> mesures(JwtAuthenticationToken token, Pageable pageable,
       @RequestParam(required=false) com.nourtech.ecareplus.care.domain.enumeration.TypeMesure typeMesure,
       @RequestParam(required=false) java.time.Instant from,@RequestParam(required=false) java.time.Instant to) {
        return portail.mesures(patient(token).utilisateurId(),true,typeMesure,from,to,pageable);
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
