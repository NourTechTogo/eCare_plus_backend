package com.nourtech.ecareplus.care.domain.entity;

import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(name="prescription_operation",uniqueConstraints=@UniqueConstraint(columnNames={"acteur_id","patient_id","source_ref"}))
public class PrescriptionOperation extends AbstractEntity {
    @Column(nullable=false) private UUID acteurId;
    @Column(nullable=false) private UUID patientId;
    @Column(nullable=false,length=128) private String sourceRef;
    @Column(nullable=false,length=64) private String empreinte;
    @Column(nullable=false,columnDefinition="text") private String resultat;
    public PrescriptionOperation(UUID acteur,UUID patient,String source,String hash,String resultat){acteurId=acteur;patientId=patient;sourceRef=source;empreinte=hash;this.resultat=resultat;}
}
