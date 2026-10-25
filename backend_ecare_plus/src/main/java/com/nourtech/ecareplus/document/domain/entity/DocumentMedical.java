package com.nourtech.ecareplus.document.domain.entity;

import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;

@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(name="document_medical",uniqueConstraints=@UniqueConstraint(name="uq_document_patient_source",columnNames={"patient_id","source_ref"}))
public class DocumentMedical extends AbstractEntity {
    @Column(nullable=false) private UUID patientId;
    @Column(nullable=false) private UUID auteurId;
    @Column(nullable=false,length=40) private String typeDocument;
    @Column(nullable=false,length=200) private String titre;
    @Column(nullable=false) private LocalDate dateDocument;
    @Column(nullable=false,length=255) private String nomOriginal;
    @Column(nullable=false,length=60) private String typeMime;
    @Column(nullable=false) private long tailleOctets;
    @Column(nullable=false,length=80) private String cleStockage;
    @Column(nullable=false,length=128) private String sourceRef;
    @Column(nullable=false,length=64) private String empreinte;
    private Instant supprimeLe;
    @Column(nullable=false) private boolean fichierSupprime;
    public DocumentMedical(UUID patient,UUID auteur,String type,String titre,LocalDate date,String nom,String mime,long taille,String cle,String source,String empreinte){
        patientId=patient;auteurId=auteur;typeDocument=type;this.titre=titre;dateDocument=date;nomOriginal=nom;typeMime=mime;tailleOctets=taille;cleStockage=cle;sourceRef=source;this.empreinte=empreinte;
    }
    public void retirer(){if(supprimeLe==null)supprimeLe=Instant.now();}
    public void fichierSupprime(){fichierSupprime=true;}
}
