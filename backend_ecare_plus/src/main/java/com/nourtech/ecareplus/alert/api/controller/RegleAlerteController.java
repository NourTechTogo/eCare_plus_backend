package com.nourtech.ecareplus.alert.api.controller;

import com.nourtech.ecareplus.alert.api.dto.request.CreerRegleAlerteRequest;
import com.nourtech.ecareplus.alert.api.dto.request.ModifierRegleAlerteRequest;
import com.nourtech.ecareplus.alert.api.dto.response.RegleAlerteResponse;
import com.nourtech.ecareplus.alert.api.mapper.AlertMapper;
import com.nourtech.ecareplus.alert.application.service.RegleAlerteService;
import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
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
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/regles-alerte")
public class RegleAlerteController {

    private final RegleAlerteService service;
    private final AlertMapper mapper;

    public RegleAlerteController(RegleAlerteService service, AlertMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<RegleAlerteResponse> creer(@Valid @RequestBody CreerRegleAlerteRequest request) {
        RegleAlerte regle = service.creer(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(regle.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(regle));
    }

    @GetMapping("/{regleId}")
    public RegleAlerteResponse consulter(@PathVariable UUID regleId) {
        return mapper.toResponse(service.consulter(regleId));
    }

    @PatchMapping("/{regleId}")
    public RegleAlerteResponse modifier(
            @PathVariable UUID regleId,
            @Valid @RequestBody ModifierRegleAlerteRequest request
    ) {
        return mapper.toResponse(service.modifier(
                regleId, request.description(), request.operateur(), request.seuilMin(), request.seuilMax(),
                request.nombreOccurrences(), request.periodeObservation()
        ));
    }

    @PatchMapping("/{regleId}/validation")
    public RegleAlerteResponse valider(@PathVariable UUID regleId) {
        return mapper.toResponse(service.valider(regleId, Instant.now()));
    }

    @PatchMapping("/{regleId}/activation")
    public RegleAlerteResponse activer(@PathVariable UUID regleId) {
        return mapper.toResponse(service.activer(regleId));
    }

    @PatchMapping("/{regleId}/desactivation")
    public RegleAlerteResponse desactiver(@PathVariable UUID regleId) {
        return mapper.toResponse(service.desactiver(regleId));
    }
}
