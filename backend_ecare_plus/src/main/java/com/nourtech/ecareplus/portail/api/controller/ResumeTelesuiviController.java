package com.nourtech.ecareplus.portail.api.controller;
import com.nourtech.ecareplus.portail.application.ResumeTelesuiviService;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequiredArgsConstructor @AccesVerifieParService
public class ResumeTelesuiviController {
    private final ResumeTelesuiviService service;
    @GetMapping("/api/v1/portail/patients/{patientId}/resume") public Map<String,Object> resume(@PathVariable UUID patientId){return service.consulter(patientId);}
}
