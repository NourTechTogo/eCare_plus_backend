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
import com.nourtech.ecareplus.care.domain.value.ValeurMesure;

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

    @Column(length = 100)
    private String sourceRef;

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
        appliquerValeur(ValeurMesure.normaliser(typeMesure, valeurPrincipale, valeurSecondaire, unite));
        // PostgreSQL persists timestamps at microsecond precision. Normalize before saving so
        // a retry with the original nanosecond timestamp compares to the exact same instant.
        this.dateMesure = Objects.requireNonNull(dateMesure, "dateMesure")
                .truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if (dateMesure.isAfter(Instant.now())) throw new IllegalArgumentException("dateMesure ne peut pas être dans le futur");
        this.contexteMesure = normaliser(contexteMesure);
        this.commentaire = normaliser(commentaire);
        this.source = Objects.requireNonNull(source, "source");
        this.statutSynchronisation = normaliser(statutSynchronisation);
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
        // Validate everything before mutating a managed entity.
        ValeurMesure normalisee = ValeurMesure.normaliser(typeMesure, valeurPrincipale, valeurSecondaire, unite);
        appliquerValeur(normalisee);
        this.contexteMesure = normaliser(contexteMesure);
        this.commentaire = normaliser(commentaire);
    }

    public void definirSourceRef(String reference) {
        String normalisee = normaliser(reference);
        if (normalisee != null && normalisee.length() > 100)
            throw new IllegalArgumentException("sourceRef doit contenir au plus 100 caractères");
        if (sourceRef != null && !sourceRef.equals(normalisee))
            throw new IllegalStateException("La référence source d'une mesure est immuable");
        this.sourceRef = normalisee;
    }

    private void appliquerValeur(ValeurMesure valeur) {
        this.valeurPrincipale = valeur.principale();
        this.valeurSecondaire = valeur.secondaire();
        this.unite = valeur.unite();
    }

    public void confirmer() {
        if (valeurConfirmee) {
            throw new IllegalStateException("La mesure est déjà confirmée");
        }
        this.valeurConfirmee = true;
    }

    private static String normaliser(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
