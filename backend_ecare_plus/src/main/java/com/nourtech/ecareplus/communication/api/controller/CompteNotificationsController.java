package com.nourtech.ecareplus.communication.api.controller;
import com.nourtech.ecareplus.communication.application.service.CompteNotificationsService;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor @AccesVerifieParService @RequestMapping("/api/v1/compte/notifications")
public class CompteNotificationsController {
    private final CompteNotificationsService service;
    @GetMapping public Page<CompteNotificationsService.NotificationVue> lister(Pageable page,@RequestParam(defaultValue="false") boolean archivees){return service.lister(page,archivees);}
    @PostMapping("/{id}/lecture") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void lire(@PathVariable UUID id){service.lire(id);}
    @PostMapping("/{id}/archivage") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void archiver(@PathVariable UUID id){service.archiver(id);}
    @PostMapping("/{id}/desarchivage") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void desarchiver(@PathVariable UUID id){service.desarchiver(id);}
    @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void supprimer(@PathVariable UUID id){service.supprimer(id);}
    @PostMapping("/{id}/restauration") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void restaurer(@PathVariable UUID id){service.restaurer(id);}
}
