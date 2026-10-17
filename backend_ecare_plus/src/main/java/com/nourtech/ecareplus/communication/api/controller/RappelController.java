package com.nourtech.ecareplus.communication.api.controller;

import com.nourtech.ecareplus.communication.api.dto.request.ModifierRappelRequest;
import com.nourtech.ecareplus.communication.api.dto.request.ProgrammerRappelRequest;
import com.nourtech.ecareplus.communication.api.dto.request.ReporterRappelRequest;
import com.nourtech.ecareplus.communication.api.dto.response.RappelResponse;
import com.nourtech.ecareplus.communication.api.mapper.CommunicationMapper;
import com.nourtech.ecareplus.communication.application.service.RappelService;
import com.nourtech.ecareplus.communication.domain.entity.Rappel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutRappel;

@RestController
@RequestMapping("/api/v1/rappels")
public class RappelController {

    private final RappelService service;
    private final CommunicationMapper mapper;

    public RappelController(RappelService service, CommunicationMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<RappelResponse> programmer(@Valid @RequestBody ProgrammerRappelRequest request) {
        Rappel rappel = service.programmer(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(rappel.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(rappel));
    }

    @GetMapping("/{rappelId}")
    public RappelResponse consulter(@PathVariable UUID rappelId) {
        return mapper.toResponse(service.consulter(rappelId));
    }

    @PatchMapping("/{rappelId}")
    public RappelResponse modifier(
            @PathVariable UUID rappelId,
            @Valid @RequestBody ModifierRappelRequest request
    ) {
        return mapper.toResponse(service.modifier(
                rappelId, request.titre(), request.message(), request.dateHeurePrevue(), request.frequence(),
                request.canal(), request.dateFin()
        ));
    }

    @PatchMapping("/{rappelId}/envoi")
    public RappelResponse envoyer(@PathVariable UUID rappelId) {
        return mapper.toResponse(service.envoyer(rappelId));
    }

    @PatchMapping("/{rappelId}/echec")
    public RappelResponse marquerEnEchec(@PathVariable UUID rappelId) {
        return mapper.toResponse(service.marquerEnEchec(rappelId));
    }

    @PatchMapping("/{rappelId}/report")
    public RappelResponse reporter(
            @PathVariable UUID rappelId,
            @Valid @RequestBody ReporterRappelRequest request
    ) {
        return mapper.toResponse(service.reporter(rappelId, request.nouvelleDateHeurePrevue()));
    }

    @PatchMapping("/{rappelId}/annulation")
    public RappelResponse annuler(@PathVariable UUID rappelId) {
        return mapper.toResponse(service.annuler(rappelId));
    }

    @PatchMapping("/{rappelId}/lecture")
    public RappelResponse marquerCommeLu(@PathVariable UUID rappelId) {
        return mapper.toResponse(service.marquerCommeLu(rappelId));
    }

    @GetMapping("/patients/{patientId}")
    public List<RappelResponse> listerPourPatient(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "PLANIFIE") StatutRappel statut
    ) {
        return service.listerPourPatient(patientId, statut).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/echeances")
    public List<RappelResponse> listerEcheances(@RequestParam Instant jusqua) {
        return service.listerEcheances(jusqua).stream().map(mapper::toResponse).toList();
    }
}
