package com.nourtech.ecareplus.care.api.controller;
import com.nourtech.ecareplus.care.application.service.NoteCibleeService;
import com.nourtech.ecareplus.care.application.service.NoteCibleeService.*;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor @AccesVerifieParService @RequestMapping("/api/v1/notes")
public class NoteCibleeController {
    private final NoteCibleeService service;private final AccesPatientService acces;
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public NoteVue creer(@RequestBody CreationNote commande){return service.creer(acces.acteurId(),commande);}
    @GetMapping public Page<NoteVue> lister(@RequestParam String typeCible,@RequestParam UUID cibleId,Pageable page){return service.lister(acces.acteurId(),typeCible,cibleId,page);}
    @PatchMapping("/{id}") public NoteVue modifier(@PathVariable UUID id,@RequestBody ModificationNote commande){return service.modifier(acces.acteurId(),id,commande);}
    @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void supprimer(@PathVariable UUID id){service.supprimer(acces.acteurId(),id);}
}
