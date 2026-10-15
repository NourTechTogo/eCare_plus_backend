package com.nourtech.ecareplus.care.api.controller;

import com.nourtech.ecareplus.care.api.dto.request.AjouterMedicamentRequest;
import com.nourtech.ecareplus.care.api.dto.request.ModifierMedicamentRequest;
import com.nourtech.ecareplus.care.api.dto.response.MedicamentResponse;
import com.nourtech.ecareplus.care.api.mapper.CareMapper;
import com.nourtech.ecareplus.care.application.service.MedicamentService;
import com.nourtech.ecareplus.care.domain.entity.Medicament;
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
@RequestMapping("/api/v1/medicaments")
public class MedicamentController {

    private final MedicamentService service;
    private final CareMapper mapper;

    public MedicamentController(MedicamentService service, CareMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<MedicamentResponse> ajouter(@Valid @RequestBody AjouterMedicamentRequest request) {
        Medicament medicament = service.ajouter(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(medicament.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(medicament));
    }

    @GetMapping("/{medicamentId}")
    public MedicamentResponse consulter(@PathVariable UUID medicamentId) {
        return mapper.toResponse(service.consulter(medicamentId));
    }

    @PatchMapping("/{medicamentId}")
    public MedicamentResponse modifier(
            @PathVariable UUID medicamentId,
            @Valid @RequestBody ModifierMedicamentRequest request
    ) {
        return mapper.toResponse(service.modifier(
                medicamentId, request.nomCommercial(), request.denomination(), request.forme(),
                request.dosage(), request.uniteDosage()
        ));
    }
}
