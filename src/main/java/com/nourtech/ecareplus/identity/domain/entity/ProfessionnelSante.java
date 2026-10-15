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
@Table(name = "professionnel_sante")
@PrimaryKeyJoinColumn(name = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ProfessionnelSante extends Utilisateur {

    @Column(nullable = false, unique = true, length = 60)
    private String numeroProfessionnel;

    @Column(nullable = false, length = 150)
    private String specialite;

    @Column(nullable = false, length = 100)
    private String fonction;

    @Column(nullable = false, length = 30)
    private String statutProfessionnel;

    @Column(nullable = false)
    private LocalDate datePriseFonction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private StructureSante structure;

    protected ProfessionnelSante(
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
            String fonction,
            LocalDate datePriseFonction,
            StructureSante structure
    ) {
        super(nom, prenoms, sexe, dateNaissance, telephone, email, motDePasseHash, languePreferee);
        this.numeroProfessionnel = texteObligatoire(numeroProfessionnel, "numeroProfessionnel");
        this.specialite = texteObligatoire(specialite, "specialite");
        this.fonction = texteObligatoire(fonction, "fonction");
        this.datePriseFonction = Objects.requireNonNull(datePriseFonction, "datePriseFonction");
        this.structure = Objects.requireNonNull(structure, "structure");
        this.statutProfessionnel = "ACTIF";
    }

    public void changerStructure(StructureSante structure) {
        this.structure = Objects.requireNonNull(structure, "structure");
    }

    public void suspendreActivite() {
        this.statutProfessionnel = "SUSPENDU";
    }
}
