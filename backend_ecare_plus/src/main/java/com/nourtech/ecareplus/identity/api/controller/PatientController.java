package com.nourtech.ecareplus.identity.api.controller;

import com.nourtech.ecareplus.identity.api.dto.request.CreerPatientRequest;
import com.nourtech.ecareplus.identity.api.dto.response.PatientResponse;
import com.nourtech.ecareplus.identity.api.mapper.IdentityMapper;
import com.nourtech.ecareplus.identity.application.service.GestionPatientService;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
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
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final GestionPatientService service;
    private final MotDePasseService motDePasseService;
    private final IdentityMapper mapper;

    public PatientController(
            GestionPatientService service,
            MotDePasseService motDePasseService,
            IdentityMapper mapper
    ) {
        this.service = service;
        this.motDePasseService = motDePasseService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PatientResponse> creer(@Valid @RequestBody CreerPatientRequest request) {
        String hash = motDePasseService.hacher(request.motDePasse());
        Patient patient = service.creer(mapper.toCommand(request, hash));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(patient.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(patient));
    }

    @GetMapping("/{patientId}")
    public PatientResponse consulter(@PathVariable UUID patientId) {
        return mapper.toResponse(service.consulter(patientId));
    }
}
