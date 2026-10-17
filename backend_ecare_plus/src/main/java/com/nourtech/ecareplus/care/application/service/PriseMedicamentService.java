package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.application.command.ProgrammerPriseCommand;
import com.nourtech.ecareplus.care.domain.entity.Medicament;
import com.nourtech.ecareplus.care.domain.entity.PriseMedicament;
import com.nourtech.ecareplus.care.domain.entity.Traitement;
import com.nourtech.ecareplus.care.infrastructure.persistence.MedicamentRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.PriseMedicamentRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.TraitementRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.UUID;

@Service
@Transactional
public class PriseMedicamentService {

    private final PriseMedicamentRepository priseRepository;
    private final TraitementRepository traitementRepository;
    private final MedicamentRepository medicamentRepository;

    public PriseMedicamentService(
            PriseMedicamentRepository priseRepository,
            TraitementRepository traitementRepository,
            MedicamentRepository medicamentRepository
    ) {
        this.priseRepository = priseRepository;
        this.traitementRepository = traitementRepository;
        this.medicamentRepository = medicamentRepository;
    }

    public PriseMedicament programmer(ProgrammerPriseCommand commande) {
        Traitement traitement = traitementRepository.findById(commande.traitementId())
                .orElseThrow(() -> new RessourceIntrouvableException("Traitement", commande.traitementId()));
        Medicament medicament = medicamentRepository.findById(commande.medicamentId())
                .orElseThrow(() -> new RessourceIntrouvableException("Medicament", commande.medicamentId()));

        return priseRepository.save(new PriseMedicament(
                traitement,
                medicament,
                commande.quantite(),
                commande.unite(),
                commande.frequence(),
                commande.heurePrevue(),
                commande.momentPrise(),
                commande.voieAdministration(),
                commande.consigne()
        ));
    }

    public PriseMedicament modifierHoraire(UUID priseId, LocalTime heurePrevue, String frequence, String momentPrise) {
        PriseMedicament prise = charger(priseId);
        prise.modifierHoraire(heurePrevue, frequence, momentPrise);
        return prise;
    }

    public PriseMedicament activer(UUID priseId) {
        PriseMedicament prise = charger(priseId);
        prise.activer();
        return prise;
    }

    public PriseMedicament desactiver(UUID priseId) {
        PriseMedicament prise = charger(priseId);
        prise.desactiver();
        return prise;
    }

    public PriseMedicament consulter(UUID priseId) {
        return charger(priseId);
    }

    private PriseMedicament charger(UUID priseId) {
        return priseRepository.findById(priseId)
                .orElseThrow(() -> new RessourceIntrouvableException("PriseMedicament", priseId));
    }
}
