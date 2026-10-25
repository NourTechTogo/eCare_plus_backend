package com.nourtech.ecareplus.communication.infrastructure.notification;
import com.nourtech.ecareplus.communication.application.service.NotificationCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class NotificationCallbackController {
    private final NotificationCallbackService service;
    @PostMapping("/api/v1/integrations/notifications/callback") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void traiter(@RequestBody String corps,@RequestHeader(value="X-Ecare-Timestamp",required=false)String date,@RequestHeader(value="X-Ecare-Signature",required=false)String signature){service.traiter(corps,date,signature);}
}
