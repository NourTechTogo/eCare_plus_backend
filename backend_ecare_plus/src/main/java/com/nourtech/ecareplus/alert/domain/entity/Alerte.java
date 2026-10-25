package com.nourtech.ecareplus.alert.domain.entity;

import com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite;
import com.nourtech.ecareplus.alert.domain.enumeration.OrigineAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.StatutAlerte;
import com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
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
@Table(name = "alerte", indexes = {
        @Index(name = "idx_alerte_patient_statut", columnList = "patient_id,statut"),
        @Index(name = "idx_alerte_professionnel", columnList = "professionnel_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alerte extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesure_id")
    private MesureSante mesure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regle_id")
    private RegleAlerte regle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professionnel_id")
    private ProfessionnelSante professionnel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeAlerte typeAlerte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigineAlerte origine;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NiveauPriorite niveauPriorite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAlerte statut;

    private Instant datePriseEnCharge;

    private Instant dateCloture;

    @Column(columnDefinition = "text")
    private String motifCloture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloturee_par_id")
    private ProfessionnelSante clotureePar;

    public Alerte(
            Patient patient,
            MesureSante mesure,
            RegleAlerte regle,
            TypeAlerte typeAlerte,
            String titre,
            String description,
            NiveauPriorite niveauPriorite
    ) {
        this(patient, mesure, regle, typeAlerte, titre, description, niveauPriorite, OrigineAlerte.MANUELLE);
    }

    public Alerte(
            Patient patient,
            MesureSante mesure,
            RegleAlerte regle,
            TypeAlerte typeAlerte,
            String titre,
            String description,
            NiveauPriorite niveauPriorite,
            OrigineAlerte origine
    ) {
        this.patient = Objects.requireNonNull(patient, "patient");
        this.mesure = mesure;
        this.regle = regle;
        this.typeAlerte = Objects.requireNonNull(typeAlerte, "typeAlerte");
        this.origine = Objects.requireNonNull(origine, "origine");
        this.titre = obligatoire(titre, "titre");
        this.description = normaliser(description);
        this.niveauPriorite = Objects.requireNonNull(niveauPriorite, "niveauPriorite");
        this.statut = StatutAlerte.NOUVELLE;
    }

    public void affecter(ProfessionnelSante professionnel) {
        verifierNonCloturee();
        this.professionnel = Objects.requireNonNull(professionnel, "professionnel");
    }

    public void prendreEnCharge(Instant maintenant) {
        if (statut != StatutAlerte.NOUVELLE) {
            throw new IllegalStateException("Seule une alerte nouvelle peut être prise en charge");
        }
        if (professionnel == null) {
            throw new IllegalStateException("L'alerte doit être affectée avant d'être prise en charge");
        }
        this.statut = StatutAlerte.PRISE_EN_CHARGE;
        this.datePriseEnCharge = Objects.requireNonNull(maintenant, "maintenant");
    }

    public void transmettre(ProfessionnelSante nouveauProfessionnel) {
        if (statut != StatutAlerte.PRISE_EN_CHARGE) {
            throw new IllegalStateException("Seule une alerte prise en charge peut être transmise");
        }
        this.professionnel = Objects.requireNonNull(nouveauProfessionnel, "nouveauProfessionnel");
        this.statut = StatutAlerte.TRANSMISE;
    }

    public void cloturer(String motifCloture, Instant maintenant) {
        cloturer(motifCloture, maintenant, null);
    }

    public void cloturer(String motifCloture, Instant maintenant, ProfessionnelSante auteur) {
        verifierNonCloturee();
        this.statut = StatutAlerte.CLOTUREE;
        this.dateCloture = Objects.requireNonNull(maintenant, "maintenant");
        this.motifCloture = obligatoire(motifCloture, "motifCloture");
        this.clotureePar = auteur;
    }

    public void rouvrir() {
        if (statut != StatutAlerte.CLOTUREE) {
            throw new IllegalStateException("Seule une alerte clôturée peut être rouverte");
        }
        this.statut = StatutAlerte.NOUVELLE;
        this.dateCloture = null;
        this.motifCloture = null;
        this.clotureePar = null;
    }

    private void verifierNonCloturee() {
        if (statut == StatutAlerte.CLOTUREE) {
            throw new IllegalStateException("L'alerte est déjà clôturée");
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
