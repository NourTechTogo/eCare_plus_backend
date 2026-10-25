package com.nourtech.ecareplus.portail.api.controller;
import com.nourtech.ecareplus.portail.application.PortailService;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/v1/portail")
public class PortailController  {
    private final PortailService s;
    public PortailController(PortailService s) {
        this.s=s;
    }
    @GetMapping("/patients") public Page<Map<String,Object>> patients(Pageable p) {
        return s.patients(p);
    }
    @GetMapping("/patients/{id}") public Map<String,Object> patient(@PathVariable UUID id) {
        return s.consulter(id);
    }
    @PostMapping("/patients") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public Map<String,Object> creer(@RequestBody Map<String,Object> r,HttpServletRequest req) {
        return s.creer(r,req.getRemoteAddr());
    }
    @PatchMapping("/patients/{id}") public Map<String,Object> modifier(@PathVariable UUID id,@RequestBody Map<String,Object> r) {
        return s.modifier(id,r);
    }
    @GetMapping("/professionnels") public Page<Map<String,Object>> pros(Pageable p) {
        return s.professionnels(p);
    }
    @GetMapping("/patients/{id}/mesures") public Page<Map<String,Object>> mesures(@PathVariable UUID id,@RequestParam(required=false) TypeMesure typeMesure,@RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,Pageable p) {
        return s.mesures(id,false,typeMesure,from,to,p);
    }
    @GetMapping("/patients/{id}/observations") public Page<Map<String,Object>> observations(@PathVariable UUID id,Pageable p) {
        return s.observations(id,p);
    }
    @PostMapping("/patients/{id}/observations") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public Map<String,Object> observer(@PathVariable UUID id,@RequestBody Map<String,Object> r) {
        return s.observer(id,PortailService.texte(r,"contenu",null));
    }
    @GetMapping("/alertes") public Page<Map<String,Object>> alertes(Pageable p) {
        return s.alertes(null,p);
    }
    @GetMapping("/patients/{id}/seuils") public List<Map<String,Object>> seuils(@PathVariable UUID id) {
        return s.seuils(id);
    }
    @GetMapping("/alertes/{id}") public Map<String,Object> alerte(@PathVariable UUID id) {
        return s.alerte(id);
    }
    @PostMapping("/alertes/{id}/cloture") public Map<String,Object> cloturer(@PathVariable UUID id,@RequestBody Map<String,Object> r) {
        return s.cloturer(id,PortailService.texte(r,"motifCloture",null));
    }
}

