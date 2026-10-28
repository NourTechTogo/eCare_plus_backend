package com.nourtech.ecareplus.communication.domain.entity;

import com.nourtech.ecareplus.care.domain.entity.PriseMedicament;
import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.domain.enumeration.RecurrenceRappel;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutRappel;
import com.nourtech.ecareplus.communication.domain.enumeration.TypeRappel;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
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
@Table(name = "rappel", indexes = {
        @Index(name = "idx_rappel_patient", columnList = "patient_id"),
        @Index(name = "idx_rappel_prise_medicament", columnList = "prise_medicament_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rappel extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prise_medicament_id")
    private PriseMedicament priseMedicament;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeRappel typeRappel;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private Instant dateHeurePrevue;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequence", nullable = false, length = 20)
    private RecurrenceRappel frequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutRappel statut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotification canal;

    private Instant dateFin;

    private Instant derniereExecution;

    /** Read status belongs to the latest occurrence, not the recurring schedule. */
    private Instant derniereLecture;

    private java.util.UUID agendaPersonnelId;
    public void rattacherAgendaPersonnel(java.util.UUID rendezVousId) {
        if(agendaPersonnelId!=null)throw new IllegalStateException("Origine du rappel déjà définie");
        if(typeRappel!=TypeRappel.RENDEZ_VOUS)throw new IllegalStateException("Type de rappel incompatible");
        agendaPersonnelId=Objects.requireNonNull(rendezVousId);
    }

    public Rappel(
            Patient patient,
            PriseMedicament priseMedicament,
            TypeRappel typeRappel,
            String titre,
            String message,
            Instant dateHeurePrevue,
            RecurrenceRappel frequence,
            CanalNotification canal,
            Instant dateFin
    ) {
        this.patient = Objects.requireNonNull(patient, "patient");
        this.priseMedicament = priseMedicament;
        this.typeRappel = Objects.requireNonNull(typeRappel, "typeRappel");
        this.titre = obligatoire(titre, "titre");
        this.message = obligatoire(message, "message");
        this.dateHeurePrevue = Objects.requireNonNull(dateHeurePrevue, "dateHeurePrevue");
        this.frequence = Objects.requireNonNullElse(frequence, RecurrenceRappel.AUCUNE);
        this.canal = Objects.requireNonNull(canal, "canal");
        this.dateFin = dateFin;
        verifierDates();
        this.statut = StatutRappel.PLANIFIE;
    }

    public void modifier(
            String titre,
            String message,
            Instant dateHeurePrevue,
            RecurrenceRappel frequence,
            CanalNotification canal,
            Instant dateFin
    ) {
        verifierPlanifie();
        this.titre = obligatoire(titre, "titre");
        this.message = obligatoire(message, "message");
        this.dateHeurePrevue = Objects.requireNonNull(dateHeurePrevue, "dateHeurePrevue");
        this.frequence = Objects.requireNonNullElse(frequence, RecurrenceRappel.AUCUNE);
        this.canal = Objects.requireNonNull(canal, "canal");
        this.dateFin = dateFin;
        verifierDates();
    }

    public void envoyer() {
        envoyer(Instant.now());
    }

    public void envoyer(Instant maintenant) {
        verifierPlanifie();
        this.derniereExecution = Objects.requireNonNull(maintenant, "maintenant");
        this.derniereLecture = null;
        Instant prochaine = frequence.prochaineOccurrence(dateHeurePrevue);
        if (prochaine == null) {
            this.statut = StatutRappel.ENVOYE;
        } else if (dateFin != null && prochaine.isAfter(dateFin)) {
            this.statut = StatutRappel.TERMINE;
        } else {
            this.dateHeurePrevue = prochaine;
        }
    }

    public void marquerEnEchec() {
        verifierPlanifie();
        this.statut = StatutRappel.ECHEC;
    }

    public void reporter(Instant nouvelleDateHeurePrevue) {
        verifierPlanifie();
        this.dateHeurePrevue = Objects.requireNonNull(nouvelleDateHeurePrevue, "nouvelleDateHeurePrevue");
        verifierDates();
    }

    public void annuler() {
        if (statut == StatutRappel.LU) {
            throw new IllegalStateException("Un rappel déjà lu ne peut plus être annulé");
        }
        this.statut = StatutRappel.ANNULE;
    }

    public void marquerCommeLu() {
        if (derniereExecution == null || derniereLecture != null
                || statut == StatutRappel.ANNULE || statut == StatutRappel.ECHEC) {
            throw new IllegalStateException("Seul un rappel envoyé peut être marqué comme lu");
        }
        this.derniereLecture = Instant.now();
        if (frequence == RecurrenceRappel.AUCUNE) this.statut = StatutRappel.LU;
    }

    private void verifierPlanifie() {
        if (statut != StatutRappel.PLANIFIE) {
            throw new IllegalStateException("Le rappel doit être planifié");
        }
    }

    private void verifierDates() {
        if (dateFin != null && dateFin.isBefore(dateHeurePrevue)) {
            throw new IllegalArgumentException("dateFin doit être postérieure ou égale à dateHeurePrevue");
        }
    }

    private static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }

    private static String normaliser(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
