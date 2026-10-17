package com.nourtech.ecareplus.communication.api.controller;

import com.nourtech.ecareplus.communication.api.dto.request.CreerNotificationRequest;
import com.nourtech.ecareplus.communication.api.dto.response.NotificationResponse;
import com.nourtech.ecareplus.communication.api.mapper.CommunicationMapper;
import com.nourtech.ecareplus.communication.application.service.NotificationService;
import com.nourtech.ecareplus.communication.domain.entity.Notification;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService service;
    private final CommunicationMapper mapper;

    public NotificationController(NotificationService service, CommunicationMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> creer(@Valid @RequestBody CreerNotificationRequest request) {
        Notification notification = service.creer(mapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(notification.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(notification));
    }

    @GetMapping("/{notificationId}")
    public NotificationResponse consulter(@PathVariable UUID notificationId) {
        return mapper.toResponse(service.consulter(notificationId));
    }

    @PatchMapping("/{notificationId}/envoi")
    public NotificationResponse envoyer(@PathVariable UUID notificationId) {
        return mapper.toResponse(service.envoyer(notificationId, Instant.now()));
    }

    @PatchMapping("/{notificationId}/echec")
    public NotificationResponse marquerEnEchec(@PathVariable UUID notificationId) {
        return mapper.toResponse(service.marquerEnEchec(notificationId));
    }

    @PatchMapping("/{notificationId}/nouvel-essai")
    public NotificationResponse reessayerEnvoi(@PathVariable UUID notificationId) {
        return mapper.toResponse(service.reessayerEnvoi(notificationId, Instant.now()));
    }

    @PatchMapping("/{notificationId}/lecture")
    public NotificationResponse marquerCommeLue(@PathVariable UUID notificationId) {
        return mapper.toResponse(service.marquerCommeLue(notificationId, Instant.now()));
    }

    @PatchMapping("/{notificationId}/annulation")
    public NotificationResponse annuler(@PathVariable UUID notificationId) {
        return mapper.toResponse(service.annuler(notificationId));
    }
}
