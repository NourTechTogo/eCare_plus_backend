package com.nourtech.ecareplus.care.api.controller;

import com.nourtech.ecareplus.care.api.dto.request.AjouterObservationRequest;
import com.nourtech.ecareplus.care.api.dto.request.AjouterTraitementRequest;
import com.nourtech.ecareplus.care.api.dto.request.DeclarerPathologieRequest;
import com.nourtech.ecareplus.care.api.dto.request.DefinirObjectifRequest;
import com.nourtech.ecareplus.care.api.dto.request.EnregistrerMesureRequest;
import com.nourtech.ecareplus.care.api.dto.request.GenererRapportRequest;
import com.nourtech.ecareplus.care.api.dto.request.OuvrirDossierRequest;
import com.nourtech.ecareplus.care.api.dto.response.DossierSuiviResponse;
import com.nourtech.ecareplus.care.api.dto.response.MesureSanteResponse;
import com.nourtech.ecareplus.care.api.dto.response.ObjectifSuiviResponse;
import com.nourtech.ecareplus.care.api.dto.response.ObservationSuiviResponse;
import com.nourtech.ecareplus.care.api.dto.response.PathologieResponse;
import com.nourtech.ecareplus.care.api.dto.response.RapportSuiviResponse;
import com.nourtech.ecareplus.care.api.dto.response.TraitementResponse;
import com.nourtech.ecareplus.care.api.mapper.CareMapper;
import com.nourtech.ecareplus.care.application.service.DossierSuiviService;
import com.nourtech.ecareplus.care.application.service.MesureSanteService;
import com.nourtech.ecareplus.care.application.service.ObservationSuiviService;
import com.nourtech.ecareplus.care.application.service.PathologieService;
import com.nourtech.ecareplus.care.application.service.RapportSuiviService;
import com.nourtech.ecareplus.care.application.service.TraitementService;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.care.domain.entity.ObjectifSuivi;
import com.nourtech.ecareplus.care.domain.entity.ObservationSuivi;
import com.nourtech.ecareplus.care.domain.entity.Pathologie;
import com.nourtech.ecareplus.care.domain.entity.RapportSuivi;
import com.nourtech.ecareplus.care.domain.entity.Traitement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dossiers-suivi")
public class DossierSuiviController {

    private final DossierSuiviService service;
    private final PathologieService pathologieService;
    private final MesureSanteService mesureSanteService;
    private final TraitementService traitementService;
    private final ObservationSuiviService observationService;
    private final RapportSuiviService rapportService;
    private final CareMapper mapper;

    public DossierSuiviController(
            DossierSuiviService service,
            PathologieService pathologieService,
            MesureSanteService mesureSanteService,
            TraitementService traitementService,
            ObservationSuiviService observationService,
            RapportSuiviService rapportService,
            CareMapper mapper
    ) {
        this.service = service;
        this.pathologieService = pathologieService;
        this.mesureSanteService = mesureSanteService;
        this.traitementService = traitementService;
        this.observationService = observationService;
        this.rapportService = rapportService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<DossierSuiviResponse> ouvrir(@Valid @RequestBody OuvrirDossierRequest request) {
        DossierSuivi dossier = service.ouvrir(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(dossier.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(dossier));
    }

    @GetMapping("/{dossierId}")
    public DossierSuiviResponse consulter(@PathVariable UUID dossierId) {
        return mapper.toResponse(service.consulter(dossierId));
    }

    @PostMapping("/{dossierId}/objectifs")
    public ResponseEntity<ObjectifSuiviResponse> definirObjectif(
            @PathVariable UUID dossierId,
            @Valid @RequestBody DefinirObjectifRequest request
    ) {
        ObjectifSuivi objectif = service.definirObjectif(mapper.toCommand(dossierId, request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(objectif.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(objectif));
    }

    @PostMapping("/{dossierId}/pathologies")
    public ResponseEntity<PathologieResponse> declarerPathologie(
            @PathVariable UUID dossierId,
            @Valid @RequestBody DeclarerPathologieRequest request
    ) {
        Pathologie pathologie = pathologieService.declarer(mapper.toCommand(dossierId, request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(pathologie.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(pathologie));
    }

    @PostMapping("/{dossierId}/mesures")
    public ResponseEntity<MesureSanteResponse> enregistrerMesure(
            @PathVariable UUID dossierId,
            @Valid @RequestBody EnregistrerMesureRequest request
    ) {
        MesureSante mesure = mesureSanteService.enregistrer(mapper.toCommand(dossierId, request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(mesure.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(mesure));
    }

    @PostMapping("/{dossierId}/traitements")
    public ResponseEntity<TraitementResponse> ajouterTraitement(
            @PathVariable UUID dossierId,
            @Valid @RequestBody AjouterTraitementRequest request
    ) {
        Traitement traitement = traitementService.ajouter(mapper.toCommand(dossierId, request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(traitement.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(traitement));
    }

    @PostMapping("/{dossierId}/observations")
    public ResponseEntity<ObservationSuiviResponse> ajouterObservation(
            @PathVariable UUID dossierId,
            @Valid @RequestBody AjouterObservationRequest request
    ) {
        ObservationSuivi observation = observationService.ajouter(mapper.toCommand(dossierId, request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(observation.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(observation));
    }

    @PostMapping("/{dossierId}/rapports")
    public ResponseEntity<RapportSuiviResponse> genererRapport(
            @PathVariable UUID dossierId,
            @Valid @RequestBody GenererRapportRequest request
    ) {
        RapportSuivi rapport = rapportService.generer(mapper.toCommand(dossierId, request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(rapport.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(rapport));
    }
}
