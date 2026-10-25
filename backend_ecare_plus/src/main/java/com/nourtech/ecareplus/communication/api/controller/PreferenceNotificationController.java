package com.nourtech.ecareplus.communication.api.controller;
import com.nourtech.ecareplus.communication.application.service.PreferenceNotificationService;
import com.nourtech.ecareplus.communication.application.service.PreferenceNotificationService.*;
import com.nourtech.ecareplus.auth.infrastructure.security.AccesVerifieParService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/v1/compte") @RequiredArgsConstructor @AccesVerifieParService
public class PreferenceNotificationController {
    private final PreferenceNotificationService service;
    @GetMapping("/preferences-notifications") public PreferenceVue preferences(){return service.consulter();}
    @PatchMapping("/preferences-notifications") public PreferenceVue modifier(@RequestBody PreferenceCommande commande){return service.modifier(commande);}
    @GetMapping("/terminaux") public List<TerminalVue> terminaux(){return service.lister();}
    @PutMapping("/terminaux/{installation}") public TerminalVue terminal(@PathVariable String installation,@RequestBody TerminalCommande commande){return service.associer(installation,commande);}
    @DeleteMapping("/terminaux/{installation}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void retirer(@PathVariable String installation){service.retirer(installation);}
}
