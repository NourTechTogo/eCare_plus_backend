package com.nourtech.ecareplus.mobile.api.controller;
import com.nourtech.ecareplus.auth.infrastructure.security.UtilisateurAuthentifie;
import com.nourtech.ecareplus.mobile.application.service.CommunicationPatientService;
import org.springframework.data.domain.*; import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*; import java.util.UUID;

@RestController @RequestMapping("/api/v1/mobile/me")
public class CommunicationPatientController {
    private final CommunicationPatientService service;
    public CommunicationPatientController(CommunicationPatientService s){service=s;}
    @GetMapping("/rappels") public Page<CommunicationPatientService.RappelMobile> rappels(JwtAuthenticationToken t, Pageable p){return service.rappels(id(t),p);}
    @GetMapping("/notifications") public Page<CommunicationPatientService.NotificationMobile> notifications(JwtAuthenticationToken t, Pageable p){return service.notifications(id(t),p);}
    @PostMapping("/rappels/{id}/lecture") public ResponseEntity<Void> lireRappel(JwtAuthenticationToken t,@PathVariable UUID id){service.lireRappel(id(t),id);return ResponseEntity.noContent().build();}
    @PostMapping("/notifications/{id}/lecture") public ResponseEntity<Void> lireNotification(JwtAuthenticationToken t,@PathVariable UUID id){service.lireNotification(id(t),id);return ResponseEntity.noContent().build();}
    private static UUID id(JwtAuthenticationToken t){return UtilisateurAuthentifie.depuis(t).utilisateurId();}
}
