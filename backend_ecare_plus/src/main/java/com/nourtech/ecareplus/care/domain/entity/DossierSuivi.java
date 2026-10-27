package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.care.domain.enumeration.StatutDossier;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
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
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "dossier_suivi", indexes = {
        @Index(name = "idx_dossier_patient_structure", columnList = "patient_id,structure_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DossierSuivi extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "structure_id")
    private StructureSante structure;

    @Column(nullable = false)
    private LocalDate dateOuverture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDossier statut;

    @Column(columnDefinition = "text")
    private String resumeMedical;

    @Column(length = 100)
    private String frequenceSuivi;

    private LocalDate prochainControle;

    @Column(name = "date_derniere_mise_ajour", nullable = false)
    private Instant dateDerniereMiseAJour;

    public DossierSuivi(Patient patient, StructureSante structure, String frequenceSuivi) {
        this.patient = Objects.requireNonNull(patient, "patient");
        this.structure = Objects.requireNonNull(structure, "structure");
        this.frequenceSuivi = normaliser(frequenceSuivi);
        this.dateOuverture = LocalDate.now();
        this.dateDerniereMiseAJour = Instant.now();
        this.statut = StatutDossier.ACTIF;
    }

    public static DossierSuivi personnel(Patient patient) {
        DossierSuivi dossier = new DossierSuivi();
        dossier.patient = Objects.requireNonNull(patient, "patient");
        dossier.dateOuverture = LocalDate.now(java.time.ZoneOffset.UTC);
        dossier.dateDerniereMiseAJour = Instant.now();
        dossier.statut = StatutDossier.ACTIF;
        return dossier;
    }

    public void mettreAJour(String resumeMedical, String frequenceSuivi, LocalDate prochainControle) {
        verifierActif();
        this.resumeMedical = normaliser(resumeMedical);
        this.frequenceSuivi = normaliser(frequenceSuivi);
        this.prochainControle = prochainControle;
        this.dateDerniereMiseAJour = Instant.now();
    }

    public void suspendre() {
        verifierActif();
        this.statut = StatutDossier.SUSPENDU;
        this.dateDerniereMiseAJour = Instant.now();
    }

    public void fermer() {
        if (statut == StatutDossier.FERME) {
            throw new IllegalStateException("Le dossier est déjà fermé");
        }
        this.statut = StatutDossier.FERME;
        this.dateDerniereMiseAJour = Instant.now();
    }

    private void verifierActif() {
        if (statut != StatutDossier.ACTIF) {
            throw new IllegalStateException("Le dossier doit être actif");
        }
    }

    private static String normaliser(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
