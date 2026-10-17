package com.nourtech.ecareplus.organization.api.controller;

import com.nourtech.ecareplus.organization.api.dto.request.CreerStructureSanteRequest;
import com.nourtech.ecareplus.organization.api.dto.response.StructureSanteResponse;
import com.nourtech.ecareplus.organization.api.mapper.OrganizationMapper;
import com.nourtech.ecareplus.organization.application.service.GestionStructureService;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
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
@RequestMapping("/api/v1/structures")
public class StructureSanteController {

    private final GestionStructureService service;
    private final OrganizationMapper mapper;

    public StructureSanteController(GestionStructureService service, OrganizationMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<StructureSanteResponse> creer(@Valid @RequestBody CreerStructureSanteRequest request) {
        StructureSante structure = service.creer(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(structure.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(structure));
    }

    @PatchMapping("/{structureId}/activation")
    public StructureSanteResponse activer(@PathVariable UUID structureId) {
        return mapper.toResponse(service.activer(structureId));
    }

    @PatchMapping("/{structureId}/suspension")
    public StructureSanteResponse suspendre(@PathVariable UUID structureId) {
        return mapper.toResponse(service.suspendre(structureId));
    }
}
