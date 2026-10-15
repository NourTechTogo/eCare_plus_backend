package com.nourtech.ecareplus.care.api.controller;

import com.nourtech.ecareplus.care.api.dto.request.ModifierMesureRequest;
import com.nourtech.ecareplus.care.api.dto.response.MesureSanteResponse;
import com.nourtech.ecareplus.care.api.mapper.CareMapper;
import com.nourtech.ecareplus.care.application.service.MesureSanteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mesures-sante")
public class MesureSanteController {

    private final MesureSanteService service;
    private final CareMapper mapper;

    public MesureSanteController(MesureSanteService service, CareMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/{mesureId}")
    public MesureSanteResponse consulter(@PathVariable UUID mesureId) {
        return mapper.toResponse(service.consulter(mesureId));
    }

    @PatchMapping("/{mesureId}")
    public MesureSanteResponse modifier(
            @PathVariable UUID mesureId,
            @Valid @RequestBody ModifierMesureRequest request
    ) {
        return mapper.toResponse(service.modifier(
                mesureId, request.valeurPrincipale(), request.valeurSecondaire(), request.unite(),
                request.contexteMesure(), request.commentaire()
        ));
    }

    @PatchMapping("/{mesureId}/confirmation")
    public MesureSanteResponse confirmer(@PathVariable UUID mesureId) {
        return mapper.toResponse(service.confirmer(mesureId));
    }
}
