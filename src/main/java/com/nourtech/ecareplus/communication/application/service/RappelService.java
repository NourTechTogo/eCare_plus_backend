package com.nourtech.ecareplus.communication.application.service;

import com.nourtech.ecareplus.care.domain.entity.PriseMedicament;
import com.nourtech.ecareplus.care.infrastructure.persistence.PriseMedicamentRepository;
import com.nourtech.ecareplus.communication.application.command.ProgrammerRappelCommand;
import com.nourtech.ecareplus.communication.domain.entity.Rappel;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.domain.enumeration.RecurrenceRappel;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutRappel;
import com.nourtech.ecareplus.communication.infrastructure.persistence.RappelRepository;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.infrastructure.persistence.PatientRepository;
import com.nourtech.ecareplus.shared.application.exception.RessourceIntrouvableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Service
@Transactional
public class RappelService {

    private final RappelRepository rappelRepository;
    private final PatientRepository patientRepository;
    private final PriseMedicamentRepository priseMedicamentRepository;

    public RappelService(
            RappelRepository rappelRepository,
            PatientRepository patientRepository,
            PriseMedicamentRepository priseMedicamentRepository
    ) {
        this.rappelRepository = rappelRepository;
        this.patientRepository = patientRepository;
        this.priseMedicamentRepository = priseMedicamentRepository;
    }

    public Rappel programmer(ProgrammerRappelCommand commande) {
        Patient patient = patientRepository.findById(commande.patientId())
                .orElseThrow(() -> new RessourceIntrouvableException("Patient", commande.patientId()));

        PriseMedicament priseMedicament = null;
        if (commande.priseMedicamentId() != null) {
            priseMedicament = priseMedicamentRepository.findById(commande.priseMedicamentId())
                    .orElseThrow(() -> new RessourceIntrouvableException("PriseMedicament", commande.priseMedicamentId()));
            if (!patient.memeIdentiteQue(priseMedicament.getTraitement().getDossier().getPatient())) {
                throw new IllegalArgumentException("La prise de médicament n'appartient pas au patient du rappel");
            }
        }

        return rappelRepository.save(new Rappel(
                patient,
                priseMedicament,
                commande.typeRappel(),
                commande.titre(),
                commande.message(),
                commande.dateHeurePrevue(),
                commande.frequence(),
                commande.canal(),
                commande.dateFin()
        ));
    }

    public Rappel modifier(
            UUID rappelId,
            String titre,
            String message,
            Instant dateHeurePrevue,
            RecurrenceRappel frequence,
            CanalNotification canal,
            Instant dateFin
    ) {
        Rappel rappel = charger(rappelId);
        rappel.modifier(titre, message, dateHeurePrevue, frequence, canal, dateFin);
        return rappel;
    }

    public Rappel envoyer(UUID rappelId) {
        Rappel rappel = charger(rappelId);
        rappel.envoyer(Instant.now());
        return rappel;
    }

    public Rappel marquerEnEchec(UUID rappelId) {
        Rappel rappel = charger(rappelId);
        rappel.marquerEnEchec();
        return rappel;
    }

    public Rappel reporter(UUID rappelId, Instant nouvelleDateHeurePrevue) {
        Rappel rappel = charger(rappelId);
        rappel.reporter(nouvelleDateHeurePrevue);
        return rappel;
    }

    public Rappel annuler(UUID rappelId) {
        Rappel rappel = charger(rappelId);
        rappel.annuler();
        return rappel;
    }

    public Rappel marquerCommeLu(UUID rappelId) {
        Rappel rappel = charger(rappelId);
        rappel.marquerCommeLu();
        return rappel;
    }

    public Rappel consulter(UUID rappelId) {
        return charger(rappelId);
    }

    @Transactional(readOnly = true)
    public List<Rappel> listerPourPatient(UUID patientId, StatutRappel statut) {
        if (!patientRepository.existsById(patientId)) {
            throw new RessourceIntrouvableException("Patient", patientId);
        }
        return rappelRepository.findAllByPatientIdAndStatutOrderByDateHeurePrevueAsc(patientId, statut);
    }

    @Transactional(readOnly = true)
    public List<Rappel> listerEcheances(Instant jusqua) {
        return rappelRepository.findAllByStatutAndDateHeurePrevueLessThanEqualOrderByDateHeurePrevueAsc(
                StatutRappel.PLANIFIE,
                jusqua
        );
    }

    private Rappel charger(UUID rappelId) {
        return rappelRepository.findById(rappelId)
                .orElseThrow(() -> new RessourceIntrouvableException("Rappel", rappelId));
    }
}
