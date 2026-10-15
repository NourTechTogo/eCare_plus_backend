package com.nourtech.ecareplus.sync.domain.entity;

import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import com.nourtech.ecareplus.sync.domain.enumeration.StatutSynchronisation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Getter
@Entity
@Table(name = "element_synchronisation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sync_utilisateur_appareil_local",
                columnNames = {"utilisateur_id", "identifiant_appareil", "identifiant_local"}
        ),
        indexes = @Index(name = "idx_sync_statut", columnList = "statut,date_creation_locale"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ElementSynchronisation extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 80)
    private String typeRessource;

    @Column(nullable = false, length = 100)
    private String identifiantAppareil;

    @Column(nullable = false, length = 100)
    private String identifiantLocal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> contenuLocal;

    @Column(nullable = false)
    private Instant dateCreationLocale;

    @Column(nullable = false)
    private int nombreTentatives;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSynchronisation statut;

    @Column(columnDefinition = "text")
    private String messageErreur;

    public ElementSynchronisation(
            Utilisateur utilisateur,
            String typeRessource,
            String identifiantAppareil,
            String identifiantLocal,
            Map<String, Object> contenuLocal,
            Instant dateCreationLocale
    ) {
        this.utilisateur = Objects.requireNonNull(utilisateur, "utilisateur");
        this.typeRessource = obligatoire(typeRessource, "typeRessource");
        this.identifiantAppareil = obligatoire(identifiantAppareil, "identifiantAppareil");
        this.identifiantLocal = obligatoire(identifiantLocal, "identifiantLocal");
        this.contenuLocal = Map.copyOf(Objects.requireNonNull(contenuLocal, "contenuLocal"));
        this.dateCreationLocale = Objects.requireNonNull(dateCreationLocale, "dateCreationLocale");
        this.statut = StatutSynchronisation.RECU;
    }

    public void commencerTraitement() {
        if (statut == StatutSynchronisation.SYNCHRONISE) {
            throw new IllegalStateException("L'élément est déjà synchronisé");
        }
        nombreTentatives++;
        statut = StatutSynchronisation.EN_COURS;
        messageErreur = null;
    }

    public void marquerCommeSynchronise() {
        if (statut != StatutSynchronisation.EN_COURS) {
            throw new IllegalStateException("L'élément doit être en cours de traitement");
        }
        statut = StatutSynchronisation.SYNCHRONISE;
        messageErreur = null;
    }

    public void signalerEchec(String messageErreur) {
        if (statut != StatutSynchronisation.EN_COURS) {
            throw new IllegalStateException("L'élément doit être en cours de traitement");
        }
        this.statut = StatutSynchronisation.EN_ECHEC;
        this.messageErreur = obligatoire(messageErreur, "messageErreur");
    }

    private static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }
}
