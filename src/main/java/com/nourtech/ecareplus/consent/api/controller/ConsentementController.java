package com.nourtech.ecareplus.consent.api.controller;

import com.nourtech.ecareplus.consent.api.dto.request.AccorderConsentementRequest;
import com.nourtech.ecareplus.consent.api.dto.request.AutoriserProcheRequest;
import com.nourtech.ecareplus.consent.api.dto.request.RetirerConsentementRequest;
import com.nourtech.ecareplus.consent.api.dto.response.AutorisationPartageResponse;
import com.nourtech.ecareplus.consent.api.dto.response.ConsentementResponse;
import com.nourtech.ecareplus.consent.api.mapper.ConsentMapper;
import com.nourtech.ecareplus.consent.application.service.GestionConsentementService;
import com.nourtech.ecareplus.consent.domain.entity.AutorisationPartage;
import com.nourtech.ecareplus.consent.domain.entity.Consentement;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ConsentementController {

    private final GestionConsentementService service;
    private final ConsentMapper mapper;

    public ConsentementController(GestionConsentementService service, ConsentMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/consentements")
    public ResponseEntity<ConsentementResponse> accorder(
            @Valid @RequestBody AccorderConsentementRequest request,
            HttpServletRequest servletRequest
    ) {
        Consentement consentement = service.accorder(mapper.toCommand(request, servletRequest.getRemoteAddr()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(consentement.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(consentement));
    }

    @PostMapping("/autorisations-partage")
    public ResponseEntity<AutorisationPartageResponse> autoriserProche(
            @Valid @RequestBody AutoriserProcheRequest request
    ) {
        AutorisationPartage autorisation = service.autoriserProche(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(autorisation.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(autorisation));
    }

    @PatchMapping("/consentements/{consentementId}/retrait")
    public ResponseEntity<Void> retirer(
            @PathVariable UUID consentementId,
            @Valid @RequestBody RetirerConsentementRequest request
    ) {
        service.retirer(consentementId, request.dateRetrait());
        return ResponseEntity.noContent().build();
    }
}
