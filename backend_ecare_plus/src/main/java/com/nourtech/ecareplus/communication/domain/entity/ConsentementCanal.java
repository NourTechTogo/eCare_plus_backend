package com.nourtech.ecareplus.communication.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class ConsentementCanal extends AbstractEntity {
    @Column(nullable=false) private UUID utilisateurId;
    @Column(nullable=false,length=20) private String canal="WHATSAPP";
    @Column(nullable=false) private boolean accepte;
    @Column(length=80) private String versionDocument;
    public ConsentementCanal(UUID utilisateur,boolean accepte,String version){utilisateurId=utilisateur;this.accepte=accepte;versionDocument=version;}
}
