package com.nourtech.ecareplus.communication.application.service;
import com.nourtech.ecareplus.communication.application.port.TransportNotification.*;
import com.nourtech.ecareplus.communication.domain.entity.*;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.infrastructure.persistence.*;
import com.nourtech.ecareplus.identity.domain.entity.*;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import com.nourtech.ecareplus.auth.infrastructure.security.AutorisationRessources;
import com.nourtech.ecareplus.organization.domain.entity.AffectationPatient;
import com.nourtech.ecareplus.organization.domain.enumeration.StatutAffectation;
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
        if(u==null||u.getStatutCompte()!=StatutCompte.ACTIF||!preferences.autorise(u.getId(),CanalNotification.valueOf(e.getCanal()))||!habilite(e,u)){e.annuler();return null;}
        String destination;
        if(e.getCanal().equals("PUSH")){var terminal=em.find(TerminalNotification.class,e.getCibleId());
            if(terminal==null||!terminal.isActif()||!terminal.getUtilisateurId().equals(u.getId())||terminal.getSessionVersion()!=u.getSessionVersion()){e.annuler();return null;}destination=terminal.getJetonPush();
        }else{destination=u.getTelephone();if(destination==null){e.annuler();return null;}}
        UUID jeton=e.reserver();var notification=em.find(Notification.class,e.getEvenementId());return new Reservation(e.getId(),jeton,new EnvoiCommande(e.getId(),e.getCanal(),destination,"eCare+","Une nouvelle information est disponible. Ouvrez votre application eCare+.",e.getEvenementId(),notification.getTypeRessource(),notification.getRessourceId()));
    }
    private boolean habilite(EnvoiNotification e,Utilisateur u){
        if(e.getPatientId()==null)return true;var p=em.find(Patient.class,e.getPatientId());if(p==null||p.getStatutCompte()!=StatutCompte.ACTIF)return false;
        if(u instanceof ProfessionnelSante pro)return autorisations.echangeAutorise(p,pro);
        if(!p.getId().equals(u.getId()))return false;
        return em.createQuery("select a from AffectationPatient a where a.patient.id=:id and a.statut=:statut",AffectationPatient.class).setParameter("id",p.getId()).setParameter("statut",StatutAffectation.ACTIVE).getResultStream().anyMatch(a->autorisations.echangeAutorise(p,a.getProfessionnel()));
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void terminer(Reservation reservation,AccuseFournisseur accuse,String erreur){
        var e=em.find(EnvoiNotification.class,reservation.id(),LockModeType.PESSIMISTIC_WRITE);if(e==null||!reservation.jeton().equals(e.getReservation()))return;
        if(accuse!=null){if(accuse.jetonInvalide()){var terminal=em.find(TerminalNotification.class,e.getCibleId());if(terminal!=null)terminal.desactiver();e.annuler();}else e.accepter(accuse.reference(),accuse.livre());}
        else e.echec(erreur,maximum,Math.min(86400,Math.max(0,delai)*(1L<<Math.min(10,e.getTentatives()-1))));
    }
}
