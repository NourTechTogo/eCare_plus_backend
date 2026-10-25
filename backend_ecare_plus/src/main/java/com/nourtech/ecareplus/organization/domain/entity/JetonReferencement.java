package com.nourtech.ecareplus.organization.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class JetonReferencement extends AbstractEntity {
    @Column(nullable=false) private UUID patientId;
    @Column(nullable=false,unique=true,length=64) private String empreinte;
    @Column(nullable=false) private Instant expireLe;
    private Instant consommeLe;
    @Column(nullable=false) private boolean revoque;
    public JetonReferencement(UUID patient,String hash,Instant expiration){patientId=patient;empreinte=hash;expireLe=expiration;}
    public boolean valide(){return !revoque&&consommeLe==null&&expireLe.isAfter(Instant.now());}
    public void revoquer(){revoque=true;}
    public void consommer(){if(!valide())throw new IllegalStateException("Jeton expiré, révoqué ou déjà utilisé");consommeLe=Instant.now();}
}
