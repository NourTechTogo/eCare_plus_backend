package com.nourtech.ecareplus.identity.api.mapper;

import com.nourtech.ecareplus.identity.api.dto.request.CreerAdministrateurStructureRequest;
import com.nourtech.ecareplus.identity.api.dto.request.CreerInfirmierRequest;
import com.nourtech.ecareplus.identity.api.dto.request.CreerMedecinRequest;
import com.nourtech.ecareplus.identity.api.dto.request.CreerPatientRequest;
import com.nourtech.ecareplus.identity.api.dto.request.CreerProcheAidantRequest;
import com.nourtech.ecareplus.identity.api.dto.response.AdministrateurStructureResponse;
import com.nourtech.ecareplus.identity.api.dto.response.InfirmierResponse;
import com.nourtech.ecareplus.identity.api.dto.response.MedecinResponse;
import com.nourtech.ecareplus.identity.api.dto.response.PatientResponse;
import com.nourtech.ecareplus.identity.api.dto.response.ProcheAidantResponse;
import com.nourtech.ecareplus.identity.api.dto.response.ProfessionnelSanteResponse;
import com.nourtech.ecareplus.identity.api.dto.response.UtilisateurResponse;
import com.nourtech.ecareplus.identity.application.command.CreerAdministrateurStructureCommand;
import com.nourtech.ecareplus.identity.application.command.CreerInfirmierCommand;
import com.nourtech.ecareplus.identity.application.command.CreerMedecinCommand;
import com.nourtech.ecareplus.identity.application.command.CreerPatientCommand;
import com.nourtech.ecareplus.identity.application.command.CreerProcheAidantCommand;
import com.nourtech.ecareplus.identity.domain.entity.AdministrateurStructure;
import com.nourtech.ecareplus.identity.domain.entity.Infirmier;
import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProcheAidant;
import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import org.springframework.stereotype.Component;

@Component
public class IdentityMapper {

    public CreerPatientCommand toCommand(CreerPatientRequest request, String motDePasseHash) {
        return new CreerPatientCommand(
                request.numeroPatient(), request.nom(), request.prenoms(), request.sexe(),
                request.dateNaissance(), request.telephone(), request.email(), motDePasseHash,
                request.languePreferee()
        );
    }

    public CreerProcheAidantCommand toCommand(CreerProcheAidantRequest request, String motDePasseHash) {
        return new CreerProcheAidantCommand(
                request.nom(), request.prenoms(), request.sexe(), request.dateNaissance(),
                request.telephone(), request.email(), motDePasseHash, request.languePreferee(),
                request.lienAvecPatient()
        );
    }

    public CreerMedecinCommand toCommand(CreerMedecinRequest request, String motDePasseHash) {
        return new CreerMedecinCommand(
                request.numeroProfessionnel(), request.numeroOrdre(), request.nom(), request.prenoms(),
                request.sexe(), request.dateNaissance(), request.telephone(), request.email(),
                motDePasseHash, request.languePreferee(), request.specialite(),
                request.domaineSpecialisation(), request.datePriseFonction(), request.structureId()
        );
    }

    public CreerInfirmierCommand toCommand(CreerInfirmierRequest request, String motDePasseHash) {
        return new CreerInfirmierCommand(
                request.numeroProfessionnel(), request.nom(), request.prenoms(), request.sexe(),
                request.dateNaissance(), request.telephone(), request.email(), motDePasseHash,
                request.languePreferee(), request.specialite(), request.service(),
                request.niveauResponsabilite(), request.datePriseFonction(), request.structureId()
        );
    }

    public CreerAdministrateurStructureCommand toCommand(
            CreerAdministrateurStructureRequest request,
            String motDePasseHash
    ) {
        return new CreerAdministrateurStructureCommand(
                request.nom(), request.prenoms(), request.sexe(), request.dateNaissance(),
                request.telephone(), request.email(), motDePasseHash, request.languePreferee(),
                request.fonctionAdministrative(), request.niveauAcces(), request.structureId()
        );
    }

    public UtilisateurResponse toResponse(Utilisateur utilisateur) {
        return new UtilisateurResponse(
                utilisateur.getId(), utilisateur.getNom(), utilisateur.getPrenoms(), utilisateur.getSexe(),
                utilisateur.getDateNaissance(), utilisateur.getTelephone(), utilisateur.getEmail(),
                utilisateur.getLanguePreferee(), utilisateur.getStatutCompte(),
                utilisateur.getDerniereConnexion(), utilisateur.getDateCreation(),
                utilisateur.getDateModification()
        );
    }

    public PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                toResponse((Utilisateur) patient), patient.getNumeroPatient(), patient.getGroupeSanguin(),
                patient.getAdresse(), patient.getPersonneAContacter(), patient.getTelephoneUrgence(),
                patient.getModeConnexionPrefere(), patient.getDateInscription(), patient.getStatutSuivi()
        );
    }

    public ProcheAidantResponse toResponse(ProcheAidant proche) {
        return new ProcheAidantResponse(
                toResponse((Utilisateur) proche), proche.getLienAvecPatient(),
                proche.getTelephoneSecondaire(), proche.getStatutAutorisation()
        );
    }

    public ProfessionnelSanteResponse toResponse(ProfessionnelSante professionnel) {
        return new ProfessionnelSanteResponse(
                toResponse((Utilisateur) professionnel), professionnel.getNumeroProfessionnel(),
                professionnel.getSpecialite(), professionnel.getFonction(),
                professionnel.getStatutProfessionnel(), professionnel.getDatePriseFonction(),
                professionnel.getStructure().getId()
        );
    }

    public MedecinResponse toResponse(Medecin medecin) {
        return new MedecinResponse(
                toResponse((ProfessionnelSante) medecin), medecin.getNumeroOrdre(),
                medecin.getDomaineSpecialisation()
        );
    }

    public InfirmierResponse toResponse(Infirmier infirmier) {
        return new InfirmierResponse(
                toResponse((ProfessionnelSante) infirmier), infirmier.getService(),
                infirmier.getNiveauResponsabilite()
        );
    }

    public AdministrateurStructureResponse toResponse(AdministrateurStructure administrateur) {
        return new AdministrateurStructureResponse(
                toResponse((Utilisateur) administrateur), administrateur.getFonctionAdministrative(),
                administrateur.getNiveauAcces(), administrateur.getStructure().getId()
        );
    }
}
