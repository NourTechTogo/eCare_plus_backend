package com.nourtech.ecareplus.care.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(name="reponse_prise_operation",uniqueConstraints=@UniqueConstraint(columnNames={"acteur_id","source_ref"}))
public class ReponsePriseOperation extends AbstractEntity {
    @Column(nullable=false) private UUID acteurId;
    @Column(nullable=false,length=128) private String sourceRef;
    @Column(nullable=false,columnDefinition="text") private String requete;
    @Column(nullable=false,columnDefinition="text") private String resultat;
    public ReponsePriseOperation(UUID acteur,String ref,String requete,String resultat){acteurId=acteur;sourceRef=ref;this.requete=requete;this.resultat=resultat;}
}
