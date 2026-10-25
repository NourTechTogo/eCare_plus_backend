package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;

@Getter
@Entity
@Table(name = "prise_medicament", indexes = {
        @Index(name = "idx_prise_traitement", columnList = "traitement_id"),
        @Index(name = "idx_prise_medicament", columnList = "medicament_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriseMedicament extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "traitement_id", nullable = false)
    private Traitement traitement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicament_id", nullable = false)
    private Medicament medicament;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantite;

    @Column(nullable = false, length = 20)
    private String unite;

    @Column(nullable = false, length = 100)
    private String frequence;

    @Column(nullable = false)
    private LocalTime heurePrevue;

    @Column(length = 50)
    private String momentPrise;

    @Column(length = 100)
    private String voieAdministration;

    @Column(columnDefinition = "text")
    private String consigne;

    @Column(nullable = false)
    private boolean actif;

    @Column(nullable=false,length=80) private String fuseau = "UTC";
    @Column(length=20) private String joursSemaine;

    public void configurerPlanning(String fuseau, java.util.Set<Integer> jours) {
        java.time.ZoneId.of(fuseau);
        if(jours==null||jours.isEmpty()||jours.stream().anyMatch(j->j==null||j<1||j>7))throw new IllegalArgumentException("Jours de semaine invalides");
        this.fuseau=fuseau;this.joursSemaine=jours.stream().sorted().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    public java.util.Set<Integer> joursPlanifies() {
        if(joursSemaine!=null)return java.util.Arrays.stream(joursSemaine.split(",")).map(Integer::valueOf).collect(java.util.stream.Collectors.toSet());
        if(java.util.Set.of("QUOTIDIEN","QUOTIDIENNE","TOUS_LES_JOURS").contains(frequence.toUpperCase(java.util.Locale.ROOT)))return java.util.Set.of(1,2,3,4,5,6,7);
        return java.util.Set.of(); // Legacy free text is not a sufficiently precise prescription.
    }

    public PriseMedicament(
            Traitement traitement,
            Medicament medicament,
            BigDecimal quantite,
            String unite,
            String frequence,
            LocalTime heurePrevue,
            String momentPrise,
            String voieAdministration,
            String consigne
    ) {
        this.traitement = Objects.requireNonNull(traitement, "traitement");
        this.medicament = Objects.requireNonNull(medicament, "medicament");
        this.quantite = Objects.requireNonNull(quantite, "quantite");
        if (quantite.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La quantité doit être strictement positive");
        }
        this.unite = obligatoire(unite, "unite");
        this.frequence = obligatoire(frequence, "frequence");
        this.heurePrevue = Objects.requireNonNull(heurePrevue, "heurePrevue").truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.momentPrise = normaliser(momentPrise);
        this.voieAdministration = normaliser(voieAdministration);
        this.consigne = normaliser(consigne);
        this.actif = true;
    }

    public void modifierHoraire(LocalTime heurePrevue, String frequence, String momentPrise) {
        this.heurePrevue = Objects.requireNonNull(heurePrevue, "heurePrevue").truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        this.frequence = obligatoire(frequence, "frequence");
        this.momentPrise = normaliser(momentPrise);
    }

    public void activer() {
        if (actif) {
            throw new IllegalStateException("La prise est déjà active");
        }
        this.actif = true;
    }

    public void desactiver() {
        if (!actif) {
            throw new IllegalStateException("La prise est déjà inactive");
        }
        this.actif = false;
    }

    private static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }

    private static String normaliser(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
