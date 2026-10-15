package com.nourtech.ecareplus.auth.domain.entity;

import com.nourtech.ecareplus.identity.domain.entity.Utilisateur;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Getter @Entity @Table(name = "demande_reinitialisation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DemandeReinitialisation extends AbstractEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name="utilisateur_id", nullable=false)
    private Utilisateur utilisateur;
    @Column(name="otp_hash", nullable=false, length=100) private String otpHash;
    @Column(name="expire_le", nullable=false) private Instant expireLe;
    @Column(name="nombre_essais", nullable=false) private int nombreEssais;
    @Column(nullable=false) private boolean verifiee;
    @Column(nullable=false) private boolean utilisee;
    @Column(name="jeton_hash", length=100) private String jetonHash;

    public DemandeReinitialisation(Utilisateur utilisateur, String otpHash, Instant expireLe) {
        this.utilisateur = utilisateur; this.otpHash = otpHash; this.expireLe = expireLe;
    }
    public boolean active(Instant maintenant) { return !utilisee && !verifiee && nombreEssais < 5 && expireLe.isAfter(maintenant); }
    public void echec() { nombreEssais++; }
    public void verifier(String jetonHash) { this.verifiee = true; this.jetonHash = jetonHash; }
    public void utiliser() { this.utilisee = true; }
    public void invalider() { this.utilisee = true; }
}
