package com.nourtech.ecareplus.care.api.controller;

import com.nourtech.ecareplus.care.api.dto.request.ModifierPathologieRequest;
import com.nourtech.ecareplus.care.api.dto.response.PathologieResponse;
import com.nourtech.ecareplus.care.api.mapper.CareMapper;
import com.nourtech.ecareplus.care.application.service.PathologieService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pathologies")
public class PathologieController {

    private final PathologieService service;
    private final CareMapper mapper;

    public PathologieController(PathologieService service, CareMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/{pathologieId}")
    public PathologieResponse consulter(@PathVariable UUID pathologieId) {
        return mapper.toResponse(service.consulter(pathologieId));
    }

    @PatchMapping("/{pathologieId}")
    public PathologieResponse modifier(
            @PathVariable UUID pathologieId,
            @Valid @RequestBody ModifierPathologieRequest request
    ) {
        return mapper.toResponse(service.modifier(
                pathologieId, request.nom(), request.code(), request.description(), request.niveauRisque()
        ));
    }

    @PatchMapping("/{pathologieId}/desactivation")
    public PathologieResponse desactiver(@PathVariable UUID pathologieId) {
        return mapper.toResponse(service.desactiver(pathologieId));
    }
}
