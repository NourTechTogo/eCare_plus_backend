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
@Table(name = "medecin")
@PrimaryKeyJoinColumn(name = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medecin extends ProfessionnelSante {

    @Column(nullable = false, unique = true, length = 80)
    private String numeroOrdre;

    @Column(nullable = false, length = 150)
    private String domaineSpecialisation;

    public Medecin(
            String numeroProfessionnel,
            String numeroOrdre,
            String nom,
            String prenoms,
            Sexe sexe,
            LocalDate dateNaissance,
            String telephone,
            String email,
            String motDePasseHash,
            String languePreferee,
            String specialite,
            String domaineSpecialisation,
            LocalDate datePriseFonction,
            StructureSante structure
    ) {
        super(numeroProfessionnel, nom, prenoms, sexe, dateNaissance, telephone, email, motDePasseHash,
                languePreferee, specialite, "MEDECIN", datePriseFonction, structure);
        this.numeroOrdre = texteObligatoire(numeroOrdre, "numeroOrdre");
        this.domaineSpecialisation = texteObligatoire(domaineSpecialisation, "domaineSpecialisation");
    }
}
