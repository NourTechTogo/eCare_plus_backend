package com.nourtech.ecareplus.sync.api.controller;
import com.nourtech.ecareplus.sync.application.service.TraitementLotSynchronisationService;
import com.nourtech.ecareplus.sync.application.service.TraitementLotSynchronisationService.*;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor @AccesVerifieParService
public class SynchronisationPatientController {
    private final TraitementLotSynchronisationService service;private final AccesPatientService acces;
    @PostMapping("/api/v1/mobile/me/synchronisations") public LotSynchronisationResultat synchroniser(@RequestBody LotSynchronisationCommande commande){return service.traiter(acces.acteurId(),commande);}
}
