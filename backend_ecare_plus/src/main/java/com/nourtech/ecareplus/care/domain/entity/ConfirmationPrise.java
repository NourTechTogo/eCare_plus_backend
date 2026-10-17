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
@Table(name = "confirmation_prise", indexes = {
        @Index(name = "idx_confirmation_prise", columnList = "prise_medicament_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfirmationPrise extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prise_medicament_id", nullable = false)
    private PriseMedicament priseMedicament;

    @Column(nullable = false)
    private Instant datePrevue;

    private Instant dateConfirmation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutPrise statut;

    @Column(columnDefinition = "text")
    private String commentaire;

    public ConfirmationPrise(PriseMedicament priseMedicament, Instant datePrevue) {
        this.priseMedicament = Objects.requireNonNull(priseMedicament, "priseMedicament");
        this.datePrevue = Objects.requireNonNull(datePrevue, "datePrevue");
        this.statut = StatutPrise.ATTENTE;
    }

    public void confirmer(String commentaire, Instant maintenant) {
        verifierEnAttente();
        this.statut = StatutPrise.PRISE;
        this.dateConfirmation = Objects.requireNonNull(maintenant, "maintenant");
        this.commentaire = normaliser(commentaire);
    }

    public void signalerOubli(String commentaire) {
        verifierEnAttente();
        this.statut = StatutPrise.OUBLIEE;
        this.commentaire = normaliser(commentaire);
    }

    public void reporter(Instant nouvelleDatePrevue) {
        verifierEnAttente();
        this.datePrevue = Objects.requireNonNull(nouvelleDatePrevue, "nouvelleDatePrevue");
        this.statut = StatutPrise.REPORTEE;
    }

    private void verifierEnAttente() {
        if (statut != StatutPrise.ATTENTE) {
            throw new IllegalStateException("La confirmation a déjà été traitée");
        }
    }

    private static String normaliser(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
