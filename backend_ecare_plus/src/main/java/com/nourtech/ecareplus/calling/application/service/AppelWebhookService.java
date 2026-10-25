package com.nourtech.ecareplus.calling.application.service;
import com.nourtech.ecareplus.calling.domain.entity.*;
import com.nourtech.ecareplus.calling.application.port.FournisseurAppel;
import com.nourtech.ecareplus.shared.application.Empreintes;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
@Service @RequiredArgsConstructor
public class AppelWebhookService {
    private final FournisseurAppel fournisseur;private final EntityManager em;private final ObjectMapper json;
    @Transactional public void traiter(String corps,String autorisation){
        fournisseur.verifierWebhook(corps,autorisation);var evenement=json.readTree(corps);String salle=evenement.path("room").path("name").asText();
        var appels=em.createQuery("select s from SessionAppel s where s.salle=:s",SessionAppel.class).setParameter("s",salle).getResultList();if(appels.isEmpty())return;
        var appel=appels.getFirst();em.refresh(appel,LockModeType.PESSIMISTIC_WRITE);
        String id=evenement.path("id").asText();if(id.isBlank())id=Empreintes.sha256(corps);if(id.length()>128)throw new IllegalArgumentException("Identifiant d'événement trop long");
        if(em.createQuery("select count(e) from EvenementAppel e where e.identifiantExterne=:id",Long.class).setParameter("id",id).getSingleResult()>0)return;
        String type=evenement.path("event").asText();
        switch(type){
            case "participant_joined","participant_left" -> {String identite=evenement.path("participant").path("identity").asText();UUID participant=UUID.fromString(identite);if(!participant.equals(appel.getInitiateurId())&&!participant.equals(appel.getDestinataireId()))throw new IllegalArgumentException("Participant inattendu");appel.participant(participant,type.equals("participant_joined"));}
            case "room_finished" -> appel.terminer(appel.getStatut().equals("EN_ATTENTE")?"EXPIRE":"TERMINE");
            case "room_started" -> { /* A room starting is not proof that both participants joined. */ }
            default -> {return;}
        }em.persist(new EvenementAppel(appel.getId(),id,type));
    }
}
