package com.nourtech.ecareplus.sync.api.controller;

import com.nourtech.ecareplus.sync.api.dto.request.RecevoirSynchronisationRequest;
import com.nourtech.ecareplus.sync.api.dto.response.ElementSynchronisationResponse;
import com.nourtech.ecareplus.sync.api.mapper.SyncMapper;
import com.nourtech.ecareplus.sync.application.service.SynchronisationService;
import com.nourtech.ecareplus.sync.domain.entity.ElementSynchronisation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/synchronisations")
public class SynchronisationController {

    private final SynchronisationService service;
    private final SyncMapper mapper;

    public SynchronisationController(SynchronisationService service, SyncMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ElementSynchronisationResponse> recevoir(
            @Valid @RequestBody RecevoirSynchronisationRequest request
    ) {
        ElementSynchronisation element = service.recevoir(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(element.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(element));
    }
}
