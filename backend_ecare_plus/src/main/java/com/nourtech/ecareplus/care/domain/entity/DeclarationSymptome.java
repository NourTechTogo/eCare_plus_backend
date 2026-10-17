package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.care.domain.enumeration.StatutEvaluationSymptome;
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
@Table(name = "declaration_symptome", indexes = {
        @Index(name = "idx_declaration_patient", columnList = "patient_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeclarationSymptome extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, length = 150)
    private String symptome;

    @Column(nullable = false, length = 30)
    private String intensite;

    @Column(columnDefinition = "text")
    private String description;

    private Instant dateApparition;

    @Column(nullable = false)
    private Instant dateDeclaration;

    @Column(nullable = false)
    private boolean persistant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutEvaluationSymptome statutEvaluation;

    public DeclarationSymptome(
            Patient patient,
            String symptome,
            String intensite,
            String description,
            Instant dateApparition,
            boolean persistant
    ) {
        this.patient = Objects.requireNonNull(patient, "patient");
        this.symptome = obligatoire(symptome, "symptome");
        this.intensite = obligatoire(intensite, "intensite");
        this.description = normaliser(description);
        this.dateApparition = dateApparition;
        this.dateDeclaration = Instant.now();
        this.persistant = persistant;
        this.statutEvaluation = StatutEvaluationSymptome.NON_EVALUE;
    }

    public void modifierDeclaration(String symptome, String intensite, String description, boolean persistant) {
        verifierNonEvaluee();
        this.symptome = obligatoire(symptome, "symptome");
        this.intensite = obligatoire(intensite, "intensite");
        this.description = normaliser(description);
        this.persistant = persistant;
    }

    public void transmettreAuProfessionnel() {
        verifierNonEvaluee();
        this.statutEvaluation = StatutEvaluationSymptome.TRANSMIS;
    }

    public void marquerCommeEvalue() {
        if (statutEvaluation != StatutEvaluationSymptome.TRANSMIS) {
            throw new IllegalStateException("La déclaration doit avoir été transmise avant d'être évaluée");
        }
        this.statutEvaluation = StatutEvaluationSymptome.EVALUE;
    }

    private void verifierNonEvaluee() {
        if (statutEvaluation != StatutEvaluationSymptome.NON_EVALUE) {
            throw new IllegalStateException("La déclaration a déjà été transmise ou évaluée");
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
