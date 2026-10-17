package com.nourtech.ecareplus.organization.api.mapper;

import com.nourtech.ecareplus.organization.api.dto.request.AffecterPatientRequest;
import com.nourtech.ecareplus.organization.api.dto.request.CreerStructureSanteRequest;
import com.nourtech.ecareplus.organization.api.dto.response.AffectationPatientResponse;
import com.nourtech.ecareplus.organization.api.dto.response.StructureSanteResponse;
import com.nourtech.ecareplus.organization.application.command.AffecterPatientCommand;
import com.nourtech.ecareplus.organization.application.command.CreerStructureSanteCommand;
import com.nourtech.ecareplus.organization.domain.entity.AffectationPatient;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    public CreerStructureSanteCommand toCommand(CreerStructureSanteRequest request) {
        return new CreerStructureSanteCommand(
                request.nom(), request.typeStructure(), request.adresse(), request.ville(), request.region(),
                request.telephone(), request.email(), request.numeroAutorisation()
        );
    }

    public AffecterPatientCommand toCommand(AffecterPatientRequest request) {
        return new AffecterPatientCommand(
                request.patientId(), request.professionnelId(), request.structureId(), request.role(),
                request.dateDebut(), request.professionnelPrincipal()
        );
    }

    public StructureSanteResponse toResponse(StructureSante structure) {
        return new StructureSanteResponse(
                structure.getId(), structure.getNom(), structure.getTypeStructure(), structure.getAdresse(),
                structure.getVille(), structure.getRegion(), structure.getTelephone(), structure.getEmail(),
                structure.getStatut(), structure.getNumeroAutorisation(), structure.getDateAdhesion(),
                structure.getVersion(), structure.getDateCreation(), structure.getDateModification()
        );
    }

    public AffectationPatientResponse toResponse(AffectationPatient affectation) {
        return new AffectationPatientResponse(
                affectation.getId(), affectation.getPatient().getId(), affectation.getProfessionnel().getId(),
                affectation.getStructure().getId(), affectation.getDateDebut(), affectation.getDateFin(),
                affectation.getRoleProfessionnel(), affectation.getStatut(),
                affectation.isProfessionnelPrincipal(), affectation.getVersion()
        );
    }
}
