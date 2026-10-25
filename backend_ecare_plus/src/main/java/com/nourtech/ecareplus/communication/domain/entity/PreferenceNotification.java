package com.nourtech.ecareplus.communication.domain.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class PreferenceNotification {
    @Id private UUID utilisateurId;
    @Version private long version;
    @Column(nullable=false) private boolean pushActif=true;
    @Column(nullable=false) private boolean whatsappActif;
    public PreferenceNotification(UUID utilisateur){utilisateurId=utilisateur;}
    public void modifier(Boolean push,Boolean whatsapp){if(push!=null)pushActif=push;if(whatsapp!=null)whatsappActif=whatsapp;}
}
