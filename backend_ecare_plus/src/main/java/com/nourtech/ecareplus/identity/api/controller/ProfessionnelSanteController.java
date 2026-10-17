package com.nourtech.ecareplus.identity.api.controller;

import com.nourtech.ecareplus.identity.api.dto.request.CreerInfirmierRequest;
import com.nourtech.ecareplus.identity.api.dto.request.CreerMedecinRequest;
import com.nourtech.ecareplus.identity.api.dto.response.InfirmierResponse;
import com.nourtech.ecareplus.identity.api.dto.response.MedecinResponse;
import com.nourtech.ecareplus.identity.api.mapper.IdentityMapper;
import com.nourtech.ecareplus.identity.application.service.GestionProfessionnelSanteService;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.identity.domain.entity.Infirmier;
import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/professionnels-sante")
public class ProfessionnelSanteController {

    private final GestionProfessionnelSanteService service;
    private final MotDePasseService motDePasseService;
    private final IdentityMapper mapper;

    public ProfessionnelSanteController(
            GestionProfessionnelSanteService service,
            MotDePasseService motDePasseService,
            IdentityMapper mapper
    ) {
        this.service = service;
        this.motDePasseService = motDePasseService;
        this.mapper = mapper;
    }

    @PostMapping("/medecins")
    public ResponseEntity<MedecinResponse> creerMedecin(@Valid @RequestBody CreerMedecinRequest request) {
        String hash = motDePasseService.hacher(request.motDePasse());
        Medecin medecin = service.creerMedecin(mapper.toCommand(request, hash));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(medecin.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(medecin));
    }

    @PostMapping("/infirmiers")
    public ResponseEntity<InfirmierResponse> creerInfirmier(@Valid @RequestBody CreerInfirmierRequest request) {
        String hash = motDePasseService.hacher(request.motDePasse());
        Infirmier infirmier = service.creerInfirmier(mapper.toCommand(request, hash));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(infirmier.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(infirmier));
    }
}
