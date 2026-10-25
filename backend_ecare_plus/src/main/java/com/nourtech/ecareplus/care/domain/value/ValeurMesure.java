package com.nourtech.ecareplus.care.domain.value;

import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

/** Canonical values shared by all entry points, before plausibility or alert evaluation. */
public record ValeurMesure(BigDecimal principale, BigDecimal secondaire, String unite) {
    public static ValeurMesure normaliser(TypeMesure type, BigDecimal principale, BigDecimal secondaire, String unite) {
        Objects.requireNonNull(type, "typeMesure");
        if (principale == null) throw new IllegalArgumentException("valeurPrincipale est obligatoire");
        if (unite == null || unite.isBlank()) throw new IllegalArgumentException("unite est obligatoire");
        String u = unite.trim().toLowerCase(Locale.ROOT).replace(" ", "");
        String canonique;
        BigDecimal min;
        BigDecimal max;
        switch (type) {
            case GLYCEMIE -> {
                canonique = "mg/dL";
                principale = switch (u) {
                    case "mg/dl" -> principale;
                    case "g/l" -> principale.multiply(new BigDecimal("100"));
                    case "mmol/l" -> principale.multiply(new BigDecimal("18.0182"));
                    default -> throw uniteInvalide(type, "mg/dL, g/L ou mmol/L");
                };
                min = new BigDecimal("10"); max = new BigDecimal("700");
            }
            case TENSION_ARTERIELLE -> {
                if (!u.equals("mmhg")) throw uniteInvalide(type, "mmHg");
                canonique = "mmHg"; min = new BigDecimal("40"); max = new BigDecimal("260");
                if (secondaire == null) throw new IllegalArgumentException("valeurSecondaire (diastolique) est obligatoire");
                plage("valeurSecondaire (diastolique)", secondaire, new BigDecimal("20"), new BigDecimal("200"));
                if (principale.compareTo(secondaire) <= 0)
                    throw new IllegalArgumentException("La systolique doit être supérieure à la diastolique");
            }
            case POIDS -> {
                if (!u.equals("kg")) throw uniteInvalide(type, "kg");
                canonique = "kg"; min = BigDecimal.ONE; max = new BigDecimal("400");
            }
            case FREQUENCE_CARDIAQUE -> {
                if (!u.equals("bpm")) throw uniteInvalide(type, "bpm");
                canonique = "bpm"; min = new BigDecimal("20"); max = new BigDecimal("250");
            }
            case TEMPERATURE -> {
                if (!u.equals("°c") && !u.equals("c")) throw uniteInvalide(type, "°C");
                canonique = "°C"; min = new BigDecimal("25"); max = new BigDecimal("45");
            }
            case SPO2 -> {
                if (!u.equals("%")) throw uniteInvalide(type, "%");
                canonique = "%"; min = BigDecimal.ZERO; max = new BigDecimal("100");
            }
            default -> throw new IllegalArgumentException("Type de mesure non supporté");
        }
        plage("valeurPrincipale (" + canonique + ")", principale, min, max);
        if (type != TypeMesure.TENSION_ARTERIELLE && secondaire != null)
            throw new IllegalArgumentException("valeurSecondaire est réservée à la tension artérielle");
        return new ValeurMesure(principale.setScale(3, RoundingMode.HALF_UP),
                secondaire == null ? null : secondaire.setScale(3, RoundingMode.HALF_UP), canonique);
    }
    public static String uniteCanonique(TypeMesure type) {
        return switch (type) {
            case GLYCEMIE -> "mg/dL";
            case TENSION_ARTERIELLE -> "mmHg";
            case POIDS -> "kg";
            case FREQUENCE_CARDIAQUE -> "bpm";
            case TEMPERATURE -> "°C";
            case SPO2 -> "%";
        };
    }
    private static void plage(String champ, BigDecimal valeur, BigDecimal min, BigDecimal max) {
        if (valeur.compareTo(min) < 0 || valeur.compareTo(max) > 0)
            throw new IllegalArgumentException(champ + " doit être compris entre " + min + " et " + max);
    }
    private static IllegalArgumentException uniteInvalide(TypeMesure type, String attendue) {
        return new IllegalArgumentException("unite incompatible avec " + type + " ; attendu : " + attendue);
    }
}
