package com.nourtech.ecareplus.identity.domain.entity;

import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutSuivi;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "patient")
@PrimaryKeyJoinColumn(name = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Patient extends Utilisateur {

    @Column(nullable = false, unique = true, length = 40)
    private String numeroPatient;

    @Column(length = 10)
    private String groupeSanguin;

    @Column(length = 300)
    private String adresse;

    @Column(name = "personne_a_contacter", length = 200)
    private String personneAContacter;

    @Column(length = 30)
    private String telephoneUrgence;

    @Column(length = 100) private String ville;
    @Column(length = 100) private String pays;
    private Double tailleCm;
    private Double poidsKg;
    @Column(columnDefinition = "text") private String allergies;
    @Column(columnDefinition = "text") private String antecedents;
    @Column(length = 40) private String pathologie;

    public void completerProfil(String ville, String pays, Double tailleCm, Double poidsKg, String allergies, String antecedents, String pathologie) {
        if (tailleCm != null && (!Double.isFinite(tailleCm) || tailleCm < 20 || tailleCm > 300)) throw new IllegalArgumentException("Taille invalide");
        if (poidsKg != null && (!Double.isFinite(poidsKg) || poidsKg < 1 || poidsKg > 700)) throw new IllegalArgumentException("Poids invalide");
        if (pathologie != null && !java.util.Set.of("diabete", "hypertension", "diabete_hypertension").contains(pathologie)) throw new IllegalArgumentException("Pathologie invalide");
        this.ville = normaliserNullable(ville); this.pays = normaliserNullable(pays);
        this.tailleCm = tailleCm; this.poidsKg = poidsKg; this.allergies = normaliserNullable(allergies);
        this.antecedents = normaliserNullable(antecedents); this.pathologie = pathologie;
    }

    @Column(length = 100)
    private String codePinHash;

    @Column(nullable = false, length = 30)
    private String modeConnexionPrefere;

    @Column(nullable = false)
    private LocalDate dateInscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSuivi statutSuivi;

    public Patient(
            String numeroPatient,
            String nom,
            String prenoms,
            Sexe sexe,
            LocalDate dateNaissance,
            String telephone,
            String email,
            String motDePasseHash,
            String languePreferee
    ) {
        super(nom, prenoms, sexe, dateNaissance, telephone, email, motDePasseHash, languePreferee);
        this.numeroPatient = texteObligatoire(numeroPatient, "numeroPatient");
        this.modeConnexionPrefere = "MOT_DE_PASSE";
        this.dateInscription = LocalDate.now();
        this.statutSuivi = StatutSuivi.EN_ATTENTE;
    }

    public void renseignerInformationsSuivi(
            String groupeSanguin,
            String adresse,
            String personneAContacter,
            String telephoneUrgence,
            String modeConnexionPrefere
    ) {
        this.groupeSanguin = normaliserNullable(groupeSanguin);
        this.adresse = normaliserNullable(adresse);
        this.personneAContacter = normaliserNullable(personneAContacter);
        this.telephoneUrgence = normaliserNullable(telephoneUrgence);
        this.modeConnexionPrefere = texteObligatoire(modeConnexionPrefere, "modeConnexionPrefere");
    }

    public void changerCodePin(String codePinHash) {
        this.codePinHash = texteObligatoire(codePinHash, "codePinHash");
    }

    public void activerSuivi() {
        this.statutSuivi = StatutSuivi.ACTIF;
    }

    public void suspendreSuivi() {
        this.statutSuivi = StatutSuivi.SUSPENDU;
    }

    public void terminerSuivi() {
        this.statutSuivi = StatutSuivi.TERMINE;
    }
}
