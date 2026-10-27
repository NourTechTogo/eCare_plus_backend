package com.nourtech.ecareplus.mobile.api.dto;

import com.nourtech.ecareplus.auth.api.dto.ProfilAuthentifieResponse;
import com.nourtech.ecareplus.care.domain.entity.DossierSuivi;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class EspacePatientResponse {
    private EspacePatientResponse() {}

    public record Dossier(UUID id, String statut, String structure, LocalDate dateOuverture,
                          String resumeMedical, String frequenceSuivi, LocalDate prochainControle) {
        public static Dossier depuis(DossierSuivi source) {
            return new Dossier(source.getId(), source.getStatut().name(), source.getStructure()==null?null:source.getStructure().getNom(),
                    source.getDateOuverture(), source.getResumeMedical(), source.getFrequenceSuivi(),
                    source.getProchainControle());
        }
    }

    public record Mesure(UUID id, String typeMesure, BigDecimal valeurPrincipale,
                         BigDecimal valeurSecondaire, String unite, Instant dateMesure,
                         String contexteMesure, String commentaire, boolean valeurConfirmee, UUID patientId, String source, String sourceRef) {
        public static Mesure depuis(MesureSante source) {
            return new Mesure(source.getId(), source.getTypeMesure().name(), source.getValeurPrincipale(),
                    source.getValeurSecondaire(), source.getUnite(), source.getDateMesure(),
                    source.getContexteMesure(), source.getCommentaire(), source.isValeurConfirmee(),source.getPatient().getId(),source.getSource().name(),source.getSourceRef());
        }
    }
}
