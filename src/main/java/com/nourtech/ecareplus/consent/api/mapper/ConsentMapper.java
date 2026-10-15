package com.nourtech.ecareplus.consent.api.mapper;

import com.nourtech.ecareplus.consent.api.dto.request.AccorderConsentementRequest;
import com.nourtech.ecareplus.consent.api.dto.request.AutoriserProcheRequest;
import com.nourtech.ecareplus.consent.api.dto.response.AutorisationPartageResponse;
import com.nourtech.ecareplus.consent.api.dto.response.ConsentementResponse;
import com.nourtech.ecareplus.consent.application.command.AccorderConsentementCommand;
import com.nourtech.ecareplus.consent.application.command.AutoriserProcheCommand;
import com.nourtech.ecareplus.consent.domain.entity.AutorisationPartage;
import com.nourtech.ecareplus.consent.domain.entity.Consentement;
import org.springframework.stereotype.Component;

@Component
public class ConsentMapper {

    public AccorderConsentementCommand toCommand(AccorderConsentementRequest request, String adresseIp) {
        return new AccorderConsentementCommand(
                request.patientId(), request.typeConsentement(), request.description(),
                request.versionDocument(), adresseIp, request.dateAcceptation()
        );
    }

    public AutoriserProcheCommand toCommand(AutoriserProcheRequest request) {
        return new AutoriserProcheCommand(
                request.patientId(), request.procheAidantId(), request.consentementId(),
                request.typeDonnee(), request.dateDebut(), request.dateFin(),
                request.peutConsulter(), request.peutRecevoirNotification()
        );
    }

    public ConsentementResponse toResponse(Consentement entity) {
        return new ConsentementResponse(
                entity.getId(), entity.getPatient().getId(), entity.getTypeConsentement(),
                entity.getDescription(), entity.getDateAcceptation(), entity.getDateRetrait(),
                entity.getStatut(), entity.getVersionDocument()
        );
    }

    public AutorisationPartageResponse toResponse(AutorisationPartage entity) {
        return new AutorisationPartageResponse(
                entity.getId(), entity.getPatient().getId(), entity.getProcheAidant().getId(),
                entity.getConsentement().getId(), entity.getTypeDonnee(), entity.getDateDebut(),
                entity.getDateFin(), entity.isPeutConsulter(),
                entity.isPeutRecevoirNotification(), entity.getStatut()
        );
    }
}
