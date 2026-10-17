package com.nourtech.ecareplus.communication.api.controller;

import com.nourtech.ecareplus.communication.api.dto.request.ProgrammerRendezVousRequest;
import com.nourtech.ecareplus.communication.api.dto.request.ReporterRendezVousRequest;
import com.nourtech.ecareplus.communication.api.dto.response.RendezVousResponse;
import com.nourtech.ecareplus.communication.api.mapper.CommunicationMapper;
import com.nourtech.ecareplus.communication.application.service.RendezVousService;
import com.nourtech.ecareplus.communication.domain.entity.RendezVous;
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
@RequestMapping("/api/v1/rendez-vous")
public class RendezVousController {

    private final RendezVousService service;
    private final CommunicationMapper mapper;

    public RendezVousController(RendezVousService service, CommunicationMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<RendezVousResponse> programmer(@Valid @RequestBody ProgrammerRendezVousRequest request) {
        RendezVous rendezVous = service.programmer(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(rendezVous.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(rendezVous));
    }

    @GetMapping("/{rendezVousId}")
    public RendezVousResponse consulter(@PathVariable UUID rendezVousId) {
        return mapper.toResponse(service.consulter(rendezVousId));
    }

    @PatchMapping("/{rendezVousId}/confirmation")
    public RendezVousResponse confirmer(@PathVariable UUID rendezVousId) {
        return mapper.toResponse(service.confirmer(rendezVousId));
    }

    @PatchMapping("/{rendezVousId}/report")
    public RendezVousResponse reporter(
            @PathVariable UUID rendezVousId,
            @Valid @RequestBody ReporterRendezVousRequest request
    ) {
        return mapper.toResponse(service.reporter(rendezVousId, request.nouvelleDateHeure()));
    }

    @PatchMapping("/{rendezVousId}/annulation")
    public RendezVousResponse annuler(@PathVariable UUID rendezVousId) {
        return mapper.toResponse(service.annuler(rendezVousId));
    }

    @PatchMapping("/{rendezVousId}/realisation")
    public RendezVousResponse marquerCommeRealise(@PathVariable UUID rendezVousId) {
        return mapper.toResponse(service.marquerCommeRealise(rendezVousId));
    }
}
