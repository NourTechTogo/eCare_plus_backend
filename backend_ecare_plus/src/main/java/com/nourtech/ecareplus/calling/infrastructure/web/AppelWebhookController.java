package com.nourtech.ecareplus.calling.infrastructure.web;
import com.nourtech.ecareplus.calling.application.service.AppelWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class AppelWebhookController {
    private final AppelWebhookService service;
    @PostMapping("/api/v1/integrations/appels/livekit") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void traiter(@RequestBody String corps,@RequestHeader(value="Authorization",required=false)String autorisation){service.traiter(corps,autorisation);}
}
