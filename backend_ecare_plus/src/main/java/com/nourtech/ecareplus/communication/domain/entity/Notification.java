package com.nourtech.ecareplus.communication.domain.entity;

import com.nourtech.ecareplus.communication.domain.enumeration.CanalNotification;
import com.nourtech.ecareplus.communication.domain.enumeration.StatutEnvoiNotification;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
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
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_utilisateur", columnList = "utilisateur_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, length = 50)
    private String typeNotification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotification canal;

    private Instant dateEnvoi;

    private Instant dateLecture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutEnvoiNotification statutEnvoi;

    @Column(nullable = false)
    private boolean sensible;
    @Column(length=160) private String sourceRef;
    public void lierSource(String source){sourceRef=source;}
    @Column(length=40) private String typeRessource;
    private java.util.UUID ressourceId;
    public void rattacherRessource(String type,java.util.UUID id){if(id==null||!java.util.Set.of("ALERTE","CONVERSATION","TRAITEMENT","APPEL").contains(type))throw new IllegalArgumentException("Cible de notification invalide");typeRessource=type;ressourceId=id;}

    public Notification(
            Utilisateur utilisateur,
            String titre,
            String message,
            String typeNotification,
            CanalNotification canal,
            boolean sensible
    ) {
        this.utilisateur = Objects.requireNonNull(utilisateur, "utilisateur");
        this.titre = obligatoire(titre, "titre");
        this.message = obligatoire(message, "message");
        this.typeNotification = obligatoire(typeNotification, "typeNotification");
        this.canal = Objects.requireNonNull(canal, "canal");
        this.sensible = sensible;
        this.statutEnvoi = StatutEnvoiNotification.EN_ATTENTE;
    }

    public void envoyer(Instant maintenant) {
        if (statutEnvoi != StatutEnvoiNotification.EN_ATTENTE) {
            throw new IllegalStateException("Seule une notification en attente peut être envoyée");
        }
        this.statutEnvoi = StatutEnvoiNotification.ENVOYEE;
        this.dateEnvoi = Objects.requireNonNull(maintenant, "maintenant");
    }

    public void marquerEnEchec() {
        if (statutEnvoi != StatutEnvoiNotification.EN_ATTENTE) {
            throw new IllegalStateException("Seule une notification en attente peut échouer");
        }
        this.statutEnvoi = StatutEnvoiNotification.ECHEC;
    }

    public void reessayerEnvoi(Instant maintenant) {
        if (statutEnvoi != StatutEnvoiNotification.ECHEC) {
            throw new IllegalStateException("Seule une notification en échec peut être réessayée");
        }
        this.statutEnvoi = StatutEnvoiNotification.ENVOYEE;
        this.dateEnvoi = Objects.requireNonNull(maintenant, "maintenant");
    }

    public void marquerCommeLue(Instant maintenant) {
        if (statutEnvoi != StatutEnvoiNotification.ENVOYEE) {
            throw new IllegalStateException("Seule une notification envoyée peut être marquée comme lue");
        }
        this.statutEnvoi = StatutEnvoiNotification.LUE;
        this.dateLecture = Objects.requireNonNull(maintenant, "maintenant");
    }

    public void annuler() {
        if (statutEnvoi != StatutEnvoiNotification.EN_ATTENTE) {
            throw new IllegalStateException("Seule une notification en attente peut être annulée");
        }
        this.statutEnvoi = StatutEnvoiNotification.ANNULEE;
    }

    private static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }
}
