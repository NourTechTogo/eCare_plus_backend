package com.nourtech.ecareplus.care.api.controller;

import com.nourtech.ecareplus.care.api.dto.request.ModifierObservationRequest;
import com.nourtech.ecareplus.care.api.dto.response.ObservationSuiviResponse;
import com.nourtech.ecareplus.care.api.mapper.CareMapper;
import com.nourtech.ecareplus.care.application.service.ObservationSuiviService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/observations-suivi")
public class ObservationSuiviController {

    private final ObservationSuiviService service;
    private final CareMapper mapper;

    public ObservationSuiviController(ObservationSuiviService service, CareMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/{observationId}")
    public ObservationSuiviResponse consulter(@PathVariable UUID observationId) {
        return mapper.toResponse(service.consulter(observationId));
    }

    @PatchMapping("/{observationId}")
    public ObservationSuiviResponse modifier(
            @PathVariable UUID observationId,
            @Valid @RequestBody ModifierObservationRequest request
    ) {
        return mapper.toResponse(service.modifier(
                observationId, request.contenu(), request.typeObservation(), request.confidentielle()
        ));
    }

    @PatchMapping("/{observationId}/archivage")
    public ObservationSuiviResponse archiver(@PathVariable UUID observationId) {
        return mapper.toResponse(service.archiver(observationId));
    }
}
