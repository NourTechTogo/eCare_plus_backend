package com.nourtech.ecareplus.calling.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import com.nourtech.ecareplus.messaging.domain.entity.Conversation;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"initiateur_id","source_ref"}))
public class SessionAppel extends AbstractEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Conversation conversation;
    @Column(nullable=false) private UUID initiateurId;
    @Column(nullable=false) private UUID destinataireId;
    @Column(nullable=false,length=20) private String type;
    @Column(nullable=false,length=30) private String statut="PREPARATION";
    @Column(nullable=false,unique=true,length=80) private String salle="ecare-"+UUID.randomUUID();
    @Column(nullable=false,length=128) private String sourceRef;
    @Column(nullable=false) private Instant expireLe=Instant.now().plusSeconds(60);
    private Instant debutLe;
    private Instant finLe;
    private Long dureeSecondes;
    @Column(nullable=false) private boolean fermetureConfirmee;
    @Column(nullable=false) private boolean initiateurConnecte;
    @Column(nullable=false) private boolean destinataireConnecte;
    public SessionAppel(Conversation conversation,UUID initiateur,UUID destinataire,String type,String ref){this.conversation=conversation;initiateurId=initiateur;destinataireId=destinataire;this.type=type;sourceRef=ref;}
    public void pret(){if(!statut.equals("PREPARATION"))throw new IllegalStateException("Appel déjà préparé");statut="EN_ATTENTE";expireLe=Instant.now().plusSeconds(60);}
    public void accepter(){if(statut.equals("ACCEPTE"))return;if(!statut.equals("EN_ATTENTE"))throw new IllegalStateException("Appel non disponible");statut="ACCEPTE";}
    public boolean actif(){return java.util.Set.of("PREPARATION","EN_ATTENTE","ACCEPTE").contains(statut);}
    public void terminer(String etat){if(!actif())return;statut=etat;finLe=Instant.now();if(debutLe!=null)dureeSecondes=Math.max(0,Duration.between(debutLe,finLe).toSeconds());}
    public void fermer(){fermetureConfirmee=true;}
    public void participant(UUID utilisateur,boolean present){if(!statut.equals("ACCEPTE"))return;if(initiateurId.equals(utilisateur))initiateurConnecte=present;else if(destinataireId.equals(utilisateur))destinataireConnecte=present;else throw new IllegalArgumentException("Participant inconnu");if(initiateurConnecte&&destinataireConnecte&&debutLe==null)debutLe=Instant.now();}
}
