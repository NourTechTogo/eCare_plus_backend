package com.nourtech.ecareplus.care.api.controller;

import com.nourtech.ecareplus.care.api.dto.request.ConfirmerPriseRequest;
import com.nourtech.ecareplus.care.api.dto.request.ProgrammerConfirmationPriseRequest;
import com.nourtech.ecareplus.care.api.dto.request.ReporterConfirmationRequest;
import com.nourtech.ecareplus.care.api.dto.request.SignalerOubliRequest;
import com.nourtech.ecareplus.care.api.dto.response.ConfirmationPriseResponse;
import com.nourtech.ecareplus.care.api.mapper.CareMapper;
import com.nourtech.ecareplus.care.application.service.ConfirmationPriseService;
import com.nourtech.ecareplus.care.domain.entity.ConfirmationPrise;
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

import java.time.Instant;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prises-medicament")
public class ConfirmationPriseController {

    private final ConfirmationPriseService service;
    private final CareMapper mapper;

    public ConfirmationPriseController(ConfirmationPriseService service, CareMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/{priseId}/confirmations")
    public ResponseEntity<ConfirmationPriseResponse> programmerConfirmation(
            @PathVariable UUID priseId,
            @Valid @RequestBody ProgrammerConfirmationPriseRequest request
    ) {
        ConfirmationPrise confirmation = service.programmer(mapper.toCommand(priseId, request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(confirmation.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(confirmation));
    }

    @GetMapping("/confirmations/{confirmationId}")
    public ConfirmationPriseResponse consulter(@PathVariable UUID confirmationId) {
        return mapper.toResponse(service.consulter(confirmationId));
    }

    @PatchMapping("/confirmations/{confirmationId}/confirmation")
    public ConfirmationPriseResponse confirmer(
            @PathVariable UUID confirmationId,
            @Valid @RequestBody ConfirmerPriseRequest request
    ) {
        return mapper.toResponse(service.confirmer(confirmationId, request.commentaire(), Instant.now()));
    }

    @PatchMapping("/confirmations/{confirmationId}/oubli")
    public ConfirmationPriseResponse signalerOubli(
            @PathVariable UUID confirmationId,
            @Valid @RequestBody SignalerOubliRequest request
    ) {
        return mapper.toResponse(service.signalerOubli(confirmationId, request.commentaire()));
    }

    @PatchMapping("/confirmations/{confirmationId}/report")
    public ConfirmationPriseResponse reporter(
            @PathVariable UUID confirmationId,
            @Valid @RequestBody ReporterConfirmationRequest request
    ) {
        return mapper.toResponse(service.reporter(confirmationId, request.nouvelleDatePrevue()));
    }
}
