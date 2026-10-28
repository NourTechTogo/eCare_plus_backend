package com.nourtech.ecareplus.care.application.service;
import com.nourtech.ecareplus.care.domain.entity.MesureSante;
import com.nourtech.ecareplus.mobile.api.dto.SyntheseConsultationVue.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.*;
import java.util.*;
final class StatistiquesConsultation {
    private StatistiquesConsultation(){}
    static long joursAvecSaisie(List<MesureSante> mesures,ZoneId zone){return mesures.stream().map(m->m.getDateMesure().atZone(zone).toLocalDate()).distinct().count();}
    static List<SerieMesureVue> calculer(List<MesureSante> mesures,LocalDate du,LocalDate au,ZoneId zone){
        record Cle(String type,String composante,String unite){}
        var groupes=new LinkedHashMap<Cle,List<PointMesureVue>>();
        for(var m:mesures){
            groupes.computeIfAbsent(new Cle(m.getTypeMesure().name(),"PRINCIPALE",m.getUnite()),k->new ArrayList<>()).add(point(m,m.getValeurPrincipale()));
            if(m.getValeurSecondaire()!=null)groupes.computeIfAbsent(new Cle(m.getTypeMesure().name(),"DIASTOLIQUE",m.getUnite()),k->new ArrayList<>()).add(point(m,m.getValeurSecondaire()));
        }
        long jours=ChronoUnit.DAYS.between(du,au)+1;
        return groupes.entrySet().stream().map(e->{var points=e.getValue();var cle=e.getKey();
            BigDecimal min=points.stream().map(PointMesureVue::valeur).min(BigDecimal::compareTo).orElseThrow();
            BigDecimal max=points.stream().map(PointMesureVue::valeur).max(BigDecimal::compareTo).orElseThrow();
            BigDecimal moyenne=points.stream().map(PointMesureVue::valeur).reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(points.size()),2,RoundingMode.HALF_UP);
            var derniere=points.getLast();long saisis=points.stream().map(p->p.dateMesure().atZone(zone).toLocalDate()).distinct().count();
            return new SerieMesureVue(cle.type(),cle.composante(),cle.unite(),points.size(),min,max,moyenne,derniere.valeur(),derniere.dateMesure(),jours-saisis,List.copyOf(points));
        }).sorted(Comparator.comparing(SerieMesureVue::typeMesure).thenComparing(SerieMesureVue::composante).thenComparing(SerieMesureVue::unite)).toList();
    }
    private static PointMesureVue point(MesureSante m,BigDecimal valeur){return new PointMesureVue(m.getId(),m.getDateMesure(),valeur,m.getContexteMesure(),m.getSource().name());}
}
