package com.nourtech.ecareplus.mobile.api.controller;

import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import com.nourtech.ecareplus.mobile.api.dto.AgendaPersonnelRequest;
import com.nourtech.ecareplus.mobile.application.service.AgendaPersonnelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/mobile/me/rendez-vous")
@RequiredArgsConstructor @AccesVerifieParService
public class AgendaPersonnelController {
    private final AgendaPersonnelService service;
    public record Annulation(@NotNull UUID sourceRef,@NotNull @PositiveOrZero Long version) {}
    @GetMapping public Page<AgendaPersonnelService.Vue> lister(@RequestParam(required=false) Instant du,
            @RequestParam(required=false) Instant au,@RequestParam(defaultValue="false") boolean inclureAnnules,Pageable page) {
        return service.lister(du,au,inclureAnnules,page);
    }
    @GetMapping("/{id}") public AgendaPersonnelService.Vue consulter(@PathVariable UUID id){return service.consulter(id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AgendaPersonnelService.Vue creer(@Valid @RequestBody AgendaPersonnelRequest commande){return service.creer(commande);}
    @PatchMapping("/{id}") public AgendaPersonnelService.Vue modifier(@PathVariable UUID id,@Valid @RequestBody AgendaPersonnelRequest commande){return service.modifier(id,commande);}
    @DeleteMapping("/{id}") public AgendaPersonnelService.Vue supprimer(@PathVariable UUID id,@Valid @RequestBody Annulation commande){return service.annuler(id,commande.sourceRef(),commande.version());}
    @PostMapping("/{id}/annulation") public AgendaPersonnelService.Vue annuler(@PathVariable UUID id,@Valid @RequestBody Annulation commande){return service.annuler(id,commande.sourceRef(),commande.version());}
}
