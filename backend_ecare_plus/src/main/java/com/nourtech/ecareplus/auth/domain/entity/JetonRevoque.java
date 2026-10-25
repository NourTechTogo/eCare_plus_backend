package com.nourtech.ecareplus.auth.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="jeton_revoque") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class JetonRevoque extends AbstractEntity {
    @Column(nullable=false,unique=true,length=64) private String empreinte;
    @Column(nullable=false) private Instant expireLe;
    public JetonRevoque(String empreinte,Instant expireLe){this.empreinte=empreinte;this.expireLe=expireLe;}
}
