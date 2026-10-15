package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
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

import java.util.Objects;

/**
 * Le diagramme ne relie explicitement qu'un ProfessionnelSante à l'observation.
 * Le rattachement au DossierSuivi est ajouté ici pour rendre l'observation exploitable
 * (retrouver les observations d'un patient suivi) ; à confirmer en équipe comme les autres
 * relations non figées du bloc suivi médical.
 */
@Getter
@Entity
@Table(name = "observation_suivi", indexes = {
        @Index(name = "idx_observation_dossier", columnList = "dossier_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ObservationSuivi extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierSuivi dossier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professionnel_id", nullable = false)
    private ProfessionnelSante professionnel;

    @Column(nullable = false, columnDefinition = "text")
    private String contenu;

    @Column(nullable = false, length = 50)
    private String typeObservation;

    @Column(nullable = false)
    private boolean confidentielle;

    @Column(nullable = false)
    private boolean archivee;

    public ObservationSuivi(
            DossierSuivi dossier,
            ProfessionnelSante professionnel,
            String contenu,
            String typeObservation,
            boolean confidentielle
    ) {
        this.dossier = Objects.requireNonNull(dossier, "dossier");
        this.professionnel = Objects.requireNonNull(professionnel, "professionnel");
        this.contenu = obligatoire(contenu, "contenu");
        this.typeObservation = obligatoire(typeObservation, "typeObservation");
        this.confidentielle = confidentielle;
        this.archivee = false;
    }

    public void modifierObservation(String contenu, String typeObservation, boolean confidentielle) {
        verifierNonArchivee();
        this.contenu = obligatoire(contenu, "contenu");
        this.typeObservation = obligatoire(typeObservation, "typeObservation");
        this.confidentielle = confidentielle;
    }

    public void archiverObservation() {
        verifierNonArchivee();
        this.archivee = true;
    }

    private void verifierNonArchivee() {
        if (archivee) {
            throw new IllegalStateException("L'observation est déjà archivée");
        }
    }

    private static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }
}
