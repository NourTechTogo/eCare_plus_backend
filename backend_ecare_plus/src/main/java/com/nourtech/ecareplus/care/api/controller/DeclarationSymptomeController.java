package com.nourtech.ecareplus.care.api.controller;

import com.nourtech.ecareplus.care.api.dto.request.DeclarerSymptomeRequest;
import com.nourtech.ecareplus.care.api.dto.request.ModifierDeclarationSymptomeRequest;
import com.nourtech.ecareplus.care.api.dto.response.DeclarationSymptomeResponse;
import com.nourtech.ecareplus.care.api.mapper.CareMapper;
import com.nourtech.ecareplus.care.application.service.DeclarationSymptomeService;
import com.nourtech.ecareplus.care.domain.entity.DeclarationSymptome;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/v1/declarations-symptomes")
public class DeclarationSymptomeController {

    private final DeclarationSymptomeService service;
    private final CareMapper mapper;

    public DeclarationSymptomeController(DeclarationSymptomeService service, CareMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<DeclarationSymptomeResponse> declarer(@Valid @RequestBody DeclarerSymptomeRequest request) {
        DeclarationSymptome declaration = service.declarer(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(declaration.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(declaration));
    }

    @GetMapping("/{declarationId}")
    public DeclarationSymptomeResponse consulter(@PathVariable UUID declarationId) {
        return mapper.toResponse(service.consulter(declarationId));
    }

    @PatchMapping("/{declarationId}")
    public DeclarationSymptomeResponse modifier(
            @PathVariable UUID declarationId,
            @Valid @RequestBody ModifierDeclarationSymptomeRequest request
    ) {
        return mapper.toResponse(service.modifier(
                declarationId, request.symptome(), request.intensite(), request.description(), request.persistant()
        ));
    }

    @PatchMapping("/{declarationId}/transmission")
    public DeclarationSymptomeResponse transmettre(@PathVariable UUID declarationId) {
        return mapper.toResponse(service.transmettre(declarationId));
    }

    @PatchMapping("/{declarationId}/evaluation")
    public DeclarationSymptomeResponse marquerCommeEvalue(@PathVariable UUID declarationId) {
        return mapper.toResponse(service.marquerCommeEvalue(declarationId));
    }
}
