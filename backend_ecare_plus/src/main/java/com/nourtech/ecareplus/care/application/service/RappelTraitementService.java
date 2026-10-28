package com.nourtech.ecareplus.care.application.service;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.infrastructure.persistence.PriseMedicamentRepository;
import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.infrastructure.persistence.NotificationRepository;
import com.nourtech.ecareplus.communication.application.service.DistributionNotificationService;
import com.nourtech.ecareplus.consent.infrastructure.persistence.ConsentementRepository;
import com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class RappelTraitementService {
    private final EntityManager em;
    private final OccurrenceTraitementService occurrences;
    private final PriseMedicamentRepository prises;
    private final ConsentementRepository consentements;
    private final NotificationRepository notifications;
    private final DistributionNotificationService distribution;
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public int traiter(UUID patientId,Instant now){
        Objects.requireNonNull(now,"maintenant");
        var patient=em.find(Patient.class,patientId,LockModeType.PESSIMISTIC_WRITE);
        if(patient==null||!patient.getStatutCompte().name().equals("ACTIF"))return 0;
        boolean consenti=consentements.findAllByPatientIdAndStatut(patientId,StatutConsentement.ACCORDE).stream().anyMatch(c->c.getTypeConsentement().equals("SUIVI_MEDICAL")&&c.estValideA(now));
        if(!consenti)return 0;
        var jour=now.atZone(ZoneOffset.UTC).toLocalDate();
        var liste=prises.planningPatient(patientId).stream().filter(p->p.isActif()&&p.getTraitement().getStatut().name().equals("ACTIF")&&p.getTraitement().getDossier().getStatut().name().equals("ACTIF")).toList();
        occurrences.materialiser(patientId,null,jour.minusDays(1),jour.plusDays(1),liste);em.flush();
        var dues=em.createQuery("select c from ConfirmationPrise c where c.priseMedicament.traitement.dossier.patient.id=:patient and c.annulee=false and c.statut in ('ATTENTE','REPORTEE') and c.datePrevue<=:now and c.datePrevue>=:debut order by c.datePrevue,c.id",ConfirmationPrise.class)
            .setParameter("patient",patientId).setParameter("now",now).setParameter("debut",now.minusSeconds(48*3600)).getResultList();
        int total=0;
        for(var c:dues){
            if(!NotificationPriseService.utile(c,now))continue;
            String source=NotificationPriseService.source(c);
            if(notifications.findByUtilisateurIdAndSourceRef(patientId,source).isPresent())continue;
            var n=new Notification(patient,"Rappel de traitement","Un rappel de votre traitement est disponible. Consultez votre planning.","TRAITEMENT",CanalNotification.APPLICATION,true);
            n.lierSource(source);n.rattacherRessource("PRISE",c.getId());n.envoyer(now);em.persist(n);distribution.programmer(n,patientId);total++;
        }
        return total;
    }
}
