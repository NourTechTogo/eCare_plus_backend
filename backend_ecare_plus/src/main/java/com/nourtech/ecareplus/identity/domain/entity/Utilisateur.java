package com.nourtech.ecareplus.identity.domain.entity;

import com.nourtech.ecareplus.identity.domain.enumeration.Sexe;
import com.nourtech.ecareplus.identity.domain.enumeration.StatutCompte;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

@Getter
@Entity
@Table(name = "utilisateur")
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Utilisateur extends AbstractEntity {

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 150)
    private String prenoms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sexe sexe;

    private LocalDate dateNaissance;

    @Column(unique = true, length = 30)
    private String telephone;

    @Column(unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 100)
    private String motDePasseHash;

    @Column(nullable = false, length = 10)
    private String languePreferee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutCompte statutCompte;

    private Instant derniereConnexion;

    @Column(nullable = false)
    private long sessionVersion;

    public void modifierEtatCivil(Sexe sexe, LocalDate dateNaissance) {
        this.sexe = Objects.requireNonNullElse(sexe, Sexe.NON_RENSEIGNE);
        if (dateNaissance != null && dateNaissance.isAfter(LocalDate.now())) throw new IllegalArgumentException("Date de naissance future");
        this.dateNaissance = dateNaissance;
    }

    protected Utilisateur(
            String nom,
            String prenoms,
            Sexe sexe,
            LocalDate dateNaissance,
            String telephone,
            String email,
            String motDePasseHash,
            String languePreferee
    ) {
        this.nom = texteObligatoire(nom, "nom");
        this.prenoms = texteObligatoire(prenoms, "prenoms");
        this.sexe = Objects.requireNonNullElse(sexe, Sexe.NON_RENSEIGNE);
        this.dateNaissance = dateNaissance;
        this.telephone = normaliserNullable(telephone);
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        if (this.telephone == null && this.email == null) {
            throw new IllegalArgumentException("Un téléphone ou un e-mail est obligatoire");
        }
        this.motDePasseHash = texteObligatoire(motDePasseHash, "motDePasseHash");
        this.languePreferee = texteObligatoire(languePreferee, "languePreferee").toLowerCase(Locale.ROOT);
        this.statutCompte = StatutCompte.EN_ATTENTE;
    }

    public void modifierProfil(String nom, String prenoms, String telephone, String email, String languePreferee) {
        this.nom = texteObligatoire(nom, "nom");
        this.prenoms = texteObligatoire(prenoms, "prenoms");
        this.telephone = normaliserNullable(telephone);
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        if (this.telephone == null && this.email == null) {
            throw new IllegalArgumentException("Un téléphone ou un e-mail est obligatoire");
        }
        this.languePreferee = texteObligatoire(languePreferee, "languePreferee").toLowerCase(Locale.ROOT);
    }

    public void changerMotDePasse(String nouveauMotDePasseHash) {
        this.motDePasseHash = texteObligatoire(nouveauMotDePasseHash, "nouveauMotDePasseHash");
        this.sessionVersion++;
    }

    public void enregistrerConnexion(Instant dateConnexion) {
        this.derniereConnexion = Objects.requireNonNull(dateConnexion, "dateConnexion");
    }

    public void activerCompte() {
        this.statutCompte = StatutCompte.ACTIF;
    }

    public void suspendreCompte() {
        this.statutCompte = StatutCompte.SUSPENDU;
    }

    public void desactiverCompte() {
        this.statutCompte = StatutCompte.DESACTIVE;
    }

    protected static String texteObligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }

    protected static String normaliserNullable(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
