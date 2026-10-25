package com.nourtech.ecareplus.communication.application.service;
import com.nourtech.ecareplus.communication.domain.entity.EnvoiNotification;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
@Service
public class NotificationCallbackService {
    private final String secret;private final EntityManager em;private final ObjectMapper json;
    public NotificationCallbackService(@Value("${app.notifications.gateway-secret:}")String secret,EntityManager em,ObjectMapper json){this.secret=secret;this.em=em;this.json=json;}
    @Transactional public void traiter(String corps,String date,String signature){
        try{
            if(secret.isBlank()||corps==null||corps.length()>4096||date==null||signature==null||Math.abs(Instant.now().getEpochSecond()-Long.parseLong(date))>300)throw new IllegalArgumentException();
            var mac=javax.crypto.Mac.getInstance("HmacSHA256");mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            if(!MessageDigest.isEqual(mac.doFinal((date+"."+corps).getBytes(StandardCharsets.UTF_8)),HexFormat.of().parseHex(signature)))throw new IllegalArgumentException();
        }catch(Exception invalide){throw new AccessDeniedException("Signature de notification invalide");}
        var evenement=json.readTree(corps);var id=UUID.fromString(evenement.path("envoiId").asText());var envoi=em.find(EnvoiNotification.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(envoi==null||envoi.getReferenceFournisseur()==null||!envoi.getReferenceFournisseur().equals(evenement.path("reference").asText()))throw new ConflitMetierException("Accusé fournisseur non encore enregistré ou incompatible");
        switch(evenement.path("statut").asText()){
            case "LIVRE" -> envoi.livrer(envoi.getReferenceFournisseur());
            case "ECHEC" -> envoi.echecDefinitif();
            default -> throw new IllegalArgumentException("Statut de livraison attendu : LIVRE ou ECHEC");
        }
    }
}
