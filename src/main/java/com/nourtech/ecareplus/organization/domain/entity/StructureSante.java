package com.nourtech.ecareplus.organization.domain.entity;

import com.nourtech.ecareplus.organization.domain.enumeration.StatutStructure;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "structure_sante")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StructureSante extends AbstractEntity {

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(nullable = false, length = 80)
    private String typeStructure;

    @Column(nullable = false, length = 300)
    private String adresse;

    @Column(nullable = false, length = 100)
    private String ville;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(nullable = false, length = 30)
    private String telephone;

    @Column(length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutStructure statut;

    @Column(nullable = false, unique = true, length = 100)
    private String numeroAutorisation;

    @Column(nullable = false)
    private LocalDate dateAdhesion;

    public StructureSante(
            String nom,
            String typeStructure,
            String adresse,
            String ville,
            String region,
            String telephone,
            String email,
            String numeroAutorisation
    ) {
        this.nom = obligatoire(nom, "nom");
        this.typeStructure = obligatoire(typeStructure, "typeStructure");
        this.adresse = obligatoire(adresse, "adresse");
        this.ville = obligatoire(ville, "ville");
        this.region = obligatoire(region, "region");
        this.telephone = obligatoire(telephone, "telephone");
        this.email = nullable(email);
        this.numeroAutorisation = obligatoire(numeroAutorisation, "numeroAutorisation");
        this.dateAdhesion = LocalDate.now();
        this.statut = StatutStructure.EN_ATTENTE;
    }

    public void modifierInformations(
            String nom,
            String adresse,
            String ville,
            String region,
            String telephone,
            String email
    ) {
        this.nom = obligatoire(nom, "nom");
        this.adresse = obligatoire(adresse, "adresse");
        this.ville = obligatoire(ville, "ville");
        this.region = obligatoire(region, "region");
        this.telephone = obligatoire(telephone, "telephone");
        this.email = nullable(email);
    }

    public void activer() {
        this.statut = StatutStructure.ACTIVE;
    }

    public void suspendreService() {
        this.statut = StatutStructure.SUSPENDUE;
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
