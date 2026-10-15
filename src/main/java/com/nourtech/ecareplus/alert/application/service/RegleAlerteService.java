package com.nourtech.ecareplus.alert.application.service;

import com.nourtech.ecareplus.alert.application.command.CreerRegleAlerteCommand;
import com.nourtech.ecareplus.alert.domain.entity.RegleAlerte;
import com.nourtech.ecareplus.alert.infrastructure.persistence.RegleAlerteRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class RegleAlerteService {

    private final RegleAlerteRepository regleAlerteRepository;

    public RegleAlerteService(RegleAlerteRepository regleAlerteRepository) {
        this.regleAlerteRepository = regleAlerteRepository;
    }

    public RegleAlerte creer(CreerRegleAlerteCommand commande) {
        return regleAlerteRepository.save(new RegleAlerte(
                commande.nom(),
                commande.description(),
                commande.typeMesure(),
                commande.operateur(),
                commande.seuilMin(),
                commande.seuilMax(),
                commande.nombreOccurrences(),
                commande.periodeObservation(),
                commande.niveauPriorite()
        ));
    }

    public RegleAlerte modifier(
            UUID regleId,
            String description,
            String operateur,
            BigDecimal seuilMin,
            BigDecimal seuilMax,
            Integer nombreOccurrences,
            Integer periodeObservation
    ) {
        RegleAlerte regle = charger(regleId);
        regle.modifierRegle(description, operateur, seuilMin, seuilMax, nombreOccurrences, periodeObservation);
        return regle;
    }

    public RegleAlerte valider(UUID regleId, Instant maintenant) {
        RegleAlerte regle = charger(regleId);
        regle.validerRegle(maintenant);
        return regle;
    }

    public RegleAlerte activer(UUID regleId) {
        RegleAlerte regle = charger(regleId);
        regle.activerRegle();
        return regle;
    }

    public RegleAlerte desactiver(UUID regleId) {
        RegleAlerte regle = charger(regleId);
        regle.desactiverRegle();
        return regle;
    }

    public RegleAlerte consulter(UUID regleId) {
        return charger(regleId);
    }

    private RegleAlerte charger(UUID regleId) {
        return regleAlerteRepository.findById(regleId)
                .orElseThrow(() -> new RessourceIntrouvableException("RegleAlerte", regleId));
    }
}
