package com.nourtech.ecareplus.subscription.application.service;

import com.nourtech.ecareplus.subscription.application.command.CreerAbonnementCommand;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.organization.infrastructure.persistence.StructureSanteRepository;
import com.nourtech.ecareplus.shared.application.exception.ConflitMetierException;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import com.nourtech.ecareplus.subscription.domain.entity.AbonnementStructure;
import com.nourtech.ecareplus.subscription.domain.enumeration.FrequencePaiement;
import com.nourtech.ecareplus.subscription.domain.enumeration.StatutAbonnement;
import com.nourtech.ecareplus.subscription.infrastructure.persistence.AbonnementStructureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class GestionAbonnementService {

    private final AbonnementStructureRepository abonnementRepository;
    private final StructureSanteRepository structureRepository;

    public GestionAbonnementService(
            AbonnementStructureRepository abonnementRepository,
            StructureSanteRepository structureRepository
    ) {
        this.abonnementRepository = abonnementRepository;
        this.structureRepository = structureRepository;
    }

    public AbonnementStructure creer(
            UUID structureId,
            String formule,
            LocalDate dateDebut,
            LocalDate dateFin,
            int nombrePatientsAutorises,
            BigDecimal montant,
            FrequencePaiement frequencePaiement
    ) {
        StructureSante structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new RessourceIntrouvableException("StructureSante", structureId));
        return abonnementRepository.save(new AbonnementStructure(
                structure,
                formule,
                dateDebut,
                dateFin,
                nombrePatientsAutorises,
                montant,
                frequencePaiement
        ));
    }

    public AbonnementStructure creer(CreerAbonnementCommand command) {
        return creer(
                command.structureId(), command.formule(), command.dateDebut(), command.dateFin(),
                command.nombrePatientsAutorises(), command.montant(), command.frequencePaiement()
        );
    }

    public AbonnementStructure activer(UUID abonnementId) {
        AbonnementStructure abonnement = abonnementRepository.findById(abonnementId)
                .orElseThrow(() -> new RessourceIntrouvableException("AbonnementStructure", abonnementId));

        boolean chevauchement = abonnementRepository.findAllByStructureIdAndStatut(
                        abonnement.getStructure().getId(),
                        StatutAbonnement.ACTIF
                ).stream()
                .filter(existant -> !existant.memeIdentiteQue(abonnement))
                .anyMatch(existant -> periodesSeChevauchent(existant, abonnement));

        if (chevauchement) {
            throw new ConflitMetierException("Un abonnement actif chevauche déjà cette période");
        }
        abonnement.activer();
        return abonnement;
    }

    private static boolean periodesSeChevauchent(
            AbonnementStructure premier,
            AbonnementStructure second
    ) {
        return !premier.getDateFin().isBefore(second.getDateDebut())
                && !second.getDateFin().isBefore(premier.getDateDebut());
    }
}
