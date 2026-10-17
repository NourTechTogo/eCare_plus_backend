package com.nourtech.ecareplus.care.api.controller;

import com.nourtech.ecareplus.care.api.dto.request.ModifierTraitementRequest;
import com.nourtech.ecareplus.care.api.dto.response.TraitementResponse;
import com.nourtech.ecareplus.care.api.mapper.CareMapper;
import com.nourtech.ecareplus.care.application.service.TraitementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/traitements")
public class TraitementController {

    private final TraitementService service;
    private final CareMapper mapper;

    public TraitementController(TraitementService service, CareMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/{traitementId}")
    public TraitementResponse consulter(@PathVariable UUID traitementId) {
        return mapper.toResponse(service.consulter(traitementId));
    }

    @PatchMapping("/{traitementId}")
    public TraitementResponse modifier(
            @PathVariable UUID traitementId,
            @Valid @RequestBody ModifierTraitementRequest request
    ) {
        return mapper.toResponse(service.modifier(
                traitementId, request.libelle(), request.description(), request.dateFin(), request.consignesGenerales()
        ));
    }

    @PatchMapping("/{traitementId}/suspension")
    public TraitementResponse suspendre(@PathVariable UUID traitementId) {
        return mapper.toResponse(service.suspendre(traitementId));
    }

    @PatchMapping("/{traitementId}/terminaison")
    public TraitementResponse terminer(@PathVariable UUID traitementId) {
        return mapper.toResponse(service.terminer(traitementId));
    }
}
