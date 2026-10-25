package com.nourtech.ecareplus.communication.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(name="envoi_notification",uniqueConstraints=@UniqueConstraint(columnNames={"evenement_id","cible_id","canal"}))
public class EnvoiNotification extends AbstractEntity {
    @Column(nullable=false) private UUID evenementId;
    @Column(nullable=false) private UUID destinataireId;
    @Column(nullable=false) private UUID cibleId;
    private UUID patientId;
    @Column(nullable=false,length=20) private String canal;
    @Column(nullable=false,length=30) private String statut="EN_ATTENTE";
    @Column(nullable=false) private int tentatives;
    @Column(nullable=false) private Instant prochaineTentative=Instant.now();
    private Instant bailExpireLe;
    private UUID reservation;
    @Column(length=200) private String referenceFournisseur;
    @Column(length=80) private String dernierCodeErreur;
    public EnvoiNotification(UUID evenement,UUID destinataire,UUID cible,UUID patient,String canal){evenementId=evenement;destinataireId=destinataire;cibleId=cible;patientId=patient;this.canal=canal;}
    public boolean disponible(Instant date){return ((statut.equals("EN_ATTENTE")||statut.equals("A_REESSAYER"))&&!prochaineTentative.isAfter(date))||(statut.equals("EN_COURS")&&bailExpireLe!=null&&!bailExpireLe.isAfter(date));}
    public UUID reserver(){reservation=UUID.randomUUID();statut="EN_COURS";tentatives++;bailExpireLe=Instant.now().plusSeconds(60);return reservation;}
    public void annuler(){statut="ANNULE";reservation=null;bailExpireLe=null;}
    public void accepter(String reference,boolean livre){referenceFournisseur=reference;statut=livre?"LIVRE":"ACCEPTE";reservation=null;bailExpireLe=null;dernierCodeErreur=null;}
    public void echec(String code,int maximum,long delai){dernierCodeErreur=code;statut=tentatives>=maximum?"ECHEC":"A_REESSAYER";prochaineTentative=Instant.now().plusSeconds(delai);reservation=null;bailExpireLe=null;}
    public void livrer(String reference){if(!reference.equals(referenceFournisseur))throw new IllegalArgumentException("Référence de livraison incompatible");statut="LIVRE";}
    public void echecDefinitif(){if(!statut.equals("LIVRE")){statut="ECHEC";dernierCodeErreur="ECHEC_LIVRAISON";}}
}
