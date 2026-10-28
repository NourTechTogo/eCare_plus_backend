package com.nourtech.ecareplus.care.application.service;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.communication.domain.entity.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
/** Shared validity check for inbox and queued external deliveries. */
@Service @RequiredArgsConstructor @Transactional
public class NotificationPriseService {
    private final EntityManager em;
    public static String source(ConfirmationPrise c){return "prise:"+c.getId()+":"+c.getDatePrevue();}
    public static boolean utile(ConfirmationPrise c,Instant now){
        var p=c.getPriseMedicament();var t=p.getTraitement();
        var jour=now.atZone(ZoneId.of(fuseau(c))).toLocalDate();
        return !c.isAnnulee()&&java.util.Set.of("ATTENTE","REPORTEE").contains(c.getStatut().name())
            &&p.isActif()&&t.getStatut().name().equals("ACTIF")&&t.getDossier().getStatut().name().equals("ACTIF")
            &&!jour.isBefore(t.getDateDebut())&&(t.getDateFin()==null||!jour.isAfter(t.getDateFin()))
            &&!c.getDatePrevue().isAfter(now)&&c.getDatePrevue().atZone(ZoneId.of(fuseau(c))).toLocalDate().equals(jour);
    }
    private static String fuseau(ConfirmationPrise c){return java.util.Objects.requireNonNullElse(c.getFuseauPrevu(),c.getPriseMedicament().getFuseau());}
    public void invalider(ConfirmationPrise c){
        for(var n:em.createQuery("select n from Notification n where n.typeRessource='PRISE' and n.ressourceId=:id and n.dateInvalidation is null",Notification.class).setParameter("id",c.getId()).getResultList()){
            n.invalider(Instant.now());
            em.createQuery("select e from EnvoiNotification e where e.evenementId=:id and e.statut in ('EN_ATTENTE','A_REESSAYER','EN_COURS')",EnvoiNotification.class).setParameter("id",n.getId()).getResultList().forEach(EnvoiNotification::annuler);
        }
    }
}
