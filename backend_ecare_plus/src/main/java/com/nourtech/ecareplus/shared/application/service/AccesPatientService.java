package com.nourtech.ecareplus.shared.application.service;

import com.nourtech.ecareplus.portail.application.PortailService;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service @Transactional(readOnly=true)
public class AccesPatientService {
    private final PortailService portail;
    public AccesPatientService(PortailService portail){this.portail=portail;}
    public Patient verifier(UUID acteurId, UUID patientId){
        if(!portail.acteur().getId().equals(acteurId))throw new AccessDeniedException("Acteur incompatible");
        return acteurId.equals(patientId)?portail.soi(patientId,true):portail.autoriser(patientId,true);
    }
    public UUID acteurId(){return portail.acteur().getId();}
    public UUID structureClinique(){var u=portail.acteur();return u instanceof com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante p?p.getStructure().getId():null;}
    public void verifierDossier(UUID acteur,com.nourtech.ecareplus.care.domain.entity.DossierSuivi dossier){verifier(acteur,dossier.getPatient().getId());UUID structure=structureClinique();if(structure!=null&&!structure.equals(dossier.getStructure().getId()))throw new AccessDeniedException("Dossier d'une autre structure");}
}
