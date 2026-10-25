package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.care.domain.enumeration.StatutTraitement;
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

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "traitement", indexes = {
        @Index(name = "idx_traitement_dossier", columnList = "dossier_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Traitement extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierSuivi dossier;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private LocalDate dateDebut;

    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutTraitement statut;

    @Column(length = 200)
    private String prescripteur;

    @Column(columnDefinition = "text")
    private String consignesGenerales;

    public Traitement(
            DossierSuivi dossier,
            String libelle,
            String description,
            LocalDate dateDebut,
            LocalDate dateFin,
            String prescripteur,
            String consignesGenerales
    ) {
        this.dossier = Objects.requireNonNull(dossier, "dossier");
        this.libelle = obligatoire(libelle, "libelle");
        this.description = normaliser(description);
        this.dateDebut = Objects.requireNonNull(dateDebut, "dateDebut");
        this.dateFin = dateFin;
        verifierDates();
        this.prescripteur = normaliser(prescripteur);
        this.consignesGenerales = normaliser(consignesGenerales);
        this.statut = StatutTraitement.ACTIF;
    }

    public void modifier(String libelle, String description, LocalDate dateFin, String consignesGenerales) {
        verifierActif();
        this.libelle = obligatoire(libelle, "libelle");
        this.description = normaliser(description);
        this.dateFin = dateFin;
        verifierDates();
        this.consignesGenerales = normaliser(consignesGenerales);
    }

    public void suspendre() {
        verifierActif();
        this.statut = StatutTraitement.SUSPENDU;
    }

    public void terminer() {
        if (statut == StatutTraitement.TERMINE) {
            throw new IllegalStateException("Le traitement est déjà terminé");
        }
        this.statut = StatutTraitement.TERMINE;
        if (dateFin == null && !dateDebut.isAfter(LocalDate.now(java.time.ZoneOffset.UTC))) {
            this.dateFin = LocalDate.now(java.time.ZoneOffset.UTC);
        }
    }

    private void verifierActif() {
        if (statut != StatutTraitement.ACTIF) {
            throw new IllegalStateException("Le traitement doit être actif");
        }
    }

    private void verifierDates() {
        if (dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début");
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
