package com.nourtech.ecareplus.identity.application.service;

import com.nourtech.ecareplus.identity.application.command.CreerAdministrateurStructureCommand;
import com.nourtech.ecareplus.identity.domain.entity.AdministrateurStructure;
import com.nourtech.ecareplus.identity.infrastructure.persistence.AdministrateurStructureRepository;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.organization.infrastructure.persistence.StructureSanteRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GestionAdministrateurStructureService {

    private final AdministrateurStructureRepository administrateurRepository;
    private final StructureSanteRepository structureRepository;
    private final VerificationUniciteCompteService uniciteCompteService;

    public GestionAdministrateurStructureService(
            AdministrateurStructureRepository administrateurRepository,
            StructureSanteRepository structureRepository,
            VerificationUniciteCompteService uniciteCompteService
    ) {
        this.administrateurRepository = administrateurRepository;
        this.structureRepository = structureRepository;
        this.uniciteCompteService = uniciteCompteService;
    }

    public AdministrateurStructure creer(CreerAdministrateurStructureCommand commande) {
        uniciteCompteService.verifier(commande.email(), commande.telephone());
        StructureSante structure = structureRepository.findById(commande.structureId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "StructureSante",
                        commande.structureId()
                ));
        return administrateurRepository.save(new AdministrateurStructure(
                commande.nom(),
                commande.prenoms(),
                commande.sexe(),
                commande.dateNaissance(),
                commande.telephone(),
                commande.email(),
                commande.motDePasseHash(),
                commande.languePreferee(),
                commande.fonctionAdministrative(),
                commande.niveauAcces(),
                structure
        ));
    }
}
