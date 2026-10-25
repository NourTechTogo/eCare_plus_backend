package com.nourtech.ecareplus.care.api.controller;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import com.nourtech.ecareplus.care.api.dto.request.CreationPrescriptionRequest;
import com.nourtech.ecareplus.care.application.service.PrescriptionAtomiqueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequiredArgsConstructor @AccesVerifieParService
public class PrescriptionAtomiqueController {
    private final PrescriptionAtomiqueService service;
    @PostMapping("/api/v1/portail/patients/{patientId}/prescriptions")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public PrescriptionAtomiqueService.PrescriptionVue creer(@PathVariable UUID patientId,@Valid @RequestBody CreationPrescriptionRequest commande){return service.creer(patientId,commande);}
}
