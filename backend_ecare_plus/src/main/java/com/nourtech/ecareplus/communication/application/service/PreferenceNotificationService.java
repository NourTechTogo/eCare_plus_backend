package com.nourtech.ecareplus.communication.application.service;
import com.nourtech.ecareplus.communication.domain.entity.*;
import com.nourtech.ecareplus.communication.infrastructure.persistence.*;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.auth.application.service.SessionService;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional
public class PreferenceNotificationService {
    private final TerminalNotificationRepository terminaux;private final PreferenceNotificationRepository preferences;private final AccesPatientService acces;private final EntityManager em;
    public record PreferenceVue(boolean pushActif,boolean whatsappActif,long version){}
    public record PreferenceCommande(Boolean pushActif,Boolean whatsappActif,String versionConsentement){}
    public record TerminalCommande(String plateforme,String jetonPush){}
    public record TerminalVue(String installationId,String plateforme,boolean actif){}
    public PreferenceVue consulter(){var id=acces.acteurId();return vue(preferences.findById(id).orElseGet(()->new PreferenceNotification(id)));}
    public PreferenceVue modifier(PreferenceCommande commande){
        var id=acces.acteurId();em.find(Utilisateur.class,id,LockModeType.PESSIMISTIC_WRITE);
        var p=preferences.findById(id).orElseGet(()->new PreferenceNotification(id));
        if(Boolean.TRUE.equals(commande.whatsappActif())&&(commande.versionConsentement()==null||commande.versionConsentement().isBlank()||commande.versionConsentement().length()>80))throw new IllegalArgumentException("Version du consentement WhatsApp obligatoire");
        if(commande.whatsappActif()!=null)em.persist(new ConsentementCanal(id,commande.whatsappActif(),commande.versionConsentement()));
        p.modifier(commande.pushActif(),commande.whatsappActif());preferences.saveAndFlush(p);return vue(p);
    }
    public TerminalVue associer(String installation,TerminalCommande commande){
        if(installation==null||!installation.matches("[A-Za-z0-9_-]{8,128}"))throw new IllegalArgumentException("Identifiant d'installation invalide");
        if(!Set.of("ANDROID","IOS","WEB").contains(Objects.requireNonNullElse(commande.plateforme(),""))||commande.jetonPush()==null||commande.jetonPush().isBlank()||commande.jetonPush().length()>4096)throw new IllegalArgumentException("Plateforme ou jeton push invalide");
        var id=acces.acteurId();em.find(Utilisateur.class,id,LockModeType.PESSIMISTIC_WRITE);
        var terminal=terminaux.findByInstallationId(installation).orElseGet(()->new TerminalNotification(installation));
        if(terminal.getUtilisateurId()!=null&&!terminal.getUtilisateurId().equals(id)&&!terminal.getJetonPush().equals(commande.jetonPush()))throw new AccessDeniedException("Installation déjà liée à un autre terminal");
        var jwt=((JwtAuthenticationToken)SecurityContextHolder.getContext().getAuthentication()).getToken();
        terminal.associer(id,commande.plateforme(),commande.jetonPush(),SessionService.empreinte(jwt.getTokenValue()),em.find(Utilisateur.class,id).getSessionVersion());
        terminaux.save(terminal);return vue(terminal);
    }
    public List<TerminalVue> lister(){return terminaux.findAllByUtilisateurId(acces.acteurId()).stream().map(this::vue).toList();}
    public void retirer(String installation){var terminal=terminaux.findByInstallationId(installation).orElseThrow(()->new IllegalArgumentException("Installation inconnue"));if(!terminal.getUtilisateurId().equals(acces.acteurId()))throw new AccessDeniedException("Installation appartenant à un autre compte");terminal.desactiver();}
    public boolean autorise(UUID utilisateur,CanalNotification canal){var p=preferences.findById(utilisateur).orElseGet(()->new PreferenceNotification(utilisateur));return canal==CanalNotification.PUSH?p.isPushActif():canal==CanalNotification.WHATSAPP&&p.isWhatsappActif();}
    private PreferenceVue vue(PreferenceNotification p){return new PreferenceVue(p.isPushActif(),p.isWhatsappActif(),p.getVersion());}
    private TerminalVue vue(TerminalNotification t){return new TerminalVue(t.getInstallationId(),t.getPlateforme(),t.isActif());}
}
