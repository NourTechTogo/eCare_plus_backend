package com.nourtech.ecareplus.communication.application.service;

import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.infrastructure.persistence.NotificationRepository;
import com.nourtech.ecareplus.shared.application.service.AccesPatientService;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import java.time.Instant;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional
public class CompteNotificationsService {
    private final NotificationRepository notifications;
    private final AccesPatientService acces;
    public record NotificationVue(UUID id,String titre,String message,String type,String statut,Instant dateEnvoi,Instant dateLecture,String typeRessource,UUID ressourceId,Instant dateArchivage){}
    public Page<NotificationVue> lister(Pageable page){return lister(page,false);}
    @Transactional(readOnly=true)
    public Page<NotificationVue> lister(Pageable page,boolean archivees){
        var pagination=PageRequest.of(page.isPaged()?page.getPageNumber():0,page.isPaged()?Math.min(100,page.getPageSize()):20,Sort.by(Sort.Order.desc("dateCreation"),Sort.Order.asc("id")));
        var utilisateur=acces.acteurId();
        return (archivees?notifications.findAllByUtilisateurIdAndDateSuppressionIsNullAndDateArchivageIsNotNull(utilisateur,pagination):notifications.findAllByUtilisateurIdAndDateSuppressionIsNullAndDateArchivageIsNull(utilisateur,pagination)).map(CompteNotificationsService::vue);
    }
    private Notification charger(UUID id,boolean inclureSupprimee){
        var n=notifications.verrouiller(id).orElseThrow(()->new RessourceIntrouvableException("Notification",id));
        if(!n.getUtilisateur().getId().equals(acces.acteurId()))throw new AccessDeniedException("Notification privée");
        if(!inclureSupprimee && n.getDateSuppression()!=null)throw new RessourceIntrouvableException("Notification",id);
        return n;
    }
    public void lire(UUID id){var n=charger(id,false);if(n.getDateLecture()==null)n.marquerCommeLue(Instant.now());}
    public void archiver(UUID id){charger(id,false).archiver(Instant.now());}
    public void desarchiver(UUID id){charger(id,false).desarchiver();}
    public void supprimer(UUID id){charger(id,true).supprimerDeLaBoite(Instant.now());}
    public void restaurer(UUID id){charger(id,true).restaurerDansLaBoite();}
    public static NotificationVue vue(Notification n){return new NotificationVue(n.getId(),n.getTitre(),n.getMessage(),n.getTypeNotification(),n.getStatutEnvoi().name(),n.getDateEnvoi(),n.getDateLecture(),n.getTypeRessource(),n.getRessourceId(),n.getDateArchivage());}
}
