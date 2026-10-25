package com.nourtech.ecareplus.calling.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class EvenementAppel extends AbstractEntity {
    @Column(nullable=false) private UUID appelId;
    @Column(nullable=false,unique=true,length=128) private String identifiantExterne;
    @Column(nullable=false,length=40) private String typeEvenement;
    public EvenementAppel(UUID appel,String identifiant,String type){appelId=appel;identifiantExterne=identifiant;typeEvenement=type;}
}
