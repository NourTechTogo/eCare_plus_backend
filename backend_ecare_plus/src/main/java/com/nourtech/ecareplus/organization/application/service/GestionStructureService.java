package com.nourtech.ecareplus.organization.application.service;

import com.nourtech.ecareplus.organization.application.command.CreerStructureSanteCommand;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.organization.infrastructure.persistence.StructureSanteRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class GestionStructureService {

    private final StructureSanteRepository structureRepository;

    public GestionStructureService(StructureSanteRepository structureRepository) {
        this.structureRepository = structureRepository;
    }

    public StructureSante creer(CreerStructureSanteCommand commande) {
        if (structureRepository.existsByNumeroAutorisation(commande.numeroAutorisation())) {
            throw new ConflitMetierException("Ce numéro d'autorisation de structure existe déjà");
        }
        return structureRepository.save(new StructureSante(
                commande.nom(),
                commande.typeStructure(),
                commande.adresse(),
                commande.ville(),
                commande.region(),
                commande.telephone(),
                commande.email(),
                commande.numeroAutorisation()
        ));
    }

    public StructureSante activer(UUID structureId) {
        StructureSante structure = charger(structureId);
        structure.activer();
        return structure;
    }

    public StructureSante suspendre(UUID structureId) {
        StructureSante structure = charger(structureId);
        structure.suspendreService();
        return structure;
    }

    private StructureSante charger(UUID structureId) {
        return structureRepository.findById(structureId)
                .orElseThrow(() -> new RessourceIntrouvableException("StructureSante", structureId));
    }
}
