package com.nourtech.ecareplus.consent.domain.entity;

import com.nourtech.ecareplus.consent.domain.enumeration.StatutAutorisation;
import com.nourtech.ecareplus.consent.domain.enumeration.TypeDonneePartage;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProcheAidant;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(name = "autorisation_partage", indexes = {
        @Index(name = "idx_autorisation_patient_statut", columnList = "patient_id,statut"),
        @Index(name = "idx_autorisation_proche_statut", columnList = "proche_aidant_id,statut")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutorisationPartage extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proche_aidant_id", nullable = false)
    private ProcheAidant procheAidant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consentement_id", nullable = false)
    private Consentement consentement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeDonneePartage typeDonnee;

    @Column(nullable = false)
    private Instant dateDebut;

    private Instant dateFin;

    @Column(nullable = false)
    private boolean peutConsulter;

    @Column(nullable = false)
    private boolean peutRecevoirNotification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAutorisation statut;

    public AutorisationPartage(
            Patient patient,
            ProcheAidant procheAidant,
            Consentement consentement,
            TypeDonneePartage typeDonnee,
            Instant dateDebut,
            Instant dateFin,
            boolean peutConsulter,
            boolean peutRecevoirNotification
    ) {
        this.patient = Objects.requireNonNull(patient, "patient");
        this.procheAidant = Objects.requireNonNull(procheAidant, "procheAidant");
        this.consentement = Objects.requireNonNull(consentement, "consentement");
        if (!consentement.getPatient().memeIdentiteQue(patient)) {
            throw new IllegalArgumentException("Le consentement doit appartenir au patient");
        }
        this.typeDonnee = Objects.requireNonNull(typeDonnee, "typeDonnee");
        this.dateDebut = Objects.requireNonNull(dateDebut, "dateDebut");
        this.dateFin = dateFin;
        verifierPeriode();
        this.peutConsulter = peutConsulter;
        this.peutRecevoirNotification = peutRecevoirNotification;
        this.statut = StatutAutorisation.EN_ATTENTE;
    }

    public void activer(Instant instant) {
        if (!consentement.estValideA(instant)) {
            throw new IllegalStateException("Le consentement lié n'est pas valide");
        }
        this.statut = StatutAutorisation.ACTIVE;
    }

    public void modifierDroits(boolean peutConsulter, boolean peutRecevoirNotification, Instant dateFin) {
        verifierModifiable();
        this.peutConsulter = peutConsulter;
        this.peutRecevoirNotification = peutRecevoirNotification;
        this.dateFin = dateFin;
        verifierPeriode();
    }

    public void retirer() {
        verifierModifiable();
        this.statut = StatutAutorisation.RETIREE;
    }

    public boolean estValideA(Instant instant) {
        return statut == StatutAutorisation.ACTIVE
                && !dateDebut.isAfter(instant)
                && (dateFin == null || dateFin.isAfter(instant))
                && consentement.estValideA(instant);
    }

    private void verifierModifiable() {
        if (statut == StatutAutorisation.RETIREE || statut == StatutAutorisation.EXPIREE) {
            throw new IllegalStateException("L'autorisation n'est plus modifiable");
        }
    }

    private void verifierPeriode() {
        if (dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La fin de l'autorisation ne peut pas précéder son début");
        }
    }
}
