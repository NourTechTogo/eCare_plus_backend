package com.nourtech.ecareplus.communication.domain.entity;

import com.nourtech.ecareplus.communication.domain.enumeration.StatutValidationContenu;
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

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(name = "contenu_educatif")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContenuEducatif extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id", nullable = false)
    private Medecin auteur;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(length = 500)
    private String resume;

    @Column(nullable = false, columnDefinition = "text")
    private String contenu;

    @Column(nullable = false, length = 50)
    private String typeContenu;

    @Column(nullable = false, length = 10)
    private String langue;

    @Column(length = 150)
    private String pathologieCible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutValidationContenu statutValidation;

    private Instant datePublication;

    @Column(length = 500)
    private String urlMedia;

    public ContenuEducatif(
            Medecin auteur,
            String titre,
            String resume,
            String contenu,
            String typeContenu,
            String langue,
            String pathologieCible,
            String urlMedia
    ) {
        this.auteur = Objects.requireNonNull(auteur, "auteur");
        this.titre = obligatoire(titre, "titre");
        this.resume = normaliser(resume);
        this.contenu = obligatoire(contenu, "contenu");
        this.typeContenu = obligatoire(typeContenu, "typeContenu");
        this.langue = obligatoire(langue, "langue");
        this.pathologieCible = normaliser(pathologieCible);
        this.urlMedia = normaliser(urlMedia);
        this.statutValidation = StatutValidationContenu.BROUILLON;
    }

    public void modifierContenu(String titre, String resume, String contenu, String urlMedia) {
        if (statutValidation != StatutValidationContenu.BROUILLON) {
            throw new IllegalStateException("Seul un contenu en brouillon peut être modifié");
        }
        this.titre = obligatoire(titre, "titre");
        this.resume = normaliser(resume);
        this.contenu = obligatoire(contenu, "contenu");
        this.urlMedia = normaliser(urlMedia);
    }

    public void validerContenu() {
        if (statutValidation != StatutValidationContenu.BROUILLON) {
            throw new IllegalStateException("Seul un contenu en brouillon peut être validé");
        }
        this.statutValidation = StatutValidationContenu.VALIDE;
    }

    public void publier(Instant maintenant) {
        if (statutValidation != StatutValidationContenu.VALIDE) {
            throw new IllegalStateException("Seul un contenu validé peut être publié");
        }
        this.statutValidation = StatutValidationContenu.PUBLIE;
        this.datePublication = Objects.requireNonNull(maintenant, "maintenant");
    }

    public void archiver() {
        if (statutValidation != StatutValidationContenu.PUBLIE) {
            throw new IllegalStateException("Seul un contenu publié peut être archivé");
        }
        this.statutValidation = StatutValidationContenu.ARCHIVE;
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
