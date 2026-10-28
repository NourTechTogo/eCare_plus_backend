package com.nourtech.ecareplus.mobile.api.dto;
import com.nourtech.ecareplus.alert.api.dto.response.RegleAppliqueeVue;
import com.nourtech.ecareplus.care.application.service.PlanningTraitementService.OccurrenceVue;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
public record SyntheseConsultationVue(UUID id,int versionSchema,PatientVue patient,LocalDate du,LocalDate au,String fuseau,
        Instant genereLe,long nombreMesures,long joursSansSaisie,List<SerieMesureVue> seriesMesures,
        List<TraitementVue> traitements,List<OccurrenceVue> reponsesPrises,List<AlerteVue> alertes,
        List<RendezVousVue> rendezVous,List<String> avertissements){
    public SyntheseConsultationVue avecIdentifiant(UUID identifiant){
        return new SyntheseConsultationVue(Objects.requireNonNull(identifiant),versionSchema,patient,du,au,fuseau,genereLe,nombreMesures,joursSansSaisie,seriesMesures,traitements,reponsesPrises,alertes,rendezVous,avertissements);
    }
    public record PatientVue(UUID id,String nom,String prenoms,LocalDate dateNaissance){}
    public record PointMesureVue(UUID mesureId,Instant dateMesure,BigDecimal valeur,String contexte,String source){}
    public record SerieMesureVue(String typeMesure,String composante,String unite,long nombre,BigDecimal minimum,
            BigDecimal maximum,BigDecimal moyenne,BigDecimal derniereValeur,Instant derniereLe,
            long joursSansSaisie,List<PointMesureVue> points){}
    public record HoraireVue(UUID id,String medicament,String dosage,String uniteDosage,BigDecimal quantite,
            String unite,LocalTime heurePrevue,String fuseau,Set<Integer> joursSemaine,boolean actif){}
    public record TraitementVue(UUID id,String libelle,LocalDate dateDebut,LocalDate dateFin,String statutCourant,
            boolean declarationPersonnelle,List<HoraireVue> horaires){}
    public record AlerteVue(UUID id,Instant dateEvenement,String origine,String titre,String niveauPriorite,
            String statut,RegleAppliqueeVue regleAppliquee){}
    public record RendezVousVue(UUID id,String motif,Instant dateHeure,String fuseau,String statut,String lieu,String professionnelNom){}
}
