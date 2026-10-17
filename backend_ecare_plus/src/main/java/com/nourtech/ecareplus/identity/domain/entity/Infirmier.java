package com.nourtech.ecareplus.identity.domain.entity;

import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "infirmier")
@PrimaryKeyJoinColumn(name = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Infirmier extends ProfessionnelSante {

    @Column(nullable = false, length = 100)
    private String service;

    @Column(nullable = false, length = 80)
    private String niveauResponsabilite;

    public Infirmier(
            String numeroProfessionnel,
            String nom,
            String prenoms,
            Sexe sexe,
            LocalDate dateNaissance,
            String telephone,
            String email,
            String motDePasseHash,
            String languePreferee,
            String specialite,
            String service,
            String niveauResponsabilite,
            LocalDate datePriseFonction,
            StructureSante structure
    ) {
        super(numeroProfessionnel, nom, prenoms, sexe, dateNaissance, telephone, email, motDePasseHash,
                languePreferee, specialite, "INFIRMIER", datePriseFonction, structure);
        this.service = texteObligatoire(service, "service");
        this.niveauResponsabilite = texteObligatoire(niveauResponsabilite, "niveauResponsabilite");
    }
}
