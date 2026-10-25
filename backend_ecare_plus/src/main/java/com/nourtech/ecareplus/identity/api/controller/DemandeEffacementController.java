package com.nourtech.ecareplus.identity.api.controller;
import com.nourtech.ecareplus.identity.application.service.DemandeEffacementService;
import com.nourtech.ecareplus.identity.application.service.DemandeEffacementService.DemandeVue;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor @AccesVerifieParService @RequestMapping("/api/v1/compte/demandes-effacement")
public class DemandeEffacementController {
    private final DemandeEffacementService service;
    public record DemandeCommande(String motif){}
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED) public DemandeVue demander(@RequestBody(required=false)DemandeCommande commande){return service.demander(commande==null?null:commande.motif());}
    @GetMapping("/{id}") public DemandeVue consulter(@PathVariable UUID id){return service.consulter(id);}
}
