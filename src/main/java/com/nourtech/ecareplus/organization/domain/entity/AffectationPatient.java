package com.nourtech.ecareplus.organization.domain.entity;

import com.nourtech.ecareplus.identity.domain.entity.Patient;
import com.nourtech.ecareplus.identity.domain.entity.ProfessionnelSante;
import com.nourtech.ecareplus.organization.domain.enumeration.RoleProfessionnel;
import com.nourtech.ecareplus.organization.domain.enumeration.StatutAffectation;
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

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Entity
@Table(name = "affectation_patient", indexes = {
        @Index(name = "idx_affectation_patient_statut", columnList = "patient_id,statut"),
        @Index(name = "idx_affectation_professionnel_statut", columnList = "professionnel_id,statut")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AffectationPatient extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professionnel_id", nullable = false)
    private ProfessionnelSante professionnel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "structure_id", nullable = false)
    private StructureSante structure;

    @Column(nullable = false)
    private LocalDate dateDebut;

    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RoleProfessionnel roleProfessionnel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutAffectation statut;

    @Column(nullable = false)
    private boolean professionnelPrincipal;

    public AffectationPatient(
            Patient patient,
            ProfessionnelSante professionnel,
            StructureSante structure,
            RoleProfessionnel roleProfessionnel,
            LocalDate dateDebut,
            boolean professionnelPrincipal
    ) {
        this.patient = Objects.requireNonNull(patient, "patient");
        this.professionnel = Objects.requireNonNull(professionnel, "professionnel");
        this.structure = Objects.requireNonNull(structure, "structure");
        if (!professionnel.getStructure().memeIdentiteQue(structure)) {
            throw new IllegalArgumentException("Le professionnel doit appartenir à la structure de l'affectation");
        }
        this.roleProfessionnel = Objects.requireNonNull(roleProfessionnel, "roleProfessionnel");
        this.dateDebut = Objects.requireNonNull(dateDebut, "dateDebut");
        this.professionnelPrincipal = professionnelPrincipal;
        this.statut = StatutAffectation.ACTIVE;
    }

    public void definirProfessionnelPrincipal(boolean principal) {
        verifierActive();
        this.professionnelPrincipal = principal;
    }

    public void terminer(LocalDate dateFin) {
        verifierActive();
        this.dateFin = Objects.requireNonNull(dateFin, "dateFin");
        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas précéder la date de début");
        }
        this.statut = StatutAffectation.TERMINEE;
        this.professionnelPrincipal = false;
    }

    private void verifierActive() {
        if (statut != StatutAffectation.ACTIVE) {
            throw new IllegalStateException("L'affectation n'est pas active");
        }
    }
}
