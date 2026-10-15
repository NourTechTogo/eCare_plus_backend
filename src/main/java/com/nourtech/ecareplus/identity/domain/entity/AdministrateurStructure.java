package com.nourtech.ecareplus.identity.domain.entity;

import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "administrateur_structure")
@PrimaryKeyJoinColumn(name = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdministrateurStructure extends Utilisateur {

    @Column(nullable = false, length = 120)
    private String fonctionAdministrative;

    @Column(nullable = false, length = 50)
    private String niveauAcces;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private StructureSante structure;

    public AdministrateurStructure(
            String nom,
            String prenoms,
            Sexe sexe,
            LocalDate dateNaissance,
            String telephone,
            String email,
            String motDePasseHash,
            String languePreferee,
            String fonctionAdministrative,
            String niveauAcces,
            StructureSante structure
    ) {
        super(nom, prenoms, sexe, dateNaissance, telephone, email, motDePasseHash, languePreferee);
        this.fonctionAdministrative = texteObligatoire(fonctionAdministrative, "fonctionAdministrative");
        this.niveauAcces = texteObligatoire(niveauAcces, "niveauAcces");
        this.structure = Objects.requireNonNull(structure, "structure");
    }
}
