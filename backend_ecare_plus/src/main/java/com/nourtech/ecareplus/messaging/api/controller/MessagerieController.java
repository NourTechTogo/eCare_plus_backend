package com.nourtech.ecareplus.messaging.api.controller;
import com.nourtech.ecareplus.auth.infrastructure.security.UtilisateurAuthentifie;
import com.nourtech.ecareplus.messaging.api.dto.*;
import com.nourtech.ecareplus.messaging.application.service.MessagerieService;
import com.nourtech.ecareplus.messaging.domain.entity.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/v1/mobile/conversations") @Transactional(readOnly=true)
public class MessagerieController  {
    private final MessagerieService service;
    public MessagerieController(MessagerieService s) {
        service=s;
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public ConversationVue ouvrir(JwtAuthenticationToken t,@Valid @RequestBody OuvrirConversationRequest r) {
        UtilisateurAuthentifie u=UtilisateurAuthentifie.depuis(t);
        UUID patient,pro;
        if("PATIENT".equals(u.role())) {
            patient=u.utilisateurId();
            pro=r.professionnelId();
        }
        else if(Set.of("MEDECIN","INFIRMIER").contains(u.role())) {
            patient=r.patientId();
            pro=u.utilisateurId();
        }
        else throw new AccessDeniedException("Conversation réservée au patient et professionnel affecté");
        if(patient==null||pro==null)throw new IllegalArgumentException("Participant requis");
        return vue(service.ouvrir(patient,pro),u.utilisateurId());
    }
    @GetMapping public List<ConversationVue> lister(JwtAuthenticationToken t) {
        return service.lister(id(t)).stream().map(c->vue(c,id(t))).toList();
    }
    @GetMapping("/{conversation}/messages") public Page<MessageVue> messages(JwtAuthenticationToken t,@PathVariable UUID conversation,Pageable p) {
        return service.messages(id(t),conversation,p).map(MessageVue::de);
    }
    @PostMapping("/{conversation}/messages") @ResponseStatus(HttpStatus.CREATED) @Transactional public MessageVue envoyer(JwtAuthenticationToken t,@PathVariable UUID conversation,@Valid @RequestBody EnvoyerMessageRequest r) {
        return MessageVue.de(service.envoyer(id(t),conversation,r.contenu(),r.piecesJointes(),r.sourceRef()));
    }
    @PostMapping("/{conversation}/lecture") @Transactional public ResponseEntity<Void> lire(JwtAuthenticationToken t,@PathVariable UUID conversation) {
        service.lire(id(t),conversation);
        return ResponseEntity.noContent().build();
    }
    private static UUID id(JwtAuthenticationToken t) {
        return UtilisateurAuthentifie.depuis(t).utilisateurId();
    }
    private ConversationVue vue(Conversation c,UUID actor) {
        var m=service.dernier(c.getId());
        return new ConversationVue(c.getId(),c.getPatient().getId(),c.getProfessionnel().getId(),c.getProfessionnel().getPrenoms()+" "+c.getProfessionnel().getNom(),c.getPatient().getPrenoms()+" "+c.getPatient().getNom(),c.getPatient().getNumeroPatient(),m==null?null:MessageVue.de(m),service.nonLus(c.getId(),actor));
    }
    public record ConversationVue(UUID id,UUID patientId,UUID professionnelId,String professionnelNom,String patientNom,String numeroPatient,MessageVue dernierMessage,long nonLus) {
    }
    public record MessageVue(UUID id,UUID expediteurId,String contenu,Instant envoyeLe,Instant luLe,List<com.nourtech.ecareplus.messaging.application.service.PieceJointeService.PieceVue> piecesJointes) {
        static MessageVue de(MessageConversation m) {
            return new MessageVue(m.getId(),m.getExpediteur().getId(),m.getContenu(),m.getEnvoyeLe(),m.getLuLe(),m.getPiecesJointes().stream().filter(p->!p.isSupprimee()).map(com.nourtech.ecareplus.messaging.application.service.PieceJointeService::vue).toList());
        }
    }
}

