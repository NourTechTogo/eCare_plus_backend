package com.nourtech.ecareplus.sync.api.mapper;

import com.nourtech.ecareplus.sync.api.dto.request.RecevoirSynchronisationRequest;
import com.nourtech.ecareplus.sync.api.dto.response.ElementSynchronisationResponse;
import com.nourtech.ecareplus.sync.application.command.RecevoirSynchronisationCommand;
import com.nourtech.ecareplus.sync.domain.entity.ElementSynchronisation;
import org.springframework.stereotype.Component;

@Component
public class SyncMapper {

    public RecevoirSynchronisationCommand toCommand(RecevoirSynchronisationRequest request) {
        return new RecevoirSynchronisationCommand(
                request.utilisateurId(), request.typeRessource(), request.identifiantAppareil(),
                request.identifiantLocal(), request.contenu(), request.dateCreationLocale()
        );
    }

    public ElementSynchronisationResponse toResponse(ElementSynchronisation entity) {
        return new ElementSynchronisationResponse(
                entity.getId(), entity.getUtilisateur().getId(), entity.getTypeRessource(),
                entity.getIdentifiantAppareil(), entity.getIdentifiantLocal(),
                entity.getDateCreationLocale(), entity.getNombreTentatives(),
                entity.getStatut(), entity.getMessageErreur()
        );
    }
}
