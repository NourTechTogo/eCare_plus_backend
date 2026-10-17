package com.nourtech.ecareplus.communication.domain.entity;

import com.nourtech.ecareplus.communication.domain.enumeration.StatutRendezVous;
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

/**
 * Le lien avec un professionnel ou une structure n'est pas encore validé dans le diagramme
 * (relation marquée "à confirmer" dans la répartition) : seul le patient est rattaché pour l'instant.
 */
@Getter
@Entity
@Table(name = "rendez_vous", indexes = {
        @Index(name = "idx_rendez_vous_patient", columnList = "patient_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RendezVous extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, length = 200)
    private String motif;

    @Column(nullable = false)
    private Instant dateHeure;

    @Column(length = 300)
    private String lieu;

    @Column(nullable = false, length = 60)
    private String typeRendezVous;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutRendezVous statut;

    @Column(columnDefinition = "text")
    private String commentaire;

    public RendezVous(Patient patient, String motif, Instant dateHeure, String lieu, String typeRendezVous, String commentaire) {
        this.patient = Objects.requireNonNull(patient, "patient");
        this.motif = obligatoire(motif, "motif");
        this.dateHeure = Objects.requireNonNull(dateHeure, "dateHeure");
        this.lieu = normaliser(lieu);
        this.typeRendezVous = obligatoire(typeRendezVous, "typeRendezVous");
        this.commentaire = normaliser(commentaire);
        this.statut = StatutRendezVous.PLANIFIE;
    }

    public void confirmer() {
        verifierModifiable();
        this.statut = StatutRendezVous.CONFIRME;
    }

    public void reporter(Instant nouvelleDateHeure) {
        verifierModifiable();
        this.dateHeure = Objects.requireNonNull(nouvelleDateHeure, "nouvelleDateHeure");
        this.statut = StatutRendezVous.REPORTE;
    }

    public void annuler() {
        verifierModifiable();
        this.statut = StatutRendezVous.ANNULE;
    }

    public void marquerCommeRealise() {
        if (statut != StatutRendezVous.CONFIRME) {
            throw new IllegalStateException("Seul un rendez-vous confirmé peut être marqué comme réalisé");
        }
        this.statut = StatutRendezVous.REALISE;
    }

    private void verifierModifiable() {
        if (statut == StatutRendezVous.REALISE || statut == StatutRendezVous.ANNULE) {
            throw new IllegalStateException("Le rendez-vous ne peut plus être modifié");
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
