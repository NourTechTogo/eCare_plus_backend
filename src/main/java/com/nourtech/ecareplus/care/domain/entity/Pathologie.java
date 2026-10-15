package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.care.domain.enumeration.NiveauRisque;
import com.nourtech.ecareplus.care.domain.enumeration.StatutPathologie;
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

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "pathologie", indexes = {
        @Index(name = "idx_pathologie_dossier", columnList = "dossier_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pathologie extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierSuivi dossier;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 40)
    private String code;

    @Column(columnDefinition = "text")
    private String description;

    private LocalDate dateDiagnosticDeclaree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutPathologie statut;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NiveauRisque niveauRisque;

    public Pathologie(
            DossierSuivi dossier,
            String nom,
            String code,
            String description,
            LocalDate dateDiagnosticDeclaree,
            NiveauRisque niveauRisque
    ) {
        this.dossier = Objects.requireNonNull(dossier, "dossier");
        this.nom = obligatoire(nom, "nom");
        this.code = normaliser(code);
        this.description = normaliser(description);
        this.dateDiagnosticDeclaree = dateDiagnosticDeclaree;
        this.niveauRisque = niveauRisque;
        this.statut = StatutPathologie.ACTIVE;
    }

    public void modifier(String nom, String code, String description, NiveauRisque niveauRisque) {
        verifierActive();
        this.nom = obligatoire(nom, "nom");
        this.code = normaliser(code);
        this.description = normaliser(description);
        this.niveauRisque = niveauRisque;
    }

    public void desactiver() {
        verifierActive();
        this.statut = StatutPathologie.INACTIVE;
    }

    private void verifierActive() {
        if (statut != StatutPathologie.ACTIVE) {
            throw new IllegalStateException("La pathologie doit être active");
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
