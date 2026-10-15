package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.care.domain.enumeration.SourceMesure;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(name = "mesure_sante", indexes = {
        @Index(name = "idx_mesure_patient_date", columnList = "patient_id,date_mesure"),
        @Index(name = "idx_mesure_dossier", columnList = "dossier_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MesureSante extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierSuivi dossier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeMesure typeMesure;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal valeurPrincipale;

    @Column(precision = 10, scale = 3)
    private BigDecimal valeurSecondaire;

    @Column(nullable = false, length = 20)
    private String unite;

    @Column(nullable = false)
    private Instant dateMesure;

    @Column(length = 100)
    private String contexteMesure;

    @Column(columnDefinition = "text")
    private String commentaire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceMesure source;

    @Column(length = 30)
    private String statutSynchronisation;

    @Column(nullable = false)
    private boolean valeurConfirmee;

    public MesureSante(
            Patient patient,
            DossierSuivi dossier,
            TypeMesure typeMesure,
            BigDecimal valeurPrincipale,
            BigDecimal valeurSecondaire,
            String unite,
            Instant dateMesure,
            String contexteMesure,
            String commentaire,
            SourceMesure source,
            String statutSynchronisation
    ) {
        this.patient = Objects.requireNonNull(patient, "patient");
        this.dossier = Objects.requireNonNull(dossier, "dossier");
        this.typeMesure = Objects.requireNonNull(typeMesure, "typeMesure");
        this.valeurPrincipale = Objects.requireNonNull(valeurPrincipale, "valeurPrincipale");
        this.valeurSecondaire = valeurSecondaire;
        this.unite = obligatoire(unite, "unite");
        this.dateMesure = Objects.requireNonNull(dateMesure, "dateMesure");
        this.contexteMesure = normaliser(contexteMesure);
        this.commentaire = normaliser(commentaire);
        this.source = Objects.requireNonNull(source, "source");
        this.statutSynchronisation = normaliser(statutSynchronisation);
        verifierPlausibilite();
        this.valeurConfirmee = false;
    }

    public void modifier(
            BigDecimal valeurPrincipale,
            BigDecimal valeurSecondaire,
            String unite,
            String contexteMesure,
            String commentaire
    ) {
        if (valeurConfirmee) {
            throw new IllegalStateException("Une mesure confirmée ne peut plus être modifiée, il faut en saisir une nouvelle");
        }
        this.valeurPrincipale = Objects.requireNonNull(valeurPrincipale, "valeurPrincipale");
        this.valeurSecondaire = valeurSecondaire;
        this.unite = obligatoire(unite, "unite");
        this.contexteMesure = normaliser(contexteMesure);
        this.commentaire = normaliser(commentaire);
        verifierPlausibilite();
    }

    public void confirmer() {
        if (valeurConfirmee) {
            throw new IllegalStateException("La mesure est déjà confirmée");
        }
        this.valeurConfirmee = true;
    }

    private void verifierPlausibilite() {
        switch (typeMesure) {
            case TENSION_ARTERIELLE -> {
                verifierPlage("valeurPrincipale (systolique)", valeurPrincipale, 40, 260);
                if (valeurSecondaire == null) {
                    throw new IllegalArgumentException("La valeur diastolique est obligatoire pour une tension artérielle");
                }
                verifierPlage("valeurSecondaire (diastolique)", valeurSecondaire, 20, 200);
                if (valeurPrincipale.compareTo(valeurSecondaire) <= 0) {
                    throw new IllegalArgumentException("La systolique doit être supérieure à la diastolique");
                }
            }
            case GLYCEMIE -> {
                verifierPlage("valeurPrincipale", valeurPrincipale, 10, 700);
                verifierAbsente(valeurSecondaire);
            }
            case POIDS -> {
                verifierPlage("valeurPrincipale", valeurPrincipale, 1, 400);
                verifierAbsente(valeurSecondaire);
            }
            case FREQUENCE_CARDIAQUE -> {
                verifierPlage("valeurPrincipale", valeurPrincipale, 20, 250);
                verifierAbsente(valeurSecondaire);
            }
        }
    }

    private static void verifierPlage(String champ, BigDecimal valeur, int min, int max) {
        if (valeur.compareTo(BigDecimal.valueOf(min)) < 0 || valeur.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw new IllegalArgumentException(champ + " doit être compris entre " + min + " et " + max);
        }
    }

    private static void verifierAbsente(BigDecimal valeurSecondaire) {
        if (valeurSecondaire != null) {
            throw new IllegalArgumentException("Une valeur secondaire n'est pas attendue pour ce type de mesure");
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
