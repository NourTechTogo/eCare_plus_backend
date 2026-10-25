package com.nourtech.ecareplus.mobile.application.service;

import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.domain.entity.Rappel;
import com.nourtech.ecareplus.communication.infrastructure.persistence.NotificationRepository;
import com.nourtech.ecareplus.communication.infrastructure.persistence.RappelRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.Instant; import java.util.UUID;

@Service @Transactional(readOnly=true)
public class CommunicationPatientService {
    @org.springframework.beans.factory.annotation.Autowired private com.nourtech.ecareplus.portail.application.PortailService portail;
    private final RappelRepository rappels; private final NotificationRepository notifications;
    public CommunicationPatientService(RappelRepository r, NotificationRepository n) { rappels=r; notifications=n; }
    public Page<RappelMobile> rappels(UUID patient, Pageable page) { portail.soi(patient,true); return rappels.findAllByPatientId(patient,page).map(RappelMobile::depuis); }
    public Page<NotificationMobile> notifications(UUID user, Pageable page) { portail.soi(user,true); return notifications.findAllByUtilisateurId(user,page).map(NotificationMobile::depuis); }
    @Transactional public void lireRappel(UUID patient, UUID id) { portail.soi(patient,true); Rappel r=rappels.findById(id).orElseThrow(()->new RessourceIntrouvableException("Rappel",id));
        if (!r.getPatient().getId().equals(patient)) throw new AccessDeniedException("Ce rappel ne vous appartient pas"); r.marquerCommeLu(); }
    @Transactional public void lireNotification(UUID user, UUID id) { portail.soi(user,true); Notification n=notifications.findById(id).orElseThrow(()->new RessourceIntrouvableException("Notification",id));
        if (!n.getUtilisateur().getId().equals(user)) throw new AccessDeniedException("Cette notification ne vous appartient pas"); n.marquerCommeLue(Instant.now()); }
    public record RappelMobile(UUID id,String type,String titre,String message,Instant dateHeurePrevue,String statut,String canal) {
        static RappelMobile depuis(Rappel r){return new RappelMobile(r.getId(),r.getTypeRappel().name(),r.getTitre(),r.getMessage(),r.getDateHeurePrevue(),r.getStatut().name(),r.getCanal().name());}}
    public record NotificationMobile(UUID id,String titre,String message,String type,String statut,Instant dateEnvoi,Instant dateLecture,String typeRessource,UUID ressourceId) {
        static NotificationMobile depuis(Notification n){return new NotificationMobile(n.getId(),n.getTitre(),n.getMessage(),n.getTypeNotification(),n.getStatutEnvoi().name(),n.getDateEnvoi(),n.getDateLecture(),n.getTypeRessource(),n.getRessourceId());}}
}
