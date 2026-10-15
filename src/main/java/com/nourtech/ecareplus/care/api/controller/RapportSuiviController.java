package com.nourtech.ecareplus.care.api.controller;

import com.nourtech.ecareplus.care.api.dto.request.MarquerRapportGenereRequest;
import com.nourtech.ecareplus.care.api.dto.response.RapportSuiviResponse;
import com.nourtech.ecareplus.care.api.mapper.CareMapper;
import com.nourtech.ecareplus.care.application.service.RapportSuiviService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rapports-suivi")
public class RapportSuiviController {

    private final RapportSuiviService service;
    private final CareMapper mapper;

    public RapportSuiviController(RapportSuiviService service, CareMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/{rapportId}")
    public RapportSuiviResponse consulter(@PathVariable UUID rapportId) {
        return mapper.toResponse(service.consulter(rapportId));
    }

    @PatchMapping("/{rapportId}/generation")
    public RapportSuiviResponse marquerGenere(
            @PathVariable UUID rapportId,
            @Valid @RequestBody MarquerRapportGenereRequest request
    ) {
        return mapper.toResponse(service.marquerGenere(rapportId, request.cheminFichier()));
    }

    @PatchMapping("/{rapportId}/archivage")
    public RapportSuiviResponse archiver(@PathVariable UUID rapportId) {
        return mapper.toResponse(service.archiver(rapportId));
    }
}
