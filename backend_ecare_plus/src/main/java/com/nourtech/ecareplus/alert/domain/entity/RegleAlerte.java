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
import java.util.UUID;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.care.domain.value.ValeurMesure;

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
    private UUID validateurId;
    @Column(length=30) private String sourceValidation;
    public void tracerValidation(UUID auteur,String source){validateurId=auteur;sourceValidation=source;}

    private UUID patientId;

    private UUID structureId;

    @Column(nullable = false, length = 20)
    private String composante = "PRINCIPALE";

    @Column(length = 20)
    private String unite;

    @Column(length = 100)
    private String contexteMesure;

    public void definirContexte(String contexte) {
        this.contexteMesure = normaliser(contexte);
        this.dateValidation = null;
        this.actif = false;
    }

    public boolean contexteApplicable(String contexte) {
        return contexteMesure == null || normaliserContexte(contexteMesure).equals(normaliserContexte(contexte));
    }

    private static String normaliserContexte(String contexte) {
        if (contexte == null) return "";
        return java.text.Normalizer.normalize(contexte, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").trim().toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
    }

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
        this.unite = ValeurMesure.uniteCanonique(typeMesure);
        appliquerSeuils(operateur, seuilMin, seuilMax);
        this.nombreOccurrences = positif(nombreOccurrences, "nombreOccurrences");
        this.periodeObservation = positif(periodeObservation, "periodeObservation");
        this.niveauPriorite = Objects.requireNonNull(niveauPriorite, "niveauPriorite");
        this.actif = false;
    }

    public void definirPortee(UUID patientId, UUID structureId, String composante, String unite) {
        String component = composante == null || composante.isBlank() ? "PRINCIPALE" : composante;
        if (!Set.of("PRINCIPALE", "DIASTOLIQUE").contains(component))
            throw new IllegalArgumentException("composante doit être PRINCIPALE ou DIASTOLIQUE");
        if (component.equals("DIASTOLIQUE") && typeMesure != TypeMesure.TENSION_ARTERIELLE)
            throw new IllegalArgumentException("DIASTOLIQUE est réservée à la tension artérielle");
        String unit = unite == null || unite.isBlank() ? ValeurMesure.uniteCanonique(typeMesure) : unite.trim();
        if (!unit.equals(ValeurMesure.uniteCanonique(typeMesure)))
            throw new IllegalArgumentException("Les seuils utilisent l'unité canonique " + ValeurMesure.uniteCanonique(typeMesure));
        this.patientId = patientId;
        this.structureId = structureId;
        this.composante = component;
        this.unite = unit;
        this.dateValidation = null;
        this.actif = false;
    }

    public BigDecimal valeurEvaluee(MesureSante mesure) {
        if (mesure.getTypeMesure() != typeMesure || unite == null || !unite.equals(mesure.getUnite()))
            throw new IllegalArgumentException("La mesure et la règle doivent utiliser le même type et la même unité");
        return "DIASTOLIQUE".equals(composante) ? mesure.getValeurSecondaire() : mesure.getValeurPrincipale();
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
            case "VARIATION_ABSOLUE" -> valeur.compareTo(seuilMin) >= 0;
            case "HORS_PLAGE" -> valeur.compareTo(seuilMin) < 0 || valeur.compareTo(seuilMax) > 0;
            default -> throw new IllegalStateException("Opérateur non supporté : " + operateur);
        };
    }

    private void appliquerSeuils(String operateur, BigDecimal seuilMin, BigDecimal seuilMax) {
        String op = obligatoire(operateur, "operateur");
        if(op.equals("VARIATION_ABSOLUE")){
            if(typeMesure!=TypeMesure.POIDS||seuilMin==null||seuilMin.signum()<=0)throw new IllegalArgumentException("Variation positive de poids attendue");this.seuilMin=seuilMin;this.seuilMax=null;
        } else if (OPERATEURS_SIMPLES.contains(op)) {
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
