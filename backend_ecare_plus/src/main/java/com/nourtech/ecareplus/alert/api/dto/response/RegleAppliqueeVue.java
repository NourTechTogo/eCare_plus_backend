package com.nourtech.ecareplus.alert.api.dto.response;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.nourtech.ecareplus.alert.domain.entity.InstantaneRegleAlerte;
public record RegleAppliqueeVue(
        Long version,
        String nom,
        TypeMesure typeMesure,
        String operateur,
        BigDecimal seuilMin,
        BigDecimal seuilMax,
        Integer nombreOccurrences,
        Integer periodeObservation,
        NiveauPriorite niveauPriorite,
        String composante,
        String unite,
        String contexteMesure,
        Instant dateValidation,
        UUID validateurId,
        String sourceValidation,
        Boolean actif
) {
    public static RegleAppliqueeVue depuis(InstantaneRegleAlerte capture){
        if(capture==null)return null;
        return new RegleAppliqueeVue(capture.getVersion(),
                capture.getNom(),
                capture.getTypeMesure(),
                capture.getOperateur(),
                capture.getSeuilMin(),
                capture.getSeuilMax(),
                capture.getNombreOccurrences(),
                capture.getPeriodeObservation(),
                capture.getNiveauPriorite(),
                capture.getComposante(),
                capture.getUnite(),
                capture.getContexteMesure(),
                capture.getDateValidation(),
                capture.getValidateurId(),
                capture.getSourceValidation(),
                capture.getActif());
    }
}
