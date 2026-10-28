package com.nourtech.ecareplus.alert.domain.entity;
import com.nourtech.ecareplus.care.domain.enumeration.TypeMesure;
import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
/** Immutable parameters associated with the rule at alert creation; no reconstruction of old alerts. */
@Embeddable @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class InstantaneRegleAlerte {
    @Column(name="capture_regle_version",updatable=false)
    private Long version;
    @Column(name="capture_regle_nom",updatable=false,length=150)
    private String nom;
    @Enumerated(EnumType.STRING)
    @Column(name="capture_regle_type_mesure",updatable=false,length=30)
    private TypeMesure typeMesure;
    @Column(name="capture_regle_operateur",updatable=false,length=20)
    private String operateur;
    @Column(name="capture_regle_seuil_min",updatable=false,precision=10,scale=3)
    private BigDecimal seuilMin;
    @Column(name="capture_regle_seuil_max",updatable=false,precision=10,scale=3)
    private BigDecimal seuilMax;
    @Column(name="capture_regle_nombre_occurrences",updatable=false)
    private Integer nombreOccurrences;
    @Column(name="capture_regle_periode_observation",updatable=false)
    private Integer periodeObservation;
    @Enumerated(EnumType.STRING)
    @Column(name="capture_regle_niveau_priorite",updatable=false,length=20)
    private NiveauPriorite niveauPriorite;
    @Column(name="capture_regle_composante",updatable=false,length=20)
    private String composante;
    @Column(name="capture_regle_unite",updatable=false,length=20)
    private String unite;
    @Column(name="capture_regle_contexte_mesure",updatable=false,length=100)
    private String contexteMesure;
    @Column(name="capture_regle_date_validation",updatable=false)
    private Instant dateValidation;
    @Column(name="capture_regle_validateur_id",updatable=false)
    private UUID validateurId;
    @Column(name="capture_regle_source_validation",updatable=false,length=30)
    private String sourceValidation;
    @Column(name="capture_regle_actif",updatable=false)
    private Boolean actif;
    public InstantaneRegleAlerte(RegleAlerte r){
        version=r.getVersion();
        nom=r.getNom();
        typeMesure=r.getTypeMesure();
        operateur=r.getOperateur();
        seuilMin=r.getSeuilMin();
        seuilMax=r.getSeuilMax();
        nombreOccurrences=r.getNombreOccurrences();
        periodeObservation=r.getPeriodeObservation();
        niveauPriorite=r.getNiveauPriorite();
        composante=r.getComposante();
        unite=r.getUnite();
        contexteMesure=r.getContexteMesure();
        dateValidation=r.getDateValidation();
        validateurId=r.getValidateurId();
        sourceValidation=r.getSourceValidation();
        actif=r.isActif();
    }
}
