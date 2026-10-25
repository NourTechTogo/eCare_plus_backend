package com.nourtech.ecareplus.identity.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class DemandeEffacement extends AbstractEntity {
    @Column(nullable=false) private UUID utilisateurId;
    @Column(nullable=false,length=30) private String statut="EN_ATTENTE";
    @Column(length=1000) private String motif;
    public DemandeEffacement(UUID utilisateur,String motif){utilisateurId=utilisateur;this.motif=motif;}
}
