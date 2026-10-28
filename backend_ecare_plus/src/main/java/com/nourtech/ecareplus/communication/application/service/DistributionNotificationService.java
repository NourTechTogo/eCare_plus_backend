package com.nourtech.ecareplus.communication.application.service;
import com.nourtech.ecareplus.communication.application.port.TransportNotification.*;
import com.nourtech.ecareplus.communication.domain.entity.*;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.infrastructure.persistence.*;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import com.nourtech.ecareplus.auth.infrastructure.security.AutorisationRessources;
import com.nourtech.ecareplus.consent.domain.entity.Consentement;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class DistributionNotificationService {
    private final EnvoiNotificationRepository envois;private final TerminalNotificationRepository terminaux;private final PreferenceNotificationService preferences;private final AutorisationRessources autorisations;private final EntityManager em;
    @Value("${app.notifications.tentatives-max:5}") private int maximum;
    @Value("${app.notifications.delai-reprise-secondes:30}") private long delai;
    public record Reservation(UUID id,UUID jeton,EnvoiCommande commande){}
    @Transactional public void programmer(Notification notification,UUID patientId){
        UUID utilisateur=notification.getUtilisateur().getId();
        for(var t:terminaux.findAllByUtilisateurIdAndActifTrue(utilisateur))if(!envois.existsByEvenementIdAndCibleIdAndCanal(notification.getId(),t.getId(),"PUSH"))envois.save(new EnvoiNotification(notification.getId(),utilisateur,t.getId(),patientId,"PUSH"));
        if(preferences.autorise(utilisateur,CanalNotification.WHATSAPP)&&notification.getUtilisateur().getTelephone()!=null&&!envois.existsByEvenementIdAndCibleIdAndCanal(notification.getId(),utilisateur,"WHATSAPP"))envois.save(new EnvoiNotification(notification.getId(),utilisateur,utilisateur,patientId,"WHATSAPP"));
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW) public Reservation reclamer(UUID id){
        var e=em.find(EnvoiNotification.class,id,LockModeType.PESSIMISTIC_WRITE);if(e==null||!e.disponible(Instant.now()))return null;
        var u=em.find(Utilisateur.class,e.getDestinataireId());
        if(u==null||u.getStatutCompte()!=StatutCompte.ACTIF||!preferences.autorise(u.getId(),CanalNotification.valueOf(e.getCanal()))||!habilite(e,u)||!evenementValide(e)){e.annuler();return null;}
        String destination;
        if(e.getCanal().equals("PUSH")){var terminal=em.find(TerminalNotification.class,e.getCibleId());
            if(terminal==null||!terminal.isActif()||!terminal.getUtilisateurId().equals(u.getId())||terminal.getSessionVersion()!=u.getSessionVersion()){e.annuler();return null;}destination=terminal.getJetonPush();
        }else{destination=u.getTelephone();if(destination==null){e.annuler();return null;}}
        UUID jeton=e.reserver();var notification=em.find(Notification.class,e.getEvenementId());return new Reservation(e.getId(),jeton,new EnvoiCommande(e.getId(),e.getCanal(),destination,"eCare+","Une nouvelle information est disponible. Ouvrez votre application eCare+.",e.getEvenementId(),notification.getTypeRessource(),notification.getRessourceId()));
    }
    private boolean evenementValide(EnvoiNotification e) {
        var n=em.find(Notification.class,e.getEvenementId());
        if(n==null||!n.getUtilisateur().getId().equals(e.getDestinataireId()))return false;
        if("PRISE".equals(n.getTypeRessource())) {
            var c=em.find(com.nourtech.ecareplus.care.domain.entity.ConfirmationPrise.class,n.getRessourceId());
            return c!=null&&n.getDateSuppression()==null&&c.getPriseMedicament().getTraitement().getDossier().getPatient().getId().equals(e.getDestinataireId())
                &&com.nourtech.ecareplus.care.application.service.NotificationPriseService.utile(c,Instant.now())
                &&com.nourtech.ecareplus.care.application.service.NotificationPriseService.source(c).equals(n.getSourceRef());
        }
        if("RENDEZ_VOUS_PERSONNEL".equals(n.getTypeRessource())) {
            var r=em.find(RendezVous.class,n.getRessourceId());
            return r!=null&&r.getPatient().getId().equals(e.getDestinataireId())&&r.getRappel()!=null
                    &&n.getDateSuppression()==null&&RappelAgendaService.encoreUtile(r,Instant.now())
                    &&RappelAgendaService.source(r,r.getRappel()).equals(n.getSourceRef());
        }
        if("ALERTE".equals(n.getTypeRessource())) {
            var a=em.find(com.nourtech.ecareplus.alert.domain.entity.Alerte.class,n.getRessourceId());
            if(a==null)return false;
            if(a.getMesure()!=null&&a.getMesure().getDossier().getStructure()==null)
                return a.getPatient().getId().equals(e.getDestinataireId());
        }
        if("TRAITEMENT".equals(n.getTypeRessource())) {
            var traitement=em.find(com.nourtech.ecareplus.care.domain.entity.Traitement.class,n.getRessourceId());
            if(traitement==null)return false;
            if(traitement.getDossier().getStructure()==null)return traitement.getDossier().getPatient().getId().equals(e.getDestinataireId());
        }
        return true;
    }
    private boolean habilite(EnvoiNotification e,Utilisateur u){
        if(e.getPatientId()==null)return true;var p=em.find(Patient.class,e.getPatientId());if(p==null||p.getStatutCompte()!=StatutCompte.ACTIF)return false;
        if(u instanceof ProfessionnelSante pro)return autorisations.echangeAutorise(p,pro);
        if(!p.getId().equals(u.getId()))return false;
        return em.createQuery("select c from Consentement c where c.patient.id=:id and c.statut=:statut and c.typeConsentement=:type",Consentement.class)
                .setParameter("id",p.getId()).setParameter("statut",StatutConsentement.ACCORDE).setParameter("type","SUIVI_MEDICAL")
                .getResultStream().anyMatch(c->c.estValideA(Instant.now()));
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void terminer(Reservation reservation,AccuseFournisseur accuse,String erreur){
        var e=em.find(EnvoiNotification.class,reservation.id(),LockModeType.PESSIMISTIC_WRITE);if(e==null||!reservation.jeton().equals(e.getReservation()))return;
        if(accuse!=null){if(accuse.jetonInvalide()){var terminal=em.find(TerminalNotification.class,e.getCibleId());if(terminal!=null)terminal.desactiver();e.annuler();}else e.accepter(accuse.reference(),accuse.livre());}
        else e.echec(erreur,maximum,Math.min(86400,Math.max(0,delai)*(1L<<Math.min(10,e.getTentatives()-1))));
    }
}
