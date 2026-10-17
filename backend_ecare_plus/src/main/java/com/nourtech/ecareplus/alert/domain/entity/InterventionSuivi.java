package com.nourtech.ecareplus.alert.domain.entity;

import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "intervention_suivi", indexes = {
        @Index(name = "idx_intervention_alerte", columnList = "alerte_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterventionSuivi extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alerte_id", nullable = false)
    private Alerte alerte;

    @Column(nullable = false, length = 100)
    private String typeIntervention;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Instant dateIntervention;

    @Column(columnDefinition = "text")
    private String resultat;

    @Column(columnDefinition = "text")
    private String actionSuivante;

    private Instant dateProchaineAction;

    public InterventionSuivi(Alerte alerte, String typeIntervention, String description, Instant dateIntervention) {
        this.alerte = Objects.requireNonNull(alerte, "alerte");
        this.typeIntervention = obligatoire(typeIntervention, "typeIntervention");
        this.description = normaliser(description);
        this.dateIntervention = Objects.requireNonNull(dateIntervention, "dateIntervention");
    }

    public void modifierIntervention(String description, String resultat) {
        this.description = normaliser(description);
        this.resultat = normaliser(resultat);
    }

    public void definirActionSuivante(String actionSuivante, Instant dateProchaineAction) {
        this.actionSuivante = normaliser(actionSuivante);
        this.dateProchaineAction = dateProchaineAction;
    }

    public void cloturerIntervention(String resultatFinal) {
        this.resultat = obligatoire(resultatFinal, "resultatFinal");
        this.actionSuivante = null;
        this.dateProchaineAction = null;
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
