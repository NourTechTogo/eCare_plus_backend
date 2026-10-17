package com.nourtech.ecareplus.subscription.api.mapper;

import com.nourtech.ecareplus.subscription.api.dto.request.CreerAbonnementRequest;
import com.nourtech.ecareplus.subscription.api.dto.response.AbonnementStructureResponse;
import com.nourtech.ecareplus.subscription.application.command.CreerAbonnementCommand;
import com.nourtech.ecareplus.subscription.domain.entity.AbonnementStructure;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public CreerAbonnementCommand toCommand(CreerAbonnementRequest request) {
        return new CreerAbonnementCommand(
                request.structureId(), request.formule(), request.dateDebut(), request.dateFin(),
                request.nombrePatientsAutorises(), request.montant(), request.frequencePaiement()
        );
    }

    public AbonnementStructureResponse toResponse(AbonnementStructure entity) {
        return new AbonnementStructureResponse(
                entity.getId(), entity.getStructure().getId(), entity.getFormule(),
                entity.getDateDebut(), entity.getDateFin(), entity.getNombrePatientsAutorises(),
                entity.getMontant(), entity.getStatut(), entity.getFrequencePaiement()
        );
    }
}
