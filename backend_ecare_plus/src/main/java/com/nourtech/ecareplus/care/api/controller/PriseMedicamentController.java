package com.nourtech.ecareplus.care.api.controller;

import com.nourtech.ecareplus.care.api.dto.request.ModifierHorairePriseRequest;
import com.nourtech.ecareplus.care.api.dto.request.ProgrammerPriseRequest;
import com.nourtech.ecareplus.care.api.dto.response.PriseMedicamentResponse;
import com.nourtech.ecareplus.care.api.mapper.CareMapper;
import com.nourtech.ecareplus.care.application.service.PriseMedicamentService;
import com.nourtech.ecareplus.care.domain.entity.PriseMedicament;
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
@RequestMapping("/api/v1/traitements")
public class PriseMedicamentController {

    private final PriseMedicamentService priseService;
    private final CareMapper mapper;

    public PriseMedicamentController(PriseMedicamentService priseService, CareMapper mapper) {
        this.priseService = priseService;
        this.mapper = mapper;
    }

    @PostMapping("/{traitementId}/prises")
    public ResponseEntity<PriseMedicamentResponse> programmerPrise(
            @PathVariable UUID traitementId,
            @Valid @RequestBody ProgrammerPriseRequest request
    ) {
        PriseMedicament prise = priseService.programmer(mapper.toCommand(traitementId, request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(prise.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(prise));
    }

    @GetMapping("/prises/{priseId}")
    public PriseMedicamentResponse consulter(@PathVariable UUID priseId) {
        return mapper.toResponse(priseService.consulter(priseId));
    }

    @PatchMapping("/prises/{priseId}/horaire")
    public PriseMedicamentResponse modifierHoraire(
            @PathVariable UUID priseId,
            @Valid @RequestBody ModifierHorairePriseRequest request
    ) {
        return mapper.toResponse(priseService.modifierHoraire(
                priseId, request.heurePrevue(), request.frequence(), request.momentPrise()
        ));
    }

    @PatchMapping("/prises/{priseId}/activation")
    public PriseMedicamentResponse activer(@PathVariable UUID priseId) {
        return mapper.toResponse(priseService.activer(priseId));
    }

    @PatchMapping("/prises/{priseId}/desactivation")
    public PriseMedicamentResponse desactiver(@PathVariable UUID priseId) {
        return mapper.toResponse(priseService.desactiver(priseId));
    }
}
