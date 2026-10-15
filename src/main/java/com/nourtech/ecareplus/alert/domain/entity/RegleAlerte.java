package com.nourtech.ecareplus.alert.domain.entity;

import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

@Getter
@Entity
@Table(name = "regle_alerte")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegleAlerte extends AbstractEntity {

    private static final Set<String> OPERATEURS_SIMPLES = Set.of(">", ">=", "<", "<=");

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeMesure typeMesure;

    @Column(nullable = false, length = 20)
    private String operateur;

    @Column(precision = 10, scale = 3)
    private BigDecimal seuilMin;

    @Column(precision = 10, scale = 3)
    private BigDecimal seuilMax;

    @Column(nullable = false)
    private Integer nombreOccurrences;

    @Column(nullable = false)
    private Integer periodeObservation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NiveauPriorite niveauPriorite;

    @Column(nullable = false)
    private boolean actif;

    private Instant dateValidation;

    public RegleAlerte(
            String nom,
            String description,
            TypeMesure typeMesure,
            String operateur,
            BigDecimal seuilMin,
            BigDecimal seuilMax,
            Integer nombreOccurrences,
            Integer periodeObservation,
            NiveauPriorite niveauPriorite
    ) {
        this.nom = obligatoire(nom, "nom");
        this.description = normaliser(description);
        this.typeMesure = Objects.requireNonNull(typeMesure, "typeMesure");
        appliquerSeuils(operateur, seuilMin, seuilMax);
        this.nombreOccurrences = positif(nombreOccurrences, "nombreOccurrences");
        this.periodeObservation = positif(periodeObservation, "periodeObservation");
        this.niveauPriorite = Objects.requireNonNull(niveauPriorite, "niveauPriorite");
        this.actif = false;
    }

    public void modifierRegle(
            String description,
            String operateur,
            BigDecimal seuilMin,
            BigDecimal seuilMax,
            Integer nombreOccurrences,
            Integer periodeObservation
    ) {
        this.description = normaliser(description);
        appliquerSeuils(operateur, seuilMin, seuilMax);
        this.nombreOccurrences = positif(nombreOccurrences, "nombreOccurrences");
        this.periodeObservation = positif(periodeObservation, "periodeObservation");
        // Une modification des paramètres d'évaluation invalide la validation médicale précédente.
        this.dateValidation = null;
        this.actif = false;
    }

    public void validerRegle(Instant maintenant) {
        if (dateValidation != null) {
            throw new IllegalStateException("La règle est déjà validée");
        }
        this.dateValidation = Objects.requireNonNull(maintenant, "maintenant");
    }

    public void activerRegle() {
        if (dateValidation == null) {
            throw new IllegalStateException("La règle doit être validée avant activation");
        }
        if (actif) {
            throw new IllegalStateException("La règle est déjà active");
        }
        this.actif = true;
    }

    public void desactiverRegle() {
        if (!actif) {
            throw new IllegalStateException("La règle est déjà inactive");
        }
        this.actif = false;
    }

    public boolean evaluerMesure(BigDecimal valeur, int occurrencesRecentes) {
        if (!actif) {
            throw new IllegalStateException("Seule une règle active peut être évaluée");
        }
        return depasseSeuil(valeur) && occurrencesRecentes >= nombreOccurrences;
    }

    public boolean depasseSeuil(BigDecimal valeur) {
        Objects.requireNonNull(valeur, "valeur");
        return switch (operateur) {
            case ">" -> valeur.compareTo(seuilMin) > 0;
            case ">=" -> valeur.compareTo(seuilMin) >= 0;
            case "<" -> valeur.compareTo(seuilMin) < 0;
            case "<=" -> valeur.compareTo(seuilMin) <= 0;
            case "HORS_PLAGE" -> valeur.compareTo(seuilMin) < 0 || valeur.compareTo(seuilMax) > 0;
            default -> throw new IllegalStateException("Opérateur non supporté : " + operateur);
        };
    }

    private void appliquerSeuils(String operateur, BigDecimal seuilMin, BigDecimal seuilMax) {
        String op = obligatoire(operateur, "operateur");
        if (OPERATEURS_SIMPLES.contains(op)) {
            this.seuilMin = Objects.requireNonNull(seuilMin, "seuilMin");
            this.seuilMax = seuilMax;
        } else if (op.equals("HORS_PLAGE")) {
            this.seuilMin = Objects.requireNonNull(seuilMin, "seuilMin");
            this.seuilMax = Objects.requireNonNull(seuilMax, "seuilMax");
            if (seuilMin.compareTo(seuilMax) >= 0) {
                throw new IllegalArgumentException("seuilMin doit être strictement inférieur à seuilMax");
            }
        } else {
            throw new IllegalArgumentException("Opérateur non supporté : " + op);
        }
        this.operateur = op;
    }

    private static Integer positif(Integer valeur, String champ) {
        if (valeur == null || valeur <= 0) {
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

    private static String normaliser(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
