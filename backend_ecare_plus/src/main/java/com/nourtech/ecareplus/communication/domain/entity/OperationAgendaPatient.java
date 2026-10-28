package com.nourtech.ecareplus.communication.domain.entity;

import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(name="operation_agenda_patient",uniqueConstraints=@UniqueConstraint(columnNames={"patient_id","source_ref"}))
public class OperationAgendaPatient extends AbstractEntity {
    @Column(nullable=false) private UUID patientId;
    @Column(nullable=false) private UUID sourceRef;
    @Column(nullable=false,length=64) private String empreinte;
    @Column(nullable=false,columnDefinition="text") private String resultat;
    public OperationAgendaPatient(UUID patient,UUID source,String empreinte,String resultat) {
        this.patientId=patient;this.sourceRef=source;this.empreinte=empreinte;this.resultat=resultat;
    }
}
