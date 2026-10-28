package com.nourtech.ecareplus.care.application.service;

import com.nourtech.ecareplus.mobile.api.dto.SyntheseConsultationVue;
import com.nourtech.ecareplus.mobile.api.dto.SyntheseConsultationVue.SerieMesureVue;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.*;
import java.math.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Pure rendering of the saved snapshot. No repository, URL, file path or live clinical query. */
@Service
public class ExportConsultationPdf {
    public static final int MAX_PAGES=300, MAX_BYTES=16*1024*1024;
    public byte[] rendre(SyntheseConsultationVue r) {
        try(var rendu=new Rendu(r)) { return rendu.creer(); }
        catch(IOException e) { throw new UncheckedIOException("Impossible de produire le PDF de consultation",e); }
    }

    private static final class Rendu implements AutoCloseable {
        final PDDocument document=new PDDocument();
        final PDType0Font regular,bold;
        final SyntheseConsultationVue rapport;
        final ZoneId zone;
        final DateTimeFormatter dates=DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
        static final Color BLEU=new Color(20,76,120), TEXTE=new Color(32,50,65), GRIS=new Color(86,104,120);
        static final float M=44, LARGEUR=PDRectangle.A4.getWidth()-2*M, BAS=55;
        PDPageContentStream flux; float y; boolean glyphesRemplaces;

        Rendu(SyntheseConsultationVue r) throws IOException {
            rapport=r;zone=ZoneId.of(r.fuseau());
            regular=police("Poppins-Regular.ttf");bold=police("Poppins-SemiBold.ttf");
        }
        PDType0Font police(String nom) throws IOException {
            try(var in=ExportConsultationPdf.class.getResourceAsStream("/pdf/fonts/"+nom)) {
                if(in==null)throw new IOException("Police PDF absente");
                return PDType0Font.load(document,in,true);
            }
        }
        byte[] creer() throws IOException {
            var info=document.getDocumentInformation();info.setTitle("eCare+ - Synthèse de consultation");info.setCreator("eCare+");
            nouvellePage();titre("Préparer ma consultation");
            ligne(rapport.patient().prenoms()+" "+rapport.patient().nom(),13,true);
            ligne("Naissance : "+val(rapport.patient().dateNaissance()),10,false);
            ligne("Du "+rapport.du()+" au "+rapport.au()+" | "+rapport.fuseau(),10,false);
            ligne("Synthèse figée le "+date(rapport.genereLe())+" | Référence : "+rapport.id(),9,false);
            espace(12);ligne(rapport.nombreMesures()+" mesures enregistrées - "+rapport.joursSansSaisie()+" jours sans saisie",12,true);
            rubrique("Repères de lecture");
            for(String avertissement:rapport.avertissements())ligne(avertissement,9,false);
            rubrique("Mesures et tendances descriptives");
            if(rapport.seriesMesures().isEmpty())ligne("Aucune mesure enregistrée pour cette période.",10,false);
            for(var s:rapport.seriesMesures()) {
                reserve(218);ligne(libelle(s.typeMesure())+" / "+libelle(s.composante())+" ("+s.unite()+")",12,true);
                ligne(s.nombre()+" valeurs | Moyenne : "+val(s.moyenne())+" | Min : "+val(s.minimum())+" | Max : "+val(s.maximum()),10,false);
                ligne("Dernière : "+val(s.derniereValeur())+" le "+date(s.derniereLe())+" | Jours sans saisie : "+s.joursSansSaisie(),9,false);
                graphique(s);
            }
            rubrique("Traitements enregistrés");
            if(rapport.traitements().isEmpty())ligne("Aucun traitement dans la période.",10,false);
            for(var t:rapport.traitements()) {
                ligne(t.libelle()+" | "+t.statutCourant()+" | "+(t.declarationPersonnelle()?"Déclaration personnelle":"Dossier de soins"),11,true);
                ligne("Du "+val(t.dateDebut())+" au "+val(t.dateFin())+" | Réf. "+t.id(),9,false);
                for(var h:t.horaires())ligne("- "+val(h.medicament())+" "+val(h.dosage())+" "+val(h.uniteDosage())+" | "+val(h.quantite())+" "+val(h.unite())+" à "+val(h.heurePrevue())+" ("+h.fuseau()+") | Jours ISO : "+new TreeSet<>(h.joursSemaine())+" | "+(h.actif()?"Actif":"Inactif"),9,false);
                espace(6);
            }
            rubrique("Réponses aux prises déjà enregistrées");
            if(rapport.reponsesPrises().isEmpty())ligne("Aucune réponse enregistrée. Aucun taux d'observance déduit.",10,false);
            for(var p:rapport.reponsesPrises()) {
                ligne(date(p.prevueLe())+" | "+val(p.medicament())+" "+val(p.dosage())+" "+val(p.uniteDosage())+" | "+val(p.quantite())+" "+val(p.unite())+" | "+p.statut(),10,false);
                ligne("Forme : "+val(p.forme())+" | Voie d'administration : "+val(p.voieAdministration()),9,false);
                ligne("Réf. "+p.id()+" | Report : "+date(p.reporteeAu())+" | Confirmation : "+date(p.dateConfirmation())+" | Annulée : "+p.annulee(),8,false);
            }
            rubrique("Alertes déjà enregistrées");
            if(rapport.alertes().isEmpty())ligne("Aucune alerte enregistrée pour cette période.",10,false);
            for(var a:rapport.alertes()) {
                ligne(date(a.dateEvenement())+" | "+a.titre()+" | "+a.niveauPriorite()+" | "+a.statut(),10,true);
                ligne("Origine : "+a.origine()+" | Réf. "+a.id(),8,false);
                var g=a.regleAppliquee();
                if(g==null)ligne("Capture de règle indisponible pour cette alerte historique.",9,false);
                else {
                    ligne("Règle capturée : "+val(g.nom())+" | Version : "+val(g.version())+" | "+val(g.typeMesure())+" / "+val(g.composante())+" | "+val(g.operateur())+" | Seuils : "+val(g.seuilMin())+" à "+val(g.seuilMax())+" "+val(g.unite()),9,false);
                    ligne("Contexte : "+val(g.contexteMesure())+" | Occurrences : "+val(g.nombreOccurrences())+" | Période : "+val(g.periodeObservation())+" | Priorité : "+val(g.niveauPriorite())+" | Active : "+val(g.actif()),8,false);
                    ligne("Validation : "+date(g.dateValidation())+" | Auteur : "+val(g.validateurId())+" | Source : "+val(g.sourceValidation()),8,false);
                }
                espace(6);
            }
            rubrique("Agenda personnel");
            if(rapport.rendezVous().isEmpty())ligne("Aucune date enregistrée pour cette période.",10,false);
            for(var rdv:rapport.rendezVous()) {
                ligne(date(rdv.dateHeure())+" | "+rdv.motif()+" | "+rdv.statut(),10,true);
                ligne("Lieu : "+val(rdv.lieu())+" | Professionnel : "+val(rdv.professionnelNom())+" | Fuseau d'origine : "+rdv.fuseau()+" | Réf. "+rdv.id(),9,false);
            }
            rubrique("Annexe - valeurs des mesures");
            ligne("Chaque composante est détaillée séparément. Les horaires sont affichés dans le fuseau du rapport. Les graphiques montrent tous les points enregistrés, sans seuil ni interpolation.",9,false);
            for(var s:rapport.seriesMesures()) {
                ligne(libelle(s.typeMesure())+" / "+libelle(s.composante())+" ("+s.unite()+")",11,true);
                for(var p:s.points()) {
                    ligne(date(p.dateMesure())+" | "+val(p.valeur())+" "+s.unite()+" | "+val(p.contexte())+" | "+val(p.source()),9,false);
                    ligne("Référence mesure : "+p.mesureId(),8,false);
                }
            }
            if(glyphesRemplaces)ligne("Certains caractères absents de la police sont affichés sous la forme [U+XXXX]. Le texte original reste disponible dans la synthèse JSON.",9,false);
            flux.close();flux=null;
            int total=document.getNumberOfPages();
            for(int i=0;i<total;i++)try(var footer=new PDPageContentStream(document,document.getPage(i),PDPageContentStream.AppendMode.APPEND,true)) {
                texte(footer,"eCare+ | Document personnel confidentiel",M,29,8,regular,GRIS);
                texte(footer,(i+1)+" / "+total,PDRectangle.A4.getWidth()-M-48,29,8,regular,GRIS);
            }
            var out=new SortieBornee();document.save(out);return out.toByteArray();
        }
        void nouvellePage() throws IOException {
            if(document.getNumberOfPages()>=MAX_PAGES)throw tropGrand();
            if(flux!=null)flux.close();
            var page=new PDPage(PDRectangle.A4);document.addPage(page);flux=new PDPageContentStream(document,page);
            y=PDRectangle.A4.getHeight()-M;
            texte(flux,"ecare+",M,y,18,bold,BLEU);
            texte(flux,"SUIVI PERSONNEL",M+LARGEUR-120,y,9,regular,GRIS);y-=29;
            flux.setStrokingColor(new Color(210,226,238));flux.moveTo(M,y);flux.lineTo(M+LARGEUR,y);flux.stroke();y-=23;
        }
        void reserve(float hauteur) throws IOException {if(y-hauteur<BAS)nouvellePage();}
        void espace(float n) {y-=n;}
        void titre(String t) throws IOException {ligne(t,21,true);espace(8);}
        void rubrique(String t) throws IOException {reserve(70);espace(13);ligne(t,14,true);espace(5);}
        void ligne(String brut,float taille,boolean gras) throws IOException {
            var font=gras?bold:regular;String t=nettoyer(brut,font);
            // Wrap by code point, including unbroken references; never crop a field.
            var ligne=new StringBuilder();float largeur=0;int coupure=-1;
            for(int offset=0;offset<t.length();) {
                int cp=t.codePointAt(offset);String c=new String(Character.toChars(cp));
                float w=font.getStringWidth(c)*taille/1000;
                if(largeur+w>LARGEUR && !ligne.isEmpty()) {
                    if(coupure>0) {
                        String reste=ligne.substring(coupure+1);imprimer(ligne.substring(0,coupure),taille,font,gras);
                        ligne.setLength(0);ligne.append(reste);largeur=font.getStringWidth(reste)*taille/1000;
                    }else {imprimer(ligne.toString(),taille,font,gras);ligne.setLength(0);largeur=0;}
                    coupure=ligne.lastIndexOf(" ");continue;
                }
                ligne.append(c);largeur+=w;if(cp==' ')coupure=ligne.length()-1;offset+=Character.charCount(cp);
            }
            if(!ligne.isEmpty())imprimer(ligne.toString(),taille,font,gras);
        }
        void imprimer(String s,float taille,PDType0Font font,boolean gras) throws IOException {
            reserve(taille*1.65f);texte(flux,s.stripTrailing(),M,y,taille,font,gras?BLEU:TEXTE);y-=taille*1.65f;
        }
        String nettoyer(String brut,PDType0Font font) {
            var out=new StringBuilder();String entree=brut==null?"-":brut;
            entree.codePoints().forEach(cp->{
                if(Character.isISOControl(cp)||Character.isWhitespace(cp)){out.append(' ');return;}
                try{font.encode(new String(Character.toChars(cp)));out.appendCodePoint(cp);}
                catch(IOException|IllegalArgumentException absent){out.append(String.format("[U+%04X]",cp));glyphesRemplaces=true;}
            });return out.toString();
        }
        void graphique(SerieMesureVue s) throws IOException {
            if(s.points().isEmpty())return;
            reserve(160);float x=M+65,base=y-115,w=LARGEUR-80,h=90;
            BigDecimal min=s.minimum(),max=s.maximum(),ecart=max.subtract(min);
            flux.setStrokingColor(new Color(186,205,220));flux.moveTo(x,base+h);flux.lineTo(x,base);flux.lineTo(x+w,base);flux.stroke();
            texte(flux,nettoyer(val(max),regular),M,base+(ecart.signum()==0?h/2:h),8,regular,GRIS);
            if(ecart.signum()!=0)texte(flux,nettoyer(val(min),regular),M,base,8,regular,GRIS);
            long debut=rapport.du().atStartOfDay(zone).toInstant().toEpochMilli();
            long fin=rapport.au().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
            flux.setNonStrokingColor(BLEU);
            for(var p:s.points()) {
                float px=x+w*(p.dateMesure().toEpochMilli()-debut)/(fin-debut);
                float py=base+(ecart.signum()==0?h/2:h*p.valeur().subtract(min).divide(ecart,MathContext.DECIMAL64).floatValue());
                flux.addRect(px-1.7f,py-1.7f,3.4f,3.4f);flux.fill();
            }
            texte(flux,rapport.du().toString(),x,base-17,8,regular,GRIS);
            texte(flux,rapport.au().toString(),x+w-60,base-17,8,regular,GRIS);
            y=base-36;
        }
        String date(Instant i){return i==null?"-":dates.format(i.atZone(zone));}
        static String val(Object o){return o==null?"-":o.toString();}
        static String libelle(String s){return s.replace('_',' ');}
        static void texte(PDPageContentStream stream,String t,float x,float y,float taille,PDType0Font f,Color couleur) throws IOException {
            stream.beginText();stream.setNonStrokingColor(couleur);stream.setFont(f,taille);stream.newLineAtOffset(x,y);stream.showText(t);stream.endText();
        }
        @Override public void close() throws IOException {try{if(flux!=null)flux.close();}finally{document.close();}}
    }
    static IllegalArgumentException tropGrand(){return new IllegalArgumentException("PDF trop volumineux ; générer une synthèse sur une période plus courte. Le rapport JSON reste disponible.");}
    private static class SortieBornee extends ByteArrayOutputStream {
        @Override public synchronized void write(byte[] b,int off,int len){if(len>MAX_BYTES-count)throw tropGrand();super.write(b,off,len);}
        @Override public synchronized void write(int b){if(count>=MAX_BYTES)throw tropGrand();super.write(b);}
    }
}
