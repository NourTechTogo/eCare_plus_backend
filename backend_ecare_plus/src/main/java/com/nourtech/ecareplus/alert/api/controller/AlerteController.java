package com.nourtech.ecareplus.alert.api.controller;

import com.nourtech.ecareplus.alert.api.dto.request.AffecterAlerteRequest;
import com.nourtech.ecareplus.alert.api.dto.request.CloturerAlerteRequest;
import com.nourtech.ecareplus.alert.api.dto.request.CloturerInterventionRequest;
import com.nourtech.ecareplus.alert.api.dto.request.DefinirActionSuivanteRequest;
import com.nourtech.ecareplus.alert.api.dto.request.EnregistrerInterventionRequest;
import com.nourtech.ecareplus.alert.api.dto.request.GenererAlerteRequest;
import com.nourtech.ecareplus.alert.api.dto.request.ModifierInterventionRequest;
import com.nourtech.ecareplus.alert.api.dto.request.TransmettreAlerteRequest;
import com.nourtech.ecareplus.alert.api.dto.response.AlerteResponse;
import com.nourtech.ecareplus.alert.api.dto.response.InterventionSuiviResponse;
import com.nourtech.ecareplus.alert.api.mapper.AlertMapper;
import com.nourtech.ecareplus.alert.application.service.AlerteService;
import com.nourtech.ecareplus.alert.application.service.InterventionSuiviService;
import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.domain.entity.InterventionSuivi;
import com.nourtech.ecareplus.alert.domain.enumeration.StatutAlerte;
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

@RestController
@RequestMapping("/api/v1/alertes")
public class AlerteController {

    private final AlerteService alerteService;
    private final InterventionSuiviService interventionService;
    private final AlertMapper mapper;

    public AlerteController(
            AlerteService alerteService,
            InterventionSuiviService interventionService,
            AlertMapper mapper
    ) {
        this.alerteService = alerteService;
        this.interventionService = interventionService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<AlerteResponse> generer(@Valid @RequestBody GenererAlerteRequest request) {
        Alerte alerte = alerteService.generer(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(alerte.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(alerte));
    }

    @PostMapping("/{alerteId}/interventions")
    public ResponseEntity<InterventionSuiviResponse> enregistrerIntervention(
            @PathVariable UUID alerteId,
            @Valid @RequestBody EnregistrerInterventionRequest request
    ) {
        InterventionSuivi intervention = interventionService.enregistrer(mapper.toCommand(alerteId, request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(intervention.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(intervention));
    }

    @GetMapping("/{alerteId}")
    public AlerteResponse consulter(@PathVariable UUID alerteId) {
        return mapper.toResponse(alerteService.consulter(alerteId));
    }

    @GetMapping("/patients/{patientId}")
    public List<AlerteResponse> listerPourPatient(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "NOUVELLE") StatutAlerte statut
    ) {
        return alerteService.listerPourPatient(patientId, statut).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/interventions/{interventionId}")
    public InterventionSuiviResponse consulterIntervention(@PathVariable UUID interventionId) {
        return mapper.toResponse(interventionService.consulter(interventionId));
    }

    @PatchMapping("/{alerteId}/affectation")
    public AlerteResponse affecter(
            @PathVariable UUID alerteId,
            @Valid @RequestBody AffecterAlerteRequest request
    ) {
        return mapper.toResponse(alerteService.affecter(alerteId, request.professionnelId()));
    }

    @PatchMapping("/{alerteId}/prise-en-charge")
    public AlerteResponse prendreEnCharge(@PathVariable UUID alerteId) {
        return mapper.toResponse(alerteService.prendreEnCharge(alerteId, Instant.now()));
    }

    @PatchMapping("/{alerteId}/transmission")
    public AlerteResponse transmettre(
            @PathVariable UUID alerteId,
            @Valid @RequestBody TransmettreAlerteRequest request
    ) {
        return mapper.toResponse(alerteService.transmettre(alerteId, request.professionnelId()));
    }

    @PatchMapping("/{alerteId}/cloture")
    public AlerteResponse cloturer(
            @PathVariable UUID alerteId,
            @Valid @RequestBody CloturerAlerteRequest request
    ) {
        return mapper.toResponse(alerteService.cloturer(alerteId, request.motifCloture(), Instant.now()));
    }

    @PatchMapping("/{alerteId}/reouverture")
    public AlerteResponse rouvrir(@PathVariable UUID alerteId) {
        return mapper.toResponse(alerteService.rouvrir(alerteId));
    }

    @PatchMapping("/interventions/{interventionId}")
    public InterventionSuiviResponse modifierIntervention(
            @PathVariable UUID interventionId,
            @Valid @RequestBody ModifierInterventionRequest request
    ) {
        return mapper.toResponse(interventionService.modifier(interventionId, request.description(), request.resultat()));
    }

    @PatchMapping("/interventions/{interventionId}/action-suivante")
    public InterventionSuiviResponse definirActionSuivante(
            @PathVariable UUID interventionId,
            @Valid @RequestBody DefinirActionSuivanteRequest request
    ) {
        return mapper.toResponse(interventionService.definirActionSuivante(
                interventionId, request.actionSuivante(), request.dateProchaineAction()
        ));
    }

    @PatchMapping("/interventions/{interventionId}/cloture")
    public InterventionSuiviResponse cloturerIntervention(
            @PathVariable UUID interventionId,
            @Valid @RequestBody CloturerInterventionRequest request
    ) {
        return mapper.toResponse(interventionService.cloturer(interventionId, request.resultatFinal()));
    }
}
