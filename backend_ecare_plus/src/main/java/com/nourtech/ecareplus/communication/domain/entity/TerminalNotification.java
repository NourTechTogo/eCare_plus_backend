package com.nourtech.ecareplus.communication.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(name="terminal_notification",uniqueConstraints=@UniqueConstraint(columnNames="installation_id"))
public class TerminalNotification extends AbstractEntity {
    @Column(nullable=false,length=128) private String installationId;
    @Column(nullable=false) private UUID utilisateurId;
    @Column(nullable=false,length=20) private String plateforme;
    @Column(nullable=false,length=4096) private String jetonPush;
    @Column(nullable=false,length=64) private String sessionRef;
    @Column(nullable=false) private long sessionVersion;
    @Column(nullable=false) private boolean actif;
    public TerminalNotification(String installation){installationId=installation;}
    public void associer(UUID utilisateur,String plateforme,String jeton,String session,long version){utilisateurId=utilisateur;this.plateforme=plateforme;jetonPush=jeton;sessionRef=session;sessionVersion=version;actif=true;}
    public void desactiver(){actif=false;}
}
