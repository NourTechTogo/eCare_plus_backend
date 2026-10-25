package com.nourtech.ecareplus.auth.application.service;
import com.nourtech.ecareplus.auth.domain.entity.JetonRevoque;
import com.nourtech.ecareplus.auth.infrastructure.persistence.JetonRevoqueRepository;
import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
@Service @Transactional(readOnly=true)
public class SessionService  {
    private final JetonRevoqueRepository revoked;
    private final UtilisateurRepository users;
    @org.springframework.beans.factory.annotation.Autowired private com.nourtech.ecareplus.communication.infrastructure.persistence.TerminalNotificationRepository terminaux;
    public SessionService(JetonRevoqueRepository r,UtilisateurRepository u) {
        revoked=r;
        users=u;
    }
    public void verifier(Jwt jwt) {
        try  {
            var u=users.findById(UUID.fromString(jwt.getSubject())).orElseThrow(()->new BadJwtException("Session inconnue"));
            Number v=jwt.getClaim("sessionVersion");
            if(u.getStatutCompte()!=StatutCompte.ACTIF || (v==null?0:v.longValue())!=u.getSessionVersion() || revoked.existsByEmpreinte(empreinte(jwt.getTokenValue()))) throw new BadJwtException("Session terminée");
        }
        catch(IllegalArgumentException e) {
            throw new BadJwtException("Session invalide",e);
        }
    }
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.context.ApplicationEventPublisher events;
    public record SessionTerminee(UUID utilisateurId){}
    @Transactional public void revoquer(Jwt jwt) {
        String h=empreinte(jwt.getTokenValue());
        if(!revoked.existsByEmpreinte(h))revoked.save(new JetonRevoque(h,jwt.getExpiresAt()));
        terminaux.findAllByUtilisateurIdAndSessionRef(UUID.fromString(jwt.getSubject()),h).forEach(com.nourtech.ecareplus.communication.domain.entity.TerminalNotification::desactiver);
        events.publishEvent(new SessionTerminee(UUID.fromString(jwt.getSubject())));
    }
    public static String empreinte(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        }
        catch(NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

