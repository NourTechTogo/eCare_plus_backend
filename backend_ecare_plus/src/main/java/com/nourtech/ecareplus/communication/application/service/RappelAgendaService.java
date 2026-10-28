package com.nourtech.ecareplus.communication.application.service;

import com.nourtech.ecareplus.communication.domain.entity.*;
import com.nourtech.ecareplus.communication.domain.enumeration.*;
import com.nourtech.ecareplus.communication.infrastructure.persistence.NotificationRepository;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement;
import com.nourtech.ecareplus.consent.infrastructure.persistence.ConsentementRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class RappelAgendaService {
    private final EntityManager em;
    private final NotificationRepository notifications;
    private final ConsentementRepository consentements;
    private final DistributionNotificationService distribution;

    public static String source(RendezVous rdv,Rappel rappel){return "agenda:"+rdv.getId()+":"+rappel.getId();}
    public static boolean encoreUtile(RendezVous rdv,Instant maintenant) {
        // Operational grace for scheduler delays, including reminders requested at the exact event time.
        return rdv.isPersonnel()&&rdv.getStatut()!=StatutRendezVous.ANNULE&&rdv.getStatut()!=StatutRendezVous.REALISE
                &&!rdv.getDateHeure().plus(Duration.ofMinutes(15)).isBefore(maintenant);
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public boolean declencher(UUID rendezVousId,Instant maintenant) {
        Objects.requireNonNull(maintenant,"maintenant");
        var rdv=em.find(RendezVous.class,rendezVousId);if(rdv==null||!rdv.isPersonnel())return false;
        var patient=em.find(Patient.class,rdv.getPatient().getId(),LockModeType.PESSIMISTIC_WRITE);
        em.refresh(rdv);
        var rappel=rdv.getRappel();
        if(rappel==null||rappel.getStatut()!=StatutRappel.PLANIFIE||rappel.getDateHeurePrevue().isAfter(maintenant))return false;
        boolean consenti=consentements.findAllByPatientIdAndStatut(patient.getId(),StatutConsentement.ACCORDE).stream()
                .anyMatch(c->"SUIVI_MEDICAL".equals(c.getTypeConsentement())&&c.estValideA(maintenant));
        if(patient.getStatutCompte()!=StatutCompte.ACTIF||!consenti||!encoreUtile(rdv,maintenant)) {
            rappel.annuler();return false;
        }
        String reference=source(rdv,rappel);
        if(notifications.findByUtilisateurIdAndSourceRef(patient.getId(),reference).isPresent()) {
            rappel.envoyer(maintenant);return false;
        }
        var notification=new Notification(patient,"Rappel de rendez-vous","Un rappel de votre agenda personnel est disponible.",
                "RENDEZ_VOUS",CanalNotification.APPLICATION,true);
        notification.lierSource(reference);notification.rattacherRessource("RENDEZ_VOUS_PERSONNEL",rdv.getId());
        notification.envoyer(maintenant);em.persist(notification);rappel.envoyer(maintenant);
        distribution.programmer(notification,patient.getId());return true;
    }

    @Transactional
    public void invalider(RendezVous rdv) {
        var rappel=rdv.getRappel();if(rappel==null)return;
        // A read, already delivered reminder remains historical; its notification is hidden below.
        if(rappel.getStatut()!=StatutRappel.LU&&rappel.getStatut()!=StatutRappel.ANNULE)rappel.annuler();
        notifications.findByUtilisateurIdAndSourceRef(rdv.getPatient().getId(),source(rdv,rappel)).ifPresent(n->{
            n.invalider(Instant.now());
            em.createQuery("select e from EnvoiNotification e where e.evenementId=:id and e.statut in ('EN_ATTENTE','A_REESSAYER','EN_COURS')",EnvoiNotification.class)
                    .setParameter("id",n.getId()).getResultList().forEach(EnvoiNotification::annuler);
        });
    }
}
