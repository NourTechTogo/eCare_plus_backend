package com.nourtech.ecareplus.messaging.domain.entity;
import com.nourtech.ecareplus.identity.domain.entity.Utilisateur; import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant; import java.util.Objects;
@Getter @Entity @Table(name="message_conversation",uniqueConstraints=@UniqueConstraint(name="uk_message_source",columnNames={"conversation_id","expediteur_id","source_ref"})) @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class MessageConversation extends AbstractEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="conversation_id") private Conversation conversation;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="expediteur_id") private Utilisateur expediteur;
 @Column(nullable=false,length=2000) private String contenu; @Column(name="envoye_le",nullable=false) private Instant envoyeLe;
 @Column(name="source_ref") private java.util.UUID sourceRef;
 public void definirSource(java.util.UUID source){sourceRef=source;}
 @Column(name="lu_le") private Instant luLe;
 @OneToMany(mappedBy="message") private java.util.List<PieceJointe> piecesJointes=new java.util.ArrayList<>();
 public void ajouterPiece(PieceJointe piece){piecesJointes.add(piece);}
 public MessageConversation(Conversation c,Utilisateur e,String texte){conversation=Objects.requireNonNull(c);expediteur=Objects.requireNonNull(e);
  if(!c.membre(e))throw new IllegalArgumentException("L'expéditeur ne participe pas à la conversation");
  if(texte==null||texte.isBlank()||texte.trim().length()>2000)throw new IllegalArgumentException("Le message doit contenir entre 1 et 2000 caractères");
  contenu=texte.trim();envoyeLe=Instant.now();}
 public void lire(){if(luLe==null)luLe=Instant.now();}
}
