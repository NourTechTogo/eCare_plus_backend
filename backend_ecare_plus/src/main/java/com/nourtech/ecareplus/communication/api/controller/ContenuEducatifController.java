package com.nourtech.ecareplus.communication.api.controller;

import com.nourtech.ecareplus.communication.api.dto.request.CreerContenuEducatifRequest;
import com.nourtech.ecareplus.communication.api.dto.request.ModifierContenuEducatifRequest;
import com.nourtech.ecareplus.communication.api.dto.response.ContenuEducatifResponse;
import com.nourtech.ecareplus.communication.api.mapper.CommunicationMapper;
import com.nourtech.ecareplus.communication.application.service.ContenuEducatifService;
import com.nourtech.ecareplus.communication.domain.entity.ContenuEducatif;
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
@RequestMapping("/api/v1/contenus-educatifs")
public class ContenuEducatifController {

    private final ContenuEducatifService service;
    private final CommunicationMapper mapper;

    public ContenuEducatifController(ContenuEducatifService service, CommunicationMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ContenuEducatifResponse> creer(@Valid @RequestBody CreerContenuEducatifRequest request) {
        ContenuEducatif contenu = service.creer(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(contenu.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(contenu));
    }

    @GetMapping("/{contenuId}")
    public ContenuEducatifResponse consulter(@PathVariable UUID contenuId) {
        return mapper.toResponse(service.consulter(contenuId));
    }

    @PatchMapping("/{contenuId}")
    public ContenuEducatifResponse modifier(
            @PathVariable UUID contenuId,
            @Valid @RequestBody ModifierContenuEducatifRequest request
    ) {
        return mapper.toResponse(service.modifier(
                contenuId, request.titre(), request.resume(), request.contenu(), request.urlMedia()
        ));
    }

    @PatchMapping("/{contenuId}/validation")
    public ContenuEducatifResponse valider(@PathVariable UUID contenuId) {
        return mapper.toResponse(service.valider(contenuId));
    }

    @PatchMapping("/{contenuId}/publication")
    public ContenuEducatifResponse publier(@PathVariable UUID contenuId) {
        return mapper.toResponse(service.publier(contenuId, Instant.now()));
    }

    @PatchMapping("/{contenuId}/archivage")
    public ContenuEducatifResponse archiver(@PathVariable UUID contenuId) {
        return mapper.toResponse(service.archiver(contenuId));
    }
}
