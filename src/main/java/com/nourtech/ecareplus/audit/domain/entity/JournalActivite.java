package com.nourtech.ecareplus.audit.domain.entity;

import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
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

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "journal_activite", indexes = {
        @Index(name = "idx_journal_utilisateur_date", columnList = "utilisateur_id,date_action"),
        @Index(name = "idx_journal_ressource", columnList = "ressource,ressource_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalActivite extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(nullable = false, length = 120)
    private String ressource;

    private UUID ressourceId;

    @Column(nullable = false)
    private Instant dateAction;

    @Column(length = 45)
    private String adresseIp;

    @Column(length = 300)
    private String appareil;

    @Column(nullable = false, length = 40)
    private String resultat;

    public JournalActivite(
            Utilisateur utilisateur,
            String action,
            String ressource,
            UUID ressourceId,
            String adresseIp,
            String appareil,
            String resultat
    ) {
        this.utilisateur = utilisateur;
        this.action = obligatoire(action, "action");
        this.ressource = obligatoire(ressource, "ressource");
        this.ressourceId = ressourceId;
        this.dateAction = Instant.now();
        this.adresseIp = nullable(adresseIp);
        this.appareil = nullable(appareil);
        this.resultat = obligatoire(resultat, "resultat");
    }

    private static String obligatoire(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " est obligatoire");
        }
        return valeur.trim();
    }

    private static String nullable(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
