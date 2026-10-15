package com.nourtech.ecareplus.consent.domain.entity;

import com.nourtech.ecareplus.consent.domain.enumeration.StatutConsentement;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
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
@Table(name = "consentement", indexes = {
        @Index(name = "idx_consentement_patient_statut", columnList = "patient_id,statut")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consentement extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, length = 80)
    private String typeConsentement;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Instant dateAcceptation;

    private Instant dateRetrait;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutConsentement statut;

    @Column(nullable = false, length = 40)
    private String versionDocument;

    @Column(nullable = false, length = 45)
    private String adresseIp;

    public Consentement(
            Patient patient,
            String typeConsentement,
            String description,
            String versionDocument,
            String adresseIp,
            Instant dateAcceptation
    ) {
        this.patient = Objects.requireNonNull(patient, "patient");
        this.typeConsentement = obligatoire(typeConsentement, "typeConsentement");
        this.description = obligatoire(description, "description");
        this.versionDocument = obligatoire(versionDocument, "versionDocument");
        this.adresseIp = obligatoire(adresseIp, "adresseIp");
        this.dateAcceptation = Objects.requireNonNull(dateAcceptation, "dateAcceptation");
        this.statut = StatutConsentement.ACCORDE;
    }

    public void retirer(Instant dateRetrait) {
        if (statut != StatutConsentement.ACCORDE) {
            throw new IllegalStateException("Seul un consentement accordé peut être retiré");
        }
        this.dateRetrait = Objects.requireNonNull(dateRetrait, "dateRetrait");
        if (dateRetrait.isBefore(dateAcceptation)) {
            throw new IllegalArgumentException("Le retrait ne peut pas précéder l'acceptation");
        }
        this.statut = StatutConsentement.RETIRE;
    }

    public boolean estValideA(Instant instant) {
        return statut == StatutConsentement.ACCORDE
                && !dateAcceptation.isAfter(instant)
                && (dateRetrait == null || dateRetrait.isAfter(instant));
    }

    private static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }
}
