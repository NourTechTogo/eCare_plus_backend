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
    private final NotificationRepository notifications;private final AccesPatientService acces;
    public record NotificationVue(UUID id,String titre,String message,String type,String statut,Instant dateEnvoi,Instant dateLecture,String typeRessource,UUID ressourceId){}
    public Page<NotificationVue> lister(Pageable page){return notifications.findAllByUtilisateurId(acces.acteurId(),PageRequest.of(page.isPaged()?page.getPageNumber():0,page.isPaged()?Math.min(100,page.getPageSize()):20,Sort.by(Sort.Order.desc("dateCreation"),Sort.Order.asc("id")))).map(CompteNotificationsService::vue);}
    public void lire(UUID id){var n=notifications.findById(id).orElseThrow(()->new RessourceIntrouvableException("Notification",id));if(!n.getUtilisateur().getId().equals(acces.acteurId()))throw new AccessDeniedException("Notification privée");if(n.getDateLecture()==null)n.marquerCommeLue(Instant.now());}
    public static NotificationVue vue(Notification n){return new NotificationVue(n.getId(),n.getTitre(),n.getMessage(),n.getTypeNotification(),n.getStatutEnvoi().name(),n.getDateEnvoi(),n.getDateLecture(),n.getTypeRessource(),n.getRessourceId());}
}
