package com.nourtech.ecareplus.organization.api.controller;

import com.nourtech.ecareplus.organization.api.dto.request.AffecterPatientRequest;
import com.nourtech.ecareplus.organization.api.dto.request.TerminerAffectationRequest;
import com.nourtech.ecareplus.organization.api.dto.response.AffectationPatientResponse;
import com.nourtech.ecareplus.organization.api.mapper.OrganizationMapper;
import com.nourtech.ecareplus.organization.application.service.AffectationPatientService;
import com.nourtech.ecareplus.organization.domain.entity.AffectationPatient;
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
@RequestMapping("/api/v1/affectations-patients")
public class AffectationPatientController {

    private final AffectationPatientService service;
    private final OrganizationMapper mapper;

    public AffectationPatientController(AffectationPatientService service, OrganizationMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<AffectationPatientResponse> affecter(
            @Valid @RequestBody AffecterPatientRequest request
    ) {
        AffectationPatient affectation = service.affecter(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(affectation.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(affectation));
    }

    @PatchMapping("/{affectationId}/terminaison")
    public ResponseEntity<Void> terminer(
            @PathVariable UUID affectationId,
            @Valid @RequestBody TerminerAffectationRequest request
    ) {
        service.terminer(affectationId, request.dateFin());
        return ResponseEntity.noContent().build();
    }
}
