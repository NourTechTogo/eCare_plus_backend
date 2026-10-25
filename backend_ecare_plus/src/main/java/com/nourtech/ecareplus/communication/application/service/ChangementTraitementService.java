package com.nourtech.ecareplus.communication.application.service;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.infrastructure.persistence.NotificationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
@Service @RequiredArgsConstructor @Transactional
public class ChangementTraitementService {
    private final EntityManager em;private final NotificationRepository notifications;private final DistributionNotificationService distribution;
    public void notifier(Traitement traitement,String reference){
        var patient=traitement.getDossier().getPatient();if(notifications.findByUtilisateurIdAndSourceRef(patient.getId(),reference).isPresent())return;
        var notification=new Notification(patient,"Mise à jour du traitement","Votre équipe soignante a actualisé votre traitement. Consultez votre planning.","TRAITEMENT",CanalNotification.APPLICATION,true);
        notification.lierSource(reference);notification.envoyer(Instant.now());em.persist(notification);distribution.programmer(notification,patient.getId());
        notification.rattacherRessource("TRAITEMENT",traitement.getId());
    }
    public void notifier(Traitement traitement){em.flush();notifier(traitement,"traitement:"+traitement.getId()+":"+traitement.getVersion());}
    public void notifier(PriseMedicament prise){em.flush();notifier(prise.getTraitement(),"prise:"+prise.getId()+":"+prise.getVersion());}
}
