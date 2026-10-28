package com.nourtech.ecareplus.mobile.api.controller;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import com.nourtech.ecareplus.care.application.service.SyntheseConsultationService;
import com.nourtech.ecareplus.care.application.service.ExportConsultationPdf;
import com.nourtech.ecareplus.mobile.api.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor @AccesVerifieParService @RequestMapping("/api/v1/mobile/me/rapports-consultation")
public class RapportConsultationController {
    private final SyntheseConsultationService service;
    private final ExportConsultationPdf pdf;
    @GetMapping(value="/{id}/pdf",produces="application/pdf")
    public org.springframework.http.ResponseEntity<byte[]> telecharger(@PathVariable UUID id){
        var synthese=service.consulter(id); // Identity, ownership and consent before rendering.
        return org.springframework.http.ResponseEntity.ok()
            .header("Content-Disposition","attachment; filename=\"ecare-consultation-"+id+".pdf\"")
            .header("Cache-Control","private, no-store")
            .header("X-Content-Type-Options","nosniff")
            .contentType(org.springframework.http.MediaType.APPLICATION_PDF).body(pdf.rendre(synthese));
    }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public SyntheseConsultationVue generer(@Valid @RequestBody GenererSyntheseConsultationRequest r){return service.generer(r);}
    @GetMapping("/{id}") public SyntheseConsultationVue consulter(@PathVariable UUID id){return service.consulter(id);}
    @GetMapping public Page<SyntheseConsultationService.ResumeVue> lister(Pageable page){return service.lister(page);}
}
