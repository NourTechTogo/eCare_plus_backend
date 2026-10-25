package com.nourtech.ecareplus.care.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class NoteCiblee extends AbstractEntity {
    @Column(nullable=false) private UUID auteurId;
    @Column(nullable=false) private UUID patientId;
    @Column(nullable=false,length=20) private String typeCible;
    @Column(nullable=false) private UUID cibleId;
    @Column(nullable=false,columnDefinition="text") private String contenu;
    @Column(nullable=false,length=20) private String visibilite;
    private Instant supprimeLe;
    public NoteCiblee(UUID auteur,UUID patient,String type,UUID cible,String texte,String visibilite){auteurId=auteur;patientId=patient;typeCible=type;cibleId=cible;modifier(texte,visibilite);}
    public void modifier(String texte,String visibilite){if(texte==null||texte.isBlank()||texte.length()>4000||!java.util.Set.of("PRIVEE","PARTAGEE").contains(java.util.Objects.requireNonNullElse(visibilite,"")))throw new IllegalArgumentException("Contenu ou visibilité invalide");contenu=texte.trim();this.visibilite=visibilite;}
    public void supprimer(){supprimeLe=Instant.now();}
}
