package com.nourtech.ecareplus.communication.api.mapper;

import com.nourtech.ecareplus.communication.api.dto.request.CreerContenuEducatifRequest;
import com.nourtech.ecareplus.communication.api.dto.request.CreerNotificationRequest;
import com.nourtech.ecareplus.communication.api.dto.request.ProgrammerRappelRequest;
import com.nourtech.ecareplus.communication.api.dto.request.ProgrammerRendezVousRequest;
import com.nourtech.ecareplus.communication.api.dto.response.ContenuEducatifResponse;
import com.nourtech.ecareplus.communication.api.dto.response.NotificationResponse;
import com.nourtech.ecareplus.communication.api.dto.response.RappelResponse;
import com.nourtech.ecareplus.communication.api.dto.response.RendezVousResponse;
import com.nourtech.ecareplus.communication.application.command.CreerContenuEducatifCommand;
import com.nourtech.ecareplus.communication.application.command.CreerNotificationCommand;
import com.nourtech.ecareplus.communication.application.command.ProgrammerRappelCommand;
import com.nourtech.ecareplus.communication.application.command.ProgrammerRendezVousCommand;
import com.nourtech.ecareplus.communication.domain.entity.ContenuEducatif;
import com.nourtech.ecareplus.communication.domain.entity.Notification;
import com.nourtech.ecareplus.communication.domain.entity.Rappel;
import com.nourtech.ecareplus.communication.domain.entity.RendezVous;
import org.springframework.stereotype.Component;

@Component
public class CommunicationMapper {

    public ProgrammerRappelCommand toCommand(ProgrammerRappelRequest request) {
        return new ProgrammerRappelCommand(
                request.patientId(), request.priseMedicamentId(), request.typeRappel(), request.titre(),
                request.message(), request.dateHeurePrevue(), request.frequence(), request.canal(),
                request.dateFin()
        );
    }

    public RappelResponse toResponse(Rappel rappel) {
        return new RappelResponse(
                rappel.getId(), rappel.getPatient().getId(),
                rappel.getPriseMedicament() != null ? rappel.getPriseMedicament().getId() : null,
                rappel.getTypeRappel(), rappel.getTitre(), rappel.getMessage(), rappel.getDateHeurePrevue(),
                rappel.getFrequence(), rappel.getStatut(), rappel.getCanal(), rappel.getDateFin(),
                rappel.getDerniereExecution(), rappel.getVersion()
        );
    }

    public ProgrammerRendezVousCommand toCommand(ProgrammerRendezVousRequest request) {
        return new ProgrammerRendezVousCommand(
                request.patientId(), request.motif(), request.dateHeure(), request.lieu(),
                request.typeRendezVous(), request.commentaire()
        );
    }

    public RendezVousResponse toResponse(RendezVous rendezVous) {
        return new RendezVousResponse(
                rendezVous.getId(), rendezVous.getPatient().getId(), rendezVous.getMotif(),
                rendezVous.getDateHeure(), rendezVous.getLieu(), rendezVous.getTypeRendezVous(),
                rendezVous.getStatut(), rendezVous.getCommentaire(), rendezVous.getVersion()
        );
    }

    public CreerNotificationCommand toCommand(CreerNotificationRequest request) {
        return new CreerNotificationCommand(
                request.utilisateurId(), request.titre(), request.message(), request.typeNotification(),
                request.canal(), request.sensible()
        );
    }

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getUtilisateur().getId(), notification.getTitre(),
                notification.getMessage(), notification.getTypeNotification(), notification.getCanal(),
                notification.getDateEnvoi(), notification.getDateLecture(), notification.getStatutEnvoi(),
                notification.isSensible(), notification.getVersion()
        );
    }

    public CreerContenuEducatifCommand toCommand(CreerContenuEducatifRequest request) {
        return new CreerContenuEducatifCommand(
                request.auteurId(), request.titre(), request.resume(), request.contenu(), request.typeContenu(),
                request.langue(), request.pathologieCible(), request.urlMedia()
        );
    }

    public ContenuEducatifResponse toResponse(ContenuEducatif contenu) {
        return new ContenuEducatifResponse(
                contenu.getId(), contenu.getAuteur().getId(), contenu.getTitre(), contenu.getResume(),
                contenu.getContenu(), contenu.getTypeContenu(), contenu.getLangue(), contenu.getPathologieCible(),
                contenu.getStatutValidation(), contenu.getDatePublication(), contenu.getUrlMedia(),
                contenu.getVersion()
        );
    }
}
