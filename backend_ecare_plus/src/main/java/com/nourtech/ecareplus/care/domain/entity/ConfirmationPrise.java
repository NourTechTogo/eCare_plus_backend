package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.care.domain.enumeration.StatutPrise;
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

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(name = "confirmation_prise", uniqueConstraints=@jakarta.persistence.UniqueConstraint(name="uq_confirmation_initiale",columnNames={"prise_medicament_id","date_initiale"}), indexes = {
        @Index(name = "idx_confirmation_prise", columnList = "prise_medicament_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfirmationPrise extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prise_medicament_id", nullable = false)
    private PriseMedicament priseMedicament;

    @Column(nullable = false)
    private Instant datePrevue;

    @Column(nullable=false,updatable=false) private Instant dateInitiale;
    @Column(nullable=false) private boolean annulee;
    @Column(precision=10,scale=3) private java.math.BigDecimal quantitePrevue;
    @Column(length=20) private String unitePrevue;
    private Instant dateActionClient;
    @Column(length=150) private String medicamentNom;
    @Column(length=40) private String medicamentDosage;
    @Column(length=20) private String medicamentUniteDosage;
    @Column(length=60) private String medicamentForme;
    @Column(length=100) private String voiePrevue;
    @Column(length=80) private String fuseauPrevu;

    public void annulerOccurrence(){if(statut==StatutPrise.ATTENTE||statut==StatutPrise.REPORTEE)annulee=true;}
    public void retablirOccurrence(){if(statut==StatutPrise.ATTENTE||statut==StatutPrise.REPORTEE)annulee=false;}
    public void appliquerInstantane(RevisionPlanningPrise.Instantane s){quantitePrevue=s.quantite();unitePrevue=s.unite();medicamentNom=s.nom();medicamentDosage=s.dosage();medicamentUniteDosage=s.uniteDosage();medicamentForme=s.forme();voiePrevue=s.voie();fuseauPrevu=s.fuseau();}
    public void actionClient(Instant date){dateActionClient=date.truncatedTo(java.time.temporal.ChronoUnit.MICROS);}

    private Instant dateConfirmation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutPrise statut;

    @Column(columnDefinition = "text")
    private String commentaire;

    public ConfirmationPrise(PriseMedicament priseMedicament, Instant datePrevue) {
        this.priseMedicament = Objects.requireNonNull(priseMedicament, "priseMedicament");
        this.datePrevue = Objects.requireNonNull(datePrevue, "datePrevue").truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.dateInitiale = this.datePrevue;
        this.quantitePrevue=priseMedicament.getQuantite();this.unitePrevue=priseMedicament.getUnite();
        this.medicamentNom=priseMedicament.getMedicament().getNomCommercial();this.medicamentDosage=priseMedicament.getMedicament().getDosage();this.medicamentUniteDosage=priseMedicament.getMedicament().getUniteDosage();this.medicamentForme=priseMedicament.getMedicament().getForme();this.voiePrevue=priseMedicament.getVoieAdministration();this.fuseauPrevu=priseMedicament.getFuseau();
        this.statut = StatutPrise.ATTENTE;
    }

    public void confirmer(String commentaire, Instant maintenant) {
        verifierEnAttente();
        this.statut = StatutPrise.PRISE;
        this.dateConfirmation = Objects.requireNonNull(maintenant, "maintenant").truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.commentaire = normaliser(commentaire);
    }

    public void signalerOubli(String commentaire) {
        verifierEnAttente();
        this.statut = StatutPrise.OUBLIEE;
        this.commentaire = normaliser(commentaire);
    }

    public void reporter(Instant nouvelleDatePrevue) {
        verifierEnAttente();
        Objects.requireNonNull(nouvelleDatePrevue, "nouvelleDatePrevue");
        Instant normalisee=nouvelleDatePrevue.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if (!normalisee.isAfter(datePrevue))
            throw new IllegalArgumentException("La nouvelle date doit être postérieure à la date prévue");
        this.datePrevue = normalisee;
        this.statut = StatutPrise.REPORTEE;
    }

    private void verifierEnAttente() {
        if(annulee)throw new IllegalStateException("Occurrence annulée");
        if (statut != StatutPrise.ATTENTE && statut != StatutPrise.REPORTEE) {
            throw new IllegalStateException("La confirmation a déjà été traitée");
        }
    }

    private static String normaliser(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
