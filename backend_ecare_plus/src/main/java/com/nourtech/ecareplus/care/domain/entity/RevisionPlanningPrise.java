package com.nourtech.ecareplus.care.domain.entity;
import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
@Table(indexes=@Index(name="idx_revision_prise",columnList="prise_id,valide_depuis"))
public class RevisionPlanningPrise extends AbstractEntity {
    @Column(nullable=false) private UUID priseId;
    @Column(nullable=false) private Instant valideDepuis;
    private Instant valideJusqua;
    @Column(nullable=false,columnDefinition="text") private String contenu;
    public RevisionPlanningPrise(UUID prise,Instant depuis,String contenu){priseId=prise;valideDepuis=depuis;this.contenu=contenu;}
    public void clore(Instant jusqua){valideJusqua=jusqua;}
    public void definirInitial(Instant depuis,String contenu){if(valideJusqua!=null)throw new IllegalStateException("Révision initiale déjà close");valideDepuis=depuis;this.contenu=contenu;}
    public boolean applicable(Instant date){return !date.isBefore(valideDepuis)&&(valideJusqua==null||date.isBefore(valideJusqua));}
    public record Instantane(LocalDate dateDebut,LocalDate dateFin,String statutTraitement,boolean actif,LocalTime heurePrevue,String fuseau,Set<Integer> joursSemaine,BigDecimal quantite,String unite,String nom,String dosage,String uniteDosage,String forme,String voie){
        public static Instantane depuis(PriseMedicament p){var t=p.getTraitement();var m=p.getMedicament();return new Instantane(t.getDateDebut(),t.getDateFin(),t.getStatut().name(),p.isActif(),p.getHeurePrevue(),p.getFuseau(),p.joursPlanifies(),p.getQuantite(),p.getUnite(),m.getNomCommercial(),m.getDosage(),m.getUniteDosage(),m.getForme(),p.getVoieAdministration());}
        public boolean prevu(LocalDate jour){return actif&&statutTraitement.equals("ACTIF")&&!jour.isBefore(dateDebut)&&(dateFin==null||!jour.isAfter(dateFin))&&joursSemaine.contains(jour.getDayOfWeek().getValue());}
        public Instant date(LocalDate jour){return jour.atTime(heurePrevue).atZone(ZoneId.of(fuseau)).toInstant();}
    }
}
