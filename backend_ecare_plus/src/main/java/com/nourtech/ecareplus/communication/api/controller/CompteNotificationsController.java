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
    @GetMapping public Page<CompteNotificationsService.NotificationVue> lister(Pageable page){return service.lister(page);}
    @PostMapping("/{id}/lecture") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void lire(@PathVariable UUID id){service.lire(id);}
}
