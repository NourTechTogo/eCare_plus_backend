package com.nourtech.ecareplus.identity.application.service;

import com.nourtech.ecareplus.identity.application.command.CreerInfirmierCommand;
import com.nourtech.ecareplus.identity.application.command.CreerMedecinCommand;
import com.nourtech.ecareplus.identity.domain.entity.Infirmier;
import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import com.nourtech.ecareplus.identity.infrastructure.persistence.MedecinRepository;
import com.nourtech.ecareplus.identity.infrastructure.persistence.InfirmierRepository;
import com.nourtech.ecareplus.identity.infrastructure.persistence.ProfessionnelSanteRepository;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.organization.infrastructure.persistence.StructureSanteRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GestionProfessionnelSanteService {

    private final ProfessionnelSanteRepository professionnelRepository;
    private final MedecinRepository medecinRepository;
    private final InfirmierRepository infirmierRepository;
    private final StructureSanteRepository structureRepository;
    private final VerificationUniciteCompteService uniciteCompteService;

    public GestionProfessionnelSanteService(
            ProfessionnelSanteRepository professionnelRepository,
            MedecinRepository medecinRepository,
            InfirmierRepository infirmierRepository,
            StructureSanteRepository structureRepository,
            VerificationUniciteCompteService uniciteCompteService
    ) {
        this.professionnelRepository = professionnelRepository;
        this.medecinRepository = medecinRepository;
        this.infirmierRepository = infirmierRepository;
        this.structureRepository = structureRepository;
        this.uniciteCompteService = uniciteCompteService;
    }

    public Medecin creerMedecin(CreerMedecinCommand commande) {
        verifierNumeroProfessionnel(commande.numeroProfessionnel());
        if (medecinRepository.existsByNumeroOrdre(commande.numeroOrdre())) {
            throw new ConflitMetierException("Ce numéro d'ordre existe déjà");
        }
        uniciteCompteService.verifier(commande.email(), commande.telephone());
        StructureSante structure = chargerStructure(commande.structureId());
        return medecinRepository.save(new Medecin(
                commande.numeroProfessionnel(),
                commande.numeroOrdre(),
                commande.nom(),
                commande.prenoms(),
                commande.sexe(),
                commande.dateNaissance(),
                commande.telephone(),
                commande.email(),
                commande.motDePasseHash(),
                commande.languePreferee(),
                commande.specialite(),
                commande.domaineSpecialisation(),
                commande.datePriseFonction(),
                structure
        ));
    }

    public Infirmier creerInfirmier(CreerInfirmierCommand commande) {
        verifierNumeroProfessionnel(commande.numeroProfessionnel());
        uniciteCompteService.verifier(commande.email(), commande.telephone());
        StructureSante structure = chargerStructure(commande.structureId());
        return infirmierRepository.save(new Infirmier(
                commande.numeroProfessionnel(),
                commande.nom(),
                commande.prenoms(),
                commande.sexe(),
                commande.dateNaissance(),
                commande.telephone(),
                commande.email(),
                commande.motDePasseHash(),
                commande.languePreferee(),
                commande.specialite(),
                commande.service(),
                commande.niveauResponsabilite(),
                commande.datePriseFonction(),
                structure
        ));
    }

    private void verifierNumeroProfessionnel(String numeroProfessionnel) {
        if (professionnelRepository.existsByNumeroProfessionnel(numeroProfessionnel)) {
            throw new ConflitMetierException("Ce numéro professionnel existe déjà");
        }
    }

    private StructureSante chargerStructure(java.util.UUID structureId) {
        return structureRepository.findById(structureId)
                .orElseThrow(() -> new RessourceIntrouvableException("StructureSante", structureId));
    }
}
