package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.care.application.command.ProgrammerConfirmationPriseCommand;
import com.nourtech.ecareplus.care.domain.entity.ConfirmationPrise;
import com.nourtech.ecareplus.care.domain.entity.PriseMedicament;
import com.nourtech.ecareplus.care.infrastructure.persistence.ConfirmationPriseRepository;
import com.nourtech.ecareplus.care.infrastructure.persistence.PriseMedicamentRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class ConfirmationPriseService {

    private final ConfirmationPriseRepository confirmationRepository;
    private final PriseMedicamentRepository priseRepository;

    public ConfirmationPriseService(
            ConfirmationPriseRepository confirmationRepository,
            PriseMedicamentRepository priseRepository
    ) {
        this.confirmationRepository = confirmationRepository;
        this.priseRepository = priseRepository;
    }

    public ConfirmationPrise programmer(ProgrammerConfirmationPriseCommand commande) {
        return confirmationRepository
                .findByPriseMedicamentIdAndDatePrevue(commande.priseMedicamentId(), commande.datePrevue())
                .orElseGet(() -> {
                    PriseMedicament prise = priseRepository.findById(commande.priseMedicamentId())
                            .orElseThrow(() -> new RessourceIntrouvableException("PriseMedicament", commande.priseMedicamentId()));
                    return confirmationRepository.save(new ConfirmationPrise(prise, commande.datePrevue()));
                });
    }

    public ConfirmationPrise confirmer(UUID confirmationId, String commentaire, Instant maintenant) {
        ConfirmationPrise confirmation = charger(confirmationId);
        confirmation.confirmer(commentaire, maintenant);
        return confirmation;
    }

    public ConfirmationPrise signalerOubli(UUID confirmationId, String commentaire) {
        ConfirmationPrise confirmation = charger(confirmationId);
        confirmation.signalerOubli(commentaire);
        return confirmation;
    }

    public ConfirmationPrise reporter(UUID confirmationId, Instant nouvelleDatePrevue) {
        ConfirmationPrise confirmation = charger(confirmationId);
        confirmation.reporter(nouvelleDatePrevue);
        return confirmation;
    }

    public ConfirmationPrise consulter(UUID confirmationId) {
        return charger(confirmationId);
    }

    private ConfirmationPrise charger(UUID confirmationId) {
        return confirmationRepository.findById(confirmationId)
                .orElseThrow(() -> new RessourceIntrouvableException("ConfirmationPrise", confirmationId));
    }
}
