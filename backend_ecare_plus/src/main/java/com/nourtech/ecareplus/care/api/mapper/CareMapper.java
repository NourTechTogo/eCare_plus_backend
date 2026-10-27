package com.nourtech.ecareplus.care.api.mapper;

import com.nourtech.ecareplus.care.api.dto.request.AjouterMedicamentRequest;
import com.nourtech.ecareplus.care.api.dto.request.AjouterObservationRequest;
import com.nourtech.ecareplus.care.api.dto.request.AjouterTraitementRequest;
import com.nourtech.ecareplus.care.api.dto.request.DeclarerPathologieRequest;
import com.nourtech.ecareplus.care.api.dto.request.DeclarerSymptomeRequest;
import com.nourtech.ecareplus.care.api.dto.request.DefinirObjectifRequest;
import com.nourtech.ecareplus.care.api.dto.request.EnregistrerMesureRequest;
import com.nourtech.ecareplus.care.api.dto.request.GenererRapportRequest;
import com.nourtech.ecareplus.care.api.dto.request.OuvrirDossierRequest;
import com.nourtech.ecareplus.care.api.dto.request.ProgrammerConfirmationPriseRequest;
import com.nourtech.ecareplus.care.api.dto.request.ProgrammerPriseRequest;
import com.nourtech.ecareplus.care.api.dto.response.ConfirmationPriseResponse;
import com.nourtech.ecareplus.care.api.dto.response.DeclarationSymptomeResponse;
import com.nourtech.ecareplus.care.api.dto.response.DossierSuiviResponse;
import com.nourtech.ecareplus.care.api.dto.response.MedicamentResponse;
import com.nourtech.ecareplus.care.api.dto.response.MesureSanteResponse;
import com.nourtech.ecareplus.care.api.dto.response.ObjectifSuiviResponse;
import com.nourtech.ecareplus.care.api.dto.response.ObservationSuiviResponse;
import com.nourtech.ecareplus.care.api.dto.response.PathologieResponse;
import com.nourtech.ecareplus.care.api.dto.response.PriseMedicamentResponse;
import com.nourtech.ecareplus.care.api.dto.response.RapportSuiviResponse;
import com.nourtech.ecareplus.care.api.dto.response.TraitementResponse;
import com.nourtech.ecareplus.care.application.command.AjouterMedicamentCommand;
import com.nourtech.ecareplus.care.application.command.AjouterObservationCommand;
import com.nourtech.ecareplus.care.application.command.AjouterTraitementCommand;
import com.nourtech.ecareplus.care.application.command.DeclarerPathologieCommand;
import com.nourtech.ecareplus.care.application.command.DeclarerSymptomeCommand;
import com.nourtech.ecareplus.care.application.command.DefinirObjectifCommand;
import com.nourtech.ecareplus.care.application.command.EnregistrerMesureCommand;
import com.nourtech.ecareplus.care.application.command.GenererRapportCommand;
import com.nourtech.ecareplus.care.application.command.OuvrirDossierCommand;
import com.nourtech.ecareplus.care.application.command.ProgrammerConfirmationPriseCommand;
import com.nourtech.ecareplus.care.application.command.ProgrammerPriseCommand;
import com.nourtech.ecareplus.care.domain.entity.ConfirmationPrise;
import com.nourtech.ecareplus.care.domain.entity.DeclarationSymptome;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.Medicament;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.care.domain.entity.ObjectifSuivi;
import com.nourtech.ecareplus.care.domain.entity.ObservationSuivi;
import com.nourtech.ecareplus.care.domain.entity.Pathologie;
import com.nourtech.ecareplus.care.domain.entity.PriseMedicament;
import com.nourtech.ecareplus.care.domain.entity.RapportSuivi;
import com.nourtech.ecareplus.care.domain.entity.Traitement;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CareMapper {

    public OuvrirDossierCommand toCommand(OuvrirDossierRequest request) {
        return new OuvrirDossierCommand(request.patientId(), request.structureId(), request.frequenceSuivi());
    }

    public DefinirObjectifCommand toCommand(UUID dossierId, DefinirObjectifRequest request) {
        return new DefinirObjectifCommand(
                dossierId, request.medecinId(), request.libelle(), request.description(), request.cibleMin(),
                request.cibleMax(), request.unite(), request.dateDebut(), request.dateFin()
        );
    }

    public DossierSuiviResponse toResponse(DossierSuivi dossier) {
        return new DossierSuiviResponse(
                dossier.getId(), dossier.getPatient().getId(), dossier.getStructure()==null?null:dossier.getStructure().getId(),
                dossier.getDateOuverture(), dossier.getStatut(), dossier.getResumeMedical(),
                dossier.getFrequenceSuivi(), dossier.getProchainControle(),
                dossier.getDateDerniereMiseAJour(), dossier.getVersion()
        );
    }

    public ObjectifSuiviResponse toResponse(ObjectifSuivi objectif) {
        return new ObjectifSuiviResponse(
                objectif.getId(), objectif.getDossier().getId(), objectif.getDefiniPar().getId(),
                objectif.getLibelle(), objectif.getDescription(), objectif.getValeurCibleMin(),
                objectif.getValeurCibleMax(), objectif.getUnite(), objectif.getDateDebut(),
                objectif.getDateFin(), objectif.getStatut(), objectif.getVersion()
        );
    }

    public DeclarerPathologieCommand toCommand(UUID dossierId, DeclarerPathologieRequest request) {
        return new DeclarerPathologieCommand(
                dossierId, request.nom(), request.code(), request.description(),
                request.dateDiagnosticDeclaree(), request.niveauRisque()
        );
    }

    public PathologieResponse toResponse(Pathologie pathologie) {
        return new PathologieResponse(
                pathologie.getId(), pathologie.getDossier().getId(), pathologie.getNom(),
                pathologie.getCode(), pathologie.getDescription(), pathologie.getDateDiagnosticDeclaree(),
                pathologie.getStatut(), pathologie.getNiveauRisque(), pathologie.getVersion()
        );
    }

    public EnregistrerMesureCommand toCommand(UUID dossierId, EnregistrerMesureRequest request) {
        return new EnregistrerMesureCommand(
                dossierId, request.patientId(), request.typeMesure(), request.valeurPrincipale(),
                request.valeurSecondaire(), request.unite(), request.dateMesure(), request.contexteMesure(),
                request.commentaire(), request.source(), request.statutSynchronisation()
        );
    }

    public MesureSanteResponse toResponse(MesureSante mesure) {
        return new MesureSanteResponse(
                mesure.getId(), mesure.getPatient().getId(), mesure.getDossier().getId(), mesure.getTypeMesure(),
                mesure.getValeurPrincipale(), mesure.getValeurSecondaire(), mesure.getUnite(), mesure.getDateMesure(),
                mesure.getContexteMesure(), mesure.getCommentaire(), mesure.getSource(),
                mesure.getStatutSynchronisation(), mesure.isValeurConfirmee(), mesure.getVersion()
        );
    }

    public AjouterTraitementCommand toCommand(UUID dossierId, AjouterTraitementRequest request) {
        return new AjouterTraitementCommand(
                dossierId, request.libelle(), request.description(), request.dateDebut(), request.dateFin(),
                request.prescripteur(), request.consignesGenerales()
        );
    }

    public TraitementResponse toResponse(Traitement traitement) {
        return new TraitementResponse(
                traitement.getId(), traitement.getDossier().getId(), traitement.getLibelle(),
                traitement.getDescription(), traitement.getDateDebut(), traitement.getDateFin(),
                traitement.getStatut(), traitement.getPrescripteur(), traitement.getConsignesGenerales(),
                traitement.getVersion()
        );
    }

    public AjouterMedicamentCommand toCommand(AjouterMedicamentRequest request) {
        return new AjouterMedicamentCommand(
                request.nomCommercial(), request.denomination(), request.forme(), request.dosage(),
                request.uniteDosage()
        );
    }

    public MedicamentResponse toResponse(Medicament medicament) {
        return new MedicamentResponse(
                medicament.getId(), medicament.getNomCommercial(), medicament.getDenomination(),
                medicament.getForme(), medicament.getDosage(), medicament.getUniteDosage(), medicament.getVersion()
        );
    }

    public ProgrammerPriseCommand toCommand(UUID traitementId, ProgrammerPriseRequest request) {
        return new ProgrammerPriseCommand(
                traitementId, request.medicamentId(), request.quantite(), request.unite(), request.frequence(),
                request.heurePrevue(), request.momentPrise(), request.voieAdministration(), request.consigne()
        );
    }

    public PriseMedicamentResponse toResponse(PriseMedicament prise) {
        return new PriseMedicamentResponse(
                prise.getId(), prise.getTraitement().getId(), prise.getMedicament().getId(), prise.getQuantite(),
                prise.getUnite(), prise.getFrequence(), prise.getHeurePrevue(), prise.getMomentPrise(),
                prise.getVoieAdministration(), prise.getConsigne(), prise.isActif(), prise.getVersion()
        );
    }

    public ProgrammerConfirmationPriseCommand toCommand(UUID priseMedicamentId, ProgrammerConfirmationPriseRequest request) {
        return new ProgrammerConfirmationPriseCommand(priseMedicamentId, request.datePrevue());
    }

    public ConfirmationPriseResponse toResponse(ConfirmationPrise confirmation) {
        return new ConfirmationPriseResponse(
                confirmation.getId(), confirmation.getPriseMedicament().getId(), confirmation.getDatePrevue(),
                confirmation.getDateConfirmation(), confirmation.getStatut(), confirmation.getCommentaire(),
                confirmation.getVersion()
        );
    }

    public DeclarerSymptomeCommand toCommand(DeclarerSymptomeRequest request) {
        return new DeclarerSymptomeCommand(
                request.patientId(), request.symptome(), request.intensite(), request.description(),
                request.dateApparition(), request.persistant()
        );
    }

    public DeclarationSymptomeResponse toResponse(DeclarationSymptome declaration) {
        return new DeclarationSymptomeResponse(
                declaration.getId(), declaration.getPatient().getId(), declaration.getSymptome(),
                declaration.getIntensite(), declaration.getDescription(), declaration.getDateApparition(),
                declaration.getDateDeclaration(), declaration.isPersistant(), declaration.getStatutEvaluation(),
                declaration.getVersion()
        );
    }

    public AjouterObservationCommand toCommand(UUID dossierId, AjouterObservationRequest request) {
        return new AjouterObservationCommand(
                dossierId, request.professionnelId(), request.contenu(), request.typeObservation(),
                request.confidentielle()
        );
    }

    public ObservationSuiviResponse toResponse(ObservationSuivi observation) {
        return new ObservationSuiviResponse(
                observation.getId(), observation.getDossier().getId(), observation.getProfessionnel().getId(),
                observation.getContenu(), observation.getTypeObservation(), observation.isConfidentielle(),
                observation.isArchivee(), observation.getVersion()
        );
    }

    public GenererRapportCommand toCommand(UUID dossierId, GenererRapportRequest request) {
        return new GenererRapportCommand(
                dossierId, request.titre(), request.dateDebut(), request.dateFin(), request.resume(),
                request.format()
        );
    }

    public RapportSuiviResponse toResponse(RapportSuivi rapport) {
        return new RapportSuiviResponse(
                rapport.getId(), rapport.getDossier().getId(), rapport.getTitre(), rapport.getDateDebut(),
                rapport.getDateFin(), rapport.getResume(), rapport.getDateGeneration(), rapport.getFormat(),
                rapport.getCheminFichier(), rapport.getStatut(), rapport.getVersion()
        );
    }
}
