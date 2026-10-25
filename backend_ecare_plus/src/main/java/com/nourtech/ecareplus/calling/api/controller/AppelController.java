package com.nourtech.ecareplus.calling.api.controller;
import com.nourtech.ecareplus.calling.application.service.*;
import com.nourtech.ecareplus.calling.application.service.AppelTransactions.AppelVue;
import com.nourtech.ecareplus.calling.application.port.FournisseurAppel;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor @AccesVerifieParService @RequestMapping("/api/v1")
public class AppelController {
    private final AppelService service;private final AccesPatientService acces;
    @PostMapping("/mobile/conversations/{conversationId}/appels") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public AppelVue creer(@PathVariable UUID conversationId,@RequestBody AppelService.CreationAppel commande){return service.creer(acces.acteurId(),conversationId,commande);}
    @GetMapping("/mobile/conversations/{conversationId}/appels") public Page<AppelVue> lister(@PathVariable UUID conversationId,Pageable page){return service.historique(acces.acteurId(),conversationId,page);}
    @PostMapping("/appels/{id}/acceptation") public AppelVue accepter(@PathVariable UUID id){return service.action(acces.acteurId(),id,"ACCEPTER");}
    @PostMapping("/appels/{id}/refus") public AppelVue refuser(@PathVariable UUID id){return service.action(acces.acteurId(),id,"REFUSER");}
    @PostMapping("/appels/{id}/fin") public AppelVue terminer(@PathVariable UUID id){return service.action(acces.acteurId(),id,"FIN");}
    @GetMapping("/appels/{id}/connexion") public FournisseurAppel.AccesAppel connexion(@PathVariable UUID id){return service.connexion(acces.acteurId(),id);}
}
