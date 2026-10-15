package com.nourtech.ecareplus.identity.domain.entity;

import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
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
@Table(name = "proche_aidant")
@PrimaryKeyJoinColumn(name = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcheAidant extends Utilisateur {

    @Column(nullable = false, length = 100)
    private String lienAvecPatient;

    @Column(length = 30)
    private String telephoneSecondaire;

    @Column(nullable = false, length = 30)
    private String statutAutorisation;

    public ProcheAidant(
            String nom,
            String prenoms,
            Sexe sexe,
            LocalDate dateNaissance,
            String telephone,
            String email,
            String motDePasseHash,
            String languePreferee,
            String lienAvecPatient
    ) {
        super(nom, prenoms, sexe, dateNaissance, telephone, email, motDePasseHash, languePreferee);
        this.lienAvecPatient = texteObligatoire(lienAvecPatient, "lienAvecPatient");
        this.statutAutorisation = "EN_ATTENTE";
    }

    public void modifierLien(String lienAvecPatient, String telephoneSecondaire) {
        this.lienAvecPatient = texteObligatoire(lienAvecPatient, "lienAvecPatient");
        this.telephoneSecondaire = normaliserNullable(telephoneSecondaire);
    }

    public void accepterInvitation() {
        this.statutAutorisation = "ACTIVE";
    }
}
