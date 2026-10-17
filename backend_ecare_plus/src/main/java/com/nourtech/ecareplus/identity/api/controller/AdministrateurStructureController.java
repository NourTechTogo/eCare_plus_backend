package com.nourtech.ecareplus.identity.api.controller;

import com.nourtech.ecareplus.identity.api.dto.request.CreerAdministrateurStructureRequest;
import com.nourtech.ecareplus.identity.api.dto.response.AdministrateurStructureResponse;
import com.nourtech.ecareplus.identity.api.mapper.IdentityMapper;
import com.nourtech.ecareplus.identity.application.service.GestionAdministrateurStructureService;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.identity.domain.entity.AdministrateurStructure;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/administrateurs-structure")
public class AdministrateurStructureController {

    private final GestionAdministrateurStructureService service;
    private final MotDePasseService motDePasseService;
    private final IdentityMapper mapper;

    public AdministrateurStructureController(
            GestionAdministrateurStructureService service,
            MotDePasseService motDePasseService,
            IdentityMapper mapper
    ) {
        this.service = service;
        this.motDePasseService = motDePasseService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<AdministrateurStructureResponse> creer(
            @Valid @RequestBody CreerAdministrateurStructureRequest request
    ) {
        String hash = motDePasseService.hacher(request.motDePasse());
        AdministrateurStructure administrateur = service.creer(mapper.toCommand(request, hash));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(administrateur.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(administrateur));
    }
}
