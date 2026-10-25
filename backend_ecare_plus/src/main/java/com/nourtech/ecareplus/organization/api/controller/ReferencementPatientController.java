package com.nourtech.ecareplus.organization.api.controller;
import com.nourtech.ecareplus.organization.application.service.ReferencementPatientService;
import com.nourtech.ecareplus.organization.application.service.ReferencementPatientService.*;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequiredArgsConstructor @AccesVerifieParService @RequestMapping("/api/v1")
public class ReferencementPatientController {
    private final ReferencementPatientService service;
    public record ResolutionCommande(String jeton){}
    @PostMapping("/mobile/me/referencements") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public JetonVue generer(){return service.generer();}
    @PostMapping("/portail/referencements/resolution") public ResolutionVue resoudre(@RequestBody ResolutionCommande commande){return service.resoudre(commande.jeton());}
    @PostMapping("/portail/referencements") public Map<String,Object> referencer(@RequestBody ReferenceCommande commande){return service.referencer(commande);}
}
