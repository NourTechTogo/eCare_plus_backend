package com.nourtech.ecareplus.messaging.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"auteur_id","source_ref"}))
public class PieceJointe extends AbstractEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Conversation conversation;
    @ManyToOne(fetch=FetchType.LAZY) private MessageConversation message;
    @Column(nullable=false) private UUID auteurId;
    @Column(nullable=false,length=255) private String nomOriginal;
    @Column(nullable=false,length=60) private String typeMime;
    @Column(nullable=false) private long tailleOctets;
    @Column(nullable=false,length=80) private String cleStockage;
    @Column(nullable=false,length=128) private String sourceRef;
    @Column(nullable=false,length=64) private String empreinte;
    @Column(nullable=false) private boolean supprimee;
    public PieceJointe(Conversation c,UUID auteur,String nom,String mime,long taille,String cle,String ref,String hash){conversation=c;auteurId=auteur;nomOriginal=nom;typeMime=mime;tailleOctets=taille;cleStockage=cle;sourceRef=ref;empreinte=hash;}
    public void rattacher(MessageConversation m){if(message!=null)throw new IllegalStateException("Pièce déjà rattachée");message=m;}
    public void supprimer(){supprimee=true;}
}
