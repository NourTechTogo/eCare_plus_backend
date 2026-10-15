package com.nourtech.ecareplus.alert.api.mapper;

import com.nourtech.ecareplus.alert.api.dto.request.CreerRegleAlerteRequest;
import com.nourtech.ecareplus.alert.api.dto.request.EnregistrerInterventionRequest;
import com.nourtech.ecareplus.alert.api.dto.request.GenererAlerteRequest;
import com.nourtech.ecareplus.alert.api.dto.response.AlerteResponse;
import com.nourtech.ecareplus.alert.api.dto.response.InterventionSuiviResponse;
import com.nourtech.ecareplus.alert.api.dto.response.RegleAlerteResponse;
import com.nourtech.ecareplus.alert.application.command.CreerRegleAlerteCommand;
import com.nourtech.ecareplus.alert.application.command.EnregistrerInterventionCommand;
import com.nourtech.ecareplus.alert.application.command.GenererAlerteCommand;
import com.nourtech.ecareplus.alert.domain.entity.Alerte;
import com.nourtech.ecareplus.alert.domain.entity.InterventionSuivi;
import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AlertMapper {

    public CreerRegleAlerteCommand toCommand(CreerRegleAlerteRequest request) {
        return new CreerRegleAlerteCommand(
                request.nom(), request.description(), request.typeMesure(), request.operateur(),
                request.seuilMin(), request.seuilMax(), request.nombreOccurrences(),
                request.periodeObservation(), request.niveauPriorite()
        );
    }

    public RegleAlerteResponse toResponse(RegleAlerte regle) {
        return new RegleAlerteResponse(
                regle.getId(), regle.getNom(), regle.getDescription(), regle.getTypeMesure(),
                regle.getOperateur(), regle.getSeuilMin(), regle.getSeuilMax(), regle.getNombreOccurrences(),
                regle.getPeriodeObservation(), regle.getNiveauPriorite(), regle.isActif(),
                regle.getDateValidation(), regle.getVersion()
        );
    }

    public GenererAlerteCommand toCommand(GenererAlerteRequest request) {
        return new GenererAlerteCommand(
                request.patientId(), request.mesureId(), request.regleId(), request.typeAlerte(),
                request.titre(), request.description(), request.niveauPriorite()
        );
    }

    public AlerteResponse toResponse(Alerte alerte) {
        return new AlerteResponse(
                alerte.getId(), alerte.getPatient().getId(),
                alerte.getMesure() != null ? alerte.getMesure().getId() : null,
                alerte.getRegle() != null ? alerte.getRegle().getId() : null,
                alerte.getProfessionnel() != null ? alerte.getProfessionnel().getId() : null,
                alerte.getTypeAlerte(), alerte.getOrigine(), alerte.getTitre(), alerte.getDescription(), alerte.getNiveauPriorite(),
                alerte.getStatut(), alerte.getDatePriseEnCharge(), alerte.getDateCloture(),
                alerte.getMotifCloture(), alerte.getVersion()
        );
    }

    public EnregistrerInterventionCommand toCommand(UUID alerteId, EnregistrerInterventionRequest request) {
        return new EnregistrerInterventionCommand(
                alerteId, request.typeIntervention(), request.description(), request.dateIntervention()
        );
    }

    public InterventionSuiviResponse toResponse(InterventionSuivi intervention) {
        return new InterventionSuiviResponse(
                intervention.getId(), intervention.getAlerte().getId(), intervention.getTypeIntervention(),
                intervention.getDescription(), intervention.getDateIntervention(), intervention.getResultat(),
                intervention.getActionSuivante(), intervention.getDateProchaineAction(), intervention.getVersion()
        );
    }
}
