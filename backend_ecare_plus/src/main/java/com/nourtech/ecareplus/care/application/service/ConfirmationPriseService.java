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
    private final NotificationPriseService rappels;
    private final jakarta.persistence.EntityManager em;

    public ConfirmationPriseService(
            ConfirmationPriseRepository confirmationRepository,
            PriseMedicamentRepository priseRepository,
            NotificationPriseService rappels,
            jakarta.persistence.EntityManager em
    ) {
        this.confirmationRepository = confirmationRepository;
        this.priseRepository = priseRepository;
        this.rappels = rappels;
        this.em = em;
    }

    public ConfirmationPrise programmer(ProgrammerConfirmationPriseCommand commande) {
        if(commande.datePrevue()==null)throw new IllegalArgumentException("Date de prise prévue obligatoire");
        Instant datePrevue=commande.datePrevue().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var prise = priseRepository.findById(commande.priseMedicamentId())
                .orElseThrow(() -> new RessourceIntrouvableException("PriseMedicament", commande.priseMedicamentId()));
        verrouiller(prise);
        return confirmationRepository
                .findByPriseMedicamentIdAndDateInitiale(commande.priseMedicamentId(), datePrevue)
                .orElseGet(() -> {
                    return confirmationRepository.save(new ConfirmationPrise(prise, datePrevue));
                });
    }

    public ConfirmationPrise confirmer(UUID confirmationId, String commentaire, Instant maintenant) {
        ConfirmationPrise confirmation = chargerPourReponse(confirmationId);
        confirmation.confirmer(commentaire, maintenant);
        rappels.invalider(confirmation);
        return confirmation;
    }

    public ConfirmationPrise signalerOubli(UUID confirmationId, String commentaire) {
        ConfirmationPrise confirmation = chargerPourReponse(confirmationId);
        confirmation.signalerOubli(commentaire);
        rappels.invalider(confirmation);
        return confirmation;
    }

    public ConfirmationPrise reporter(UUID confirmationId, Instant nouvelleDatePrevue) {
        ConfirmationPrise confirmation = chargerPourReponse(confirmationId);
        confirmation.reporter(nouvelleDatePrevue);
        rappels.invalider(confirmation);
        return confirmation;
    }

    public ConfirmationPrise consulter(UUID confirmationId) {
        return charger(confirmationId);
    }

    private void verrouiller(PriseMedicament prise) {
        em.find(com.nourtech.ecareplus.identity.domain.entity.Patient.class,
                prise.getTraitement().getDossier().getPatient().getId(),jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }
    private ConfirmationPrise chargerPourReponse(UUID id) {
        var c=charger(id);verrouiller(c.getPriseMedicament());em.refresh(c);return c;
    }
    private ConfirmationPrise charger(UUID confirmationId) {
        return confirmationRepository.findById(confirmationId)
                .orElseThrow(() -> new RessourceIntrouvableException("ConfirmationPrise", confirmationId));
    }
}
