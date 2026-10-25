package com.nourtech.ecareplus.sync.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"utilisateur_id","identifiant_appareil","type_operation","identifiant_local"}))
public class OperationSynchronisation extends AbstractEntity {
    @Column(nullable=false) private UUID utilisateurId;
    @Column(nullable=false,length=128) private String identifiantAppareil;
    @Column(nullable=false,length=40) private String typeOperation;
    @Column(nullable=false,length=128) private String identifiantLocal;
    @Column(nullable=false,length=64) private String empreinte;
    @Column(nullable=false,columnDefinition="text") private String resultat;
    public OperationSynchronisation(UUID utilisateur,String appareil,String type,String local,String hash,String resultat){utilisateurId=utilisateur;identifiantAppareil=appareil;typeOperation=type;identifiantLocal=local;empreinte=hash;this.resultat=resultat;}
}
