package com.nourtech.ecareplus.communication.application.service;

import com.nourtech.ecareplus.communication.application.command.CreerNotificationCommand;
import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.infrastructure.persistence.NotificationRepository;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import com.nourtech.ecareplus.identity.infrastructure.persistence.UtilisateurRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;

    public NotificationService(NotificationRepository notificationRepository, UtilisateurRepository utilisateurRepository) {
        this.notificationRepository = notificationRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public Notification creer(CreerNotificationCommand commande) {
        Utilisateur utilisateur = utilisateurRepository.findById(commande.utilisateurId())
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur", commande.utilisateurId()));

        return notificationRepository.save(new Notification(
                utilisateur,
                commande.titre(),
                commande.message(),
                commande.typeNotification(),
                commande.canal(),
                commande.sensible()
        ));
    }

    public Notification envoyer(UUID notificationId, Instant maintenant) {
        Notification notification = charger(notificationId);
        verifierPublicationInterne(notification);
        notification.envoyer(maintenant);
        return notification;
    }

    public Notification marquerEnEchec(UUID notificationId) {
        Notification notification = charger(notificationId);
        notification.marquerEnEchec();
        return notification;
    }

    public Notification reessayerEnvoi(UUID notificationId, Instant maintenant) {
        Notification notification = charger(notificationId);
        verifierPublicationInterne(notification);
        notification.reessayerEnvoi(maintenant);
        return notification;
    }

    public Notification marquerCommeLue(UUID notificationId, Instant maintenant) {
        Notification notification = charger(notificationId);
        notification.marquerCommeLue(maintenant);
        return notification;
    }

    public Notification annuler(UUID notificationId) {
        Notification notification = charger(notificationId);
        notification.annuler();
        return notification;
    }

    public Notification consulter(UUID notificationId) {
        return charger(notificationId);
    }

    private Notification charger(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RessourceIntrouvableException("Notification", notificationId));
    }
    private static void verifierPublicationInterne(Notification notification){if(notification.getCanal()!=com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification.APPLICATION)throw new com.nourtech.ecareplus.shared.application.exception.ConflitMetierException("L'envoi externe exige un transport et son accusé fournisseur ; cette route ne peut pas l'attester");}
}
