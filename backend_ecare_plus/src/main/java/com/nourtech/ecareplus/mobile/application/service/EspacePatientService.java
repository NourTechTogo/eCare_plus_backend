package com.nourtech.ecareplus.mobile.application.service;

import com.nourtech.ecareplus.auth.api.dto.ProfilAuthentifieResponse;
import com.nourtech.ecareplus.care.application.command.EnregistrerMesureCommand;
import com.nourtech.ecareplus.care.application.service.MesureSanteService;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.care.domain.enumeration.SourceMesure;
import com.nourtech.ecareplus.care.domain.enumeration.StatutDossier;
import com.nourtech.ecareplus.care.infrastructure.persistence.DossierSuiviRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.MesureSanteRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.mobile.api.dto.CreerMesureMobileRequest;
import com.nourtech.ecareplus.mobile.api.dto.EspacePatientResponse;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EspacePatientService {
    private final PatientRepository patients;
    private final DossierSuiviRepository dossiers;
    private final MesureSanteRepository mesures;
    private final MesureSanteService mesureSanteService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.nourtech.ecareplus.portail.application.PortailService portail;

    public EspacePatientService(PatientRepository patients, DossierSuiviRepository dossiers,
                                MesureSanteRepository mesures, MesureSanteService mesureSanteService) {
        this.patients = patients;
        this.dossiers = dossiers;
        this.mesures = mesures;
        this.mesureSanteService = mesureSanteService;
    }

    @Transactional
    public void assurerDossier(UUID id) { portail.dossier(portail.soi(id,false)); }

    public ProfilAuthentifieResponse profil(UUID id) { return ProfilAuthentifieResponse.depuis(patient(id)); }

    @Transactional
    public EspacePatientResponse.Dossier dossier(UUID id) {
        portail.soi(id,true);
        return EspacePatientResponse.Dossier.depuis(dossierActif(id));
    }

    public Page<EspacePatientResponse.Mesure> mesures(UUID id, Pageable pageable) {
        portail.soi(id,true);
        patient(id);
        return mesures.findAllByPatientId(id, pageable).map(EspacePatientResponse.Mesure::depuis);
    }

    @Transactional
    public EspacePatientResponse.Mesure enregistrer(UUID id, CreerMesureMobileRequest request) {
        portail.soi(id,true);
        Patient patient = patient(id);
        // A later institutional admission must not change the identity of an uncertain retry.
        DossierSuivi dossier = request.sourceRef() == null ? dossierActif(id)
                : mesures.findByPatientIdAndSourceRef(id, request.sourceRef()).map(MesureSante::getDossier)
                    .orElseGet(() -> dossierActif(id));
        MesureSante mesure = mesureSanteService.enregistrer(new EnregistrerMesureCommand(
                dossier.getId(), patient.getId(), request.typeMesure(), request.valeurPrincipale(),
                request.valeurSecondaire(), request.unite(), request.dateMesure(), request.contexteMesure(),
                request.commentaire(), SourceMesure.PATIENT, "SYNCHRONISEE", request.sourceRef()));
        return EspacePatientResponse.Mesure.depuis(mesure);
    }

    private Patient patient(UUID id) {
        return patients.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Patient", id));
    }

    private DossierSuivi dossierActif(UUID id) {
        return portail.dossier(patient(id));
    }
}
