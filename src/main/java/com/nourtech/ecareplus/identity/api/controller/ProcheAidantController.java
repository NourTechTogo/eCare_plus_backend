package com.nourtech.ecareplus.identity.api.controller;

import com.nourtech.ecareplus.identity.api.dto.request.CreerProcheAidantRequest;
import com.nourtech.ecareplus.identity.api.dto.response.ProcheAidantResponse;
import com.nourtech.ecareplus.identity.api.mapper.IdentityMapper;
import com.nourtech.ecareplus.identity.application.service.GestionProcheAidantService;
import com.nourtech.ecareplus.identity.application.service.MotDePasseService;
import com.nourtech.ecareplus.identity.domain.entity.ProcheAidant;
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
@RequestMapping("/api/v1/proches-aidants")
public class ProcheAidantController {

    private final GestionProcheAidantService service;
    private final MotDePasseService motDePasseService;
    private final IdentityMapper mapper;

    public ProcheAidantController(
            GestionProcheAidantService service,
            MotDePasseService motDePasseService,
            IdentityMapper mapper
    ) {
        this.service = service;
        this.motDePasseService = motDePasseService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ProcheAidantResponse> creer(@Valid @RequestBody CreerProcheAidantRequest request) {
        String hash = motDePasseService.hacher(request.motDePasse());
        ProcheAidant proche = service.creer(mapper.toCommand(request, hash));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(proche.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(proche));
    }

    @GetMapping("/{procheAidantId}")
    public ProcheAidantResponse consulter(@PathVariable UUID procheAidantId) {
        return mapper.toResponse(service.consulter(procheAidantId));
    }
}
