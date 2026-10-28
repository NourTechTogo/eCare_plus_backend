package com.nourtech.ecareplus;

import com.nourtech.ecareplus.care.application.service.ExportConsultationPdf;
import com.nourtech.ecareplus.mobile.api.dto.SyntheseConsultationVue;
import com.nourtech.ecareplus.mobile.api.dto.SyntheseConsultationVue.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class ExportConsultationPdfTests {
    final ExportConsultationPdf pdf=new ExportConsultationPdf();
    SyntheseConsultationVue exemple(List<SerieMesureVue> series,List<String> notes){
        return new SyntheseConsultationVue(UUID.fromString("a0000000-0000-0000-0000-000000000001"),1,
            new PatientVue(UUID.randomUUID(),"Fictif","Camille",LocalDate.of(1980,3,14)),LocalDate.of(2026,10,20),LocalDate.of(2026,10,22),"Africa/Lome",Instant.parse("2026-10-23T08:00:00Z"),3,0,series,List.of(),List.of(),List.of(),List.of(),notes);
    }
    SerieMesureVue serie(int nombre){
        var points=new ArrayList<PointMesureVue>();
        for(int i=0;i<nombre;i++)points.add(new PointMesureVue(UUID.randomUUID(),Instant.parse("2026-10-20T08:00:00Z").plusSeconds(i*60L),new BigDecimal("81.25"),"Après le repas","PATIENT"));
        return new SerieMesureVue("POIDS","PRINCIPALE","kg",nombre,new BigDecimal("81.25"),new BigDecimal("81.25"),new BigDecimal("81.25"),new BigDecimal("81.25"),points.getLast().dateMesure(),0,points);
    }
    @Test void documentLisibleAvecGraphiqueEtToutesLesValeurs()throws Exception{
        var constante=serie(3);
        var points=new ArrayList<PointMesureVue>();
        for(int i=0;i<3;i++){var p=constante.points().get(i);points.add(new PointMesureVue(p.mesureId(),p.dateMesure().plusSeconds(i*86400L),p.valeur().add(BigDecimal.valueOf(i)),p.contexte(),p.source()));}
        var variable=new SerieMesureVue("POIDS","PRINCIPALE","kg",3,new BigDecimal("81.25"),new BigDecimal("83.25"),new BigDecimal("82.25"),new BigDecimal("83.25"),points.getLast().dateMesure(),0,points);
        var bytes=pdf.rendre(exemple(List.of(variable),List.of("Synthèse descriptive, sans diagnostic ni recommandation automatique.","Les jours sans saisie ne constituent pas un oubli de traitement.")));
        try(var doc=PDDocument.load(bytes)){
            var texte=new PDFTextStripper().getText(doc);
            assertThat(texte).contains("Camille Fictif","81.25","Africa/Lome","Annexe","Après le repas","Document personnel confidentiel");
            assertThat(texte.split("Référence mesure :",-1).length-1).isEqualTo(3);
            var parser=new org.apache.pdfbox.pdfparser.PDFStreamParser(doc.getPage(0));parser.parse();
            assertThat(parser.getTokens().stream().filter(t->t instanceof org.apache.pdfbox.contentstream.operator.Operator op && op.getName().equals("re")).count()).isEqualTo(3);
            assertThat(doc.getNumberOfPages()).isBetween(2,4);
            if(Boolean.getBoolean("ecare.pdf.exemple")){
                var dir=Path.of("../../output/pdf");Files.createDirectories(dir);Files.write(dir.resolve("ecare-consultation-exemple-fictif.pdf"),bytes);
                var renderer=new PDFRenderer(doc);
                for(int i=0;i<doc.getNumberOfPages();i++)ImageIO.write(renderer.renderImageWithDPI(i,110),"PNG",dir.resolve("apercu-"+(i+1)+".png").toFile());
            }
        }
    }
    @Test void paginationNePerdPasLesValeursEtGereUnicodeIndisponible()throws Exception{
        var s=serie(160);var bytes=pdf.rendre(exemple(List.of(s),List.of("Texte 😀 avec accents : é è à ç œ et identifiant "+"x".repeat(1500))));
        try(var doc=PDDocument.load(bytes)){
            String texte=new PDFTextStripper().getText(doc);
            assertThat(texte).contains("[U+1F600]","Certains caractères","é è à ç œ",s.points().getLast().mesureId().toString());
            assertThat(texte.split("Référence mesure :",-1).length-1).isEqualTo(160);
            assertThat(doc.getNumberOfPages()).isGreaterThan(5);
        }
    }
    @Test void rapportVideResteExplicite()throws Exception{
        try(var doc=PDDocument.load(pdf.rendre(exemple(List.of(),List.of())))){
            assertThat(new PDFTextStripper().getText(doc)).contains("Aucune mesure","Aucune réponse","Aucun traitement","Aucune alerte","Aucune date");
        }
    }
    @Test void tropDePagesRefuseLeDocumentEntier(){
        assertThatThrownBy(()->pdf.rendre(exemple(List.of(),Collections.nCopies(20000,"Texte fictif long occupant volontairement les pages de ce document de test."))))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("période plus courte");
    }
    @Test void toutesLesRubriquesUtilisentLeContenuEnregistre()throws Exception{
        var base=exemple(List.of(),List.of());var instant=base.genereLe();
        var horaire=new HoraireVue(UUID.randomUUID(),"Exemple injectable","10","mg",BigDecimal.ONE,"dose",LocalTime.of(8,0),"Africa/Lome",Set.of(1,3,5),true);
        var traitement=new TraitementVue(UUID.randomUUID(),"Libellé fictif",base.du(),null,"ACTIF",true,List.of(horaire));
        var reponse=new com.nourtech.ecareplus.care.application.service.PlanningTraitementService.OccurrenceVue(UUID.randomUUID(),traitement.id(),horaire.id(),instant,null,BigDecimal.ONE,"dose","Exemple injectable","10","mg","Solution injectable","Sous-cutanée","PRISE",false,1,"Africa/Lome",instant,instant);
        var regle=new com.nourtech.ecareplus.alert.api.dto.response.RegleAppliqueeVue(2L,"Règle fictive",com.nourtech.ecareplus.care.domain.enumeration.TypeMesure.POIDS,"SUPERIEUR",BigDecimal.TEN,null,1,24,com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.ELEVE,"PRINCIPALE","kg",null,instant,UUID.randomUUID(),"Source fictive",true);
        var alerte=new AlerteVue(UUID.randomUUID(),instant,"AUTOMATIQUE","Alerte de test","HAUTE","OUVERTE",regle);
        var date=new RendezVousVue(UUID.randomUUID(),"Consultation fictive",instant,"Africa/Lome","ANNULE","Lieu fictif","Nom fictif");
        var complet=new SyntheseConsultationVue(base.id(),1,base.patient(),base.du(),base.au(),base.fuseau(),instant,0,3,List.of(),List.of(traitement),List.of(reponse),List.of(alerte),List.of(date),List.of());
        try(var doc=PDDocument.load(pdf.rendre(complet))){
            assertThat(new PDFTextStripper().getText(doc)).contains("Libellé fictif","Solution injectable","Sous-cutanée","Règle fictive","Source fictive","Consultation fictive","ANNULE","Lieu fictif");
        }
    }
}
