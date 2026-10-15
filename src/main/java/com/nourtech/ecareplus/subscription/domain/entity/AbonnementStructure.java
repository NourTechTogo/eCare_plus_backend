package com.nourtech.ecareplus.subscription.domain.entity;

import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import com.nourtech.ecareplus.subscription.domain.enumeration.FrequencePaiement;
import com.nourtech.ecareplus.subscription.domain.enumeration.StatutAbonnement;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "abonnement_structure", indexes = {
        @Index(name = "idx_abonnement_structure_statut", columnList = "structure_id,statut")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AbonnementStructure extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private StructureSante structure;

    @Column(nullable = false, length = 80)
    private String formule;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Column(nullable = false)
    private int nombrePatientsAutorises;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAbonnement statut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FrequencePaiement frequencePaiement;

    public AbonnementStructure(
            StructureSante structure,
            String formule,
            LocalDate dateDebut,
            LocalDate dateFin,
            int nombrePatientsAutorises,
            BigDecimal montant,
            FrequencePaiement frequencePaiement
    ) {
        this.structure = Objects.requireNonNull(structure, "structure");
        this.formule = obligatoire(formule, "formule");
        this.dateDebut = Objects.requireNonNull(dateDebut, "dateDebut");
        this.dateFin = Objects.requireNonNull(dateFin, "dateFin");
        this.nombrePatientsAutorises = positif(nombrePatientsAutorises, "nombrePatientsAutorises");
        this.montant = Objects.requireNonNull(montant, "montant");
        if (montant.signum() < 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être négatif");
        }
        this.frequencePaiement = Objects.requireNonNull(frequencePaiement, "frequencePaiement");
        verifierPeriode();
        this.statut = StatutAbonnement.EN_ATTENTE;
    }

    public void activer() {
        if (statut == StatutAbonnement.RESILIE) {
            throw new IllegalStateException("Un abonnement résilié ne peut pas être réactivé");
        }
        statut = StatutAbonnement.ACTIF;
    }

    public void renouveler(LocalDate nouvelleDateFin, int nouvelleLimite, BigDecimal nouveauMontant) {
        if (!nouvelleDateFin.isAfter(dateFin)) {
            throw new IllegalArgumentException("La nouvelle échéance doit être postérieure à l'échéance actuelle");
        }
        this.dateFin = nouvelleDateFin;
        this.nombrePatientsAutorises = positif(nouvelleLimite, "nouvelleLimite");
        this.montant = Objects.requireNonNull(nouveauMontant, "nouveauMontant");
        if (nouveauMontant.signum() < 0) {
            throw new IllegalArgumentException("Le montant ne peut pas être négatif");
        }
        this.statut = StatutAbonnement.ACTIF;
    }

    public void suspendre() {
        statut = StatutAbonnement.SUSPENDU;
    }

    public boolean autoriseNombrePatients(int nombrePatientsActifs) {
        return statut == StatutAbonnement.ACTIF && nombrePatientsActifs < nombrePatientsAutorises;
    }

    private void verifierPeriode() {
        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début");
        }
    }

    private static int positif(int valeur, String champ) {
        if (valeur <= 0) {
            throw new IllegalArgumentException(champ + " doit être strictement positif");
        }
        return valeur;
    }

    private static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }
}
