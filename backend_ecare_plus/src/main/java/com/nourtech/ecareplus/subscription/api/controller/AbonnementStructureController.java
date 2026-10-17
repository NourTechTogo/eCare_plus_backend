package com.nourtech.ecareplus.subscription.api.controller;

import com.nourtech.ecareplus.subscription.api.dto.request.CreerAbonnementRequest;
import com.nourtech.ecareplus.subscription.api.dto.response.AbonnementStructureResponse;
import com.nourtech.ecareplus.subscription.api.mapper.SubscriptionMapper;
import com.nourtech.ecareplus.subscription.application.service.GestionAbonnementService;
import com.nourtech.ecareplus.subscription.domain.entity.AbonnementStructure;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/abonnements-structures")
public class AbonnementStructureController {

    private final GestionAbonnementService service;
    private final SubscriptionMapper mapper;

    public AbonnementStructureController(GestionAbonnementService service, SubscriptionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<AbonnementStructureResponse> creer(
            @Valid @RequestBody CreerAbonnementRequest request
    ) {
        AbonnementStructure abonnement = service.creer(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(abonnement.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(abonnement));
    }

    @PatchMapping("/{abonnementId}/activation")
    public AbonnementStructureResponse activer(@PathVariable UUID abonnementId) {
        return mapper.toResponse(service.activer(abonnementId));
    }
}
