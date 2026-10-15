package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.care.domain.enumeration.StatutObjectif;
import com.nourtech.ecareplus.identity.domain.entity.Medecin;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "objectif_suivi")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ObjectifSuivi extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierSuivi dossier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Medecin definiPar;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(columnDefinition = "text")
    private String description;

    @Column(precision = 12, scale = 3)
    private BigDecimal valeurCibleMin;

    @Column(precision = 12, scale = 3)
    private BigDecimal valeurCibleMax;

    @Column(length = 30)
    private String unite;

    @Column(nullable = false)
    private LocalDate dateDebut;

    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutObjectif statut;

    public ObjectifSuivi(
            DossierSuivi dossier,
            Medecin definiPar,
            String libelle,
            String description,
            BigDecimal valeurCibleMin,
            BigDecimal valeurCibleMax,
            String unite,
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        this.dossier = Objects.requireNonNull(dossier, "dossier");
        this.definiPar = Objects.requireNonNull(definiPar, "definiPar");
        if (!definiPar.getStructure().memeIdentiteQue(dossier.getStructure())) {
            throw new IllegalArgumentException("Le médecin doit appartenir à la structure du dossier");
        }
        this.libelle = obligatoire(libelle, "libelle");
        this.description = nullable(description);
        this.valeurCibleMin = valeurCibleMin;
        this.valeurCibleMax = valeurCibleMax;
        this.unite = nullable(unite);
        this.dateDebut = Objects.requireNonNull(dateDebut, "dateDebut");
        this.dateFin = dateFin;
        verifierBornesEtDates();
        this.statut = dateDebut.isAfter(LocalDate.now()) ? StatutObjectif.PLANIFIE : StatutObjectif.ACTIF;
    }

    public void modifierCibles(BigDecimal minimum, BigDecimal maximum, LocalDate dateFin) {
        if (statut == StatutObjectif.ATTEINT || statut == StatutObjectif.NON_ATTEINT || statut == StatutObjectif.ANNULE) {
            throw new IllegalStateException("Un objectif terminé ne peut plus être modifié");
        }
        this.valeurCibleMin = minimum;
        this.valeurCibleMax = maximum;
        this.dateFin = dateFin;
        verifierBornesEtDates();
    }

    public void cloturer(boolean atteint) {
        this.statut = atteint ? StatutObjectif.ATTEINT : StatutObjectif.NON_ATTEINT;
        if (dateFin == null) {
            dateFin = LocalDate.now();
        }
    }

    private void verifierBornesEtDates() {
        if (valeurCibleMin != null && valeurCibleMax != null && valeurCibleMin.compareTo(valeurCibleMax) > 0) {
            throw new IllegalArgumentException("La cible minimale ne peut pas dépasser la cible maximale");
        }
        if (dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début");
        }
    }

    private static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }

    private static String nullable(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
