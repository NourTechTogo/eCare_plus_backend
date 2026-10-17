package com.nourtech.ecareplus.messaging.api.controller;
import com.nourtech.ecareplus.auth.infrastructure.security.UtilisateurAuthentifie; import com.nourtech.ecareplus.messaging.api.dto.*;
import com.nourtech.ecareplus.messaging.application.service.MessagerieService; import com.nourtech.ecareplus.messaging.domain.entity.*;
import jakarta.validation.Valid; import org.springframework.data.domain.*; import org.springframework.http.*; import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken; import org.springframework.web.bind.annotation.*; import java.time.Instant; import java.util.*;
@RestController @RequestMapping("/api/v1/mobile/conversations")
public class MessagerieController {
 private final MessagerieService service; public MessagerieController(MessagerieService s){service=s;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ConversationVue ouvrir(JwtAuthenticationToken t,@Valid @RequestBody OuvrirConversationRequest r){UtilisateurAuthentifie u=UtilisateurAuthentifie.depuis(t);if(!"PATIENT".equals(u.role()))throw new AccessDeniedException("Réservé au patient");return ConversationVue.de(service.ouvrir(u.utilisateurId(),r.professionnelId()));}
 @GetMapping public List<ConversationVue> lister(JwtAuthenticationToken t){return service.lister(id(t)).stream().map(ConversationVue::de).toList();}
 @GetMapping("/{conversation}/messages") public Page<MessageVue> messages(JwtAuthenticationToken t,@PathVariable UUID conversation,Pageable p){return service.messages(id(t),conversation,p).map(MessageVue::de);}
 @PostMapping("/{conversation}/messages") @ResponseStatus(HttpStatus.CREATED) public MessageVue envoyer(JwtAuthenticationToken t,@PathVariable UUID conversation,@Valid @RequestBody EnvoyerMessageRequest r){return MessageVue.de(service.envoyer(id(t),conversation,r.contenu()));}
 @PostMapping("/{conversation}/lecture") public ResponseEntity<Void> lire(JwtAuthenticationToken t,@PathVariable UUID conversation){service.lire(id(t),conversation);return ResponseEntity.noContent().build();}
 private static UUID id(JwtAuthenticationToken t){return UtilisateurAuthentifie.depuis(t).utilisateurId();}
 public record ConversationVue(UUID id,UUID patientId,UUID professionnelId,String professionnelNom){static ConversationVue de(Conversation c){return new ConversationVue(c.getId(),c.getPatient().getId(),c.getProfessionnel().getId(),c.getProfessionnel().getPrenoms()+" "+c.getProfessionnel().getNom());}}
 public record MessageVue(UUID id,UUID expediteurId,String contenu,Instant envoyeLe,Instant luLe){static MessageVue de(MessageConversation m){return new MessageVue(m.getId(),m.getExpediteur().getId(),m.getContenu(),m.getEnvoyeLe(),m.getLuLe());}}
}
