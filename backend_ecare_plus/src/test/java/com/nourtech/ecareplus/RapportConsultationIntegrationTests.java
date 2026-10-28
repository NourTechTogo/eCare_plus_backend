package com.nourtech.ecareplus;
import com.fasterxml.jackson.databind.JsonNode;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.domain.enumeration.*;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RapportConsultationIntegrationTests extends RapportConsultationFixture {
    @Test void patientAutonomeObtientStatistiquesCanoniquesEtComposantesDistinctes()throws Exception{
        var c=compte(true);
        mesure(c,TypeMesure.GLYCEMIE,"1.2",null,"g/L","2026-10-20T08:00:00Z");mesure(c,TypeMesure.GLYCEMIE,"140",null,"mg/dL","2026-10-22T08:00:00Z");
        mesure(c,TypeMesure.TENSION_ARTERIELLE,"120","80","mmHg","2026-10-20T09:00:00Z");mesure(c,TypeMesure.TENSION_ARTERIELLE,"140","90","mmHg","2026-10-22T09:00:00Z");
        var r=generer(c,commande());assertThat(r.path("patient").path("id").asText()).isEqualTo(c.id().toString());assertThat(r.path("nombreMesures").asInt()).isEqualTo(4);
        var g=serie(r,"GLYCEMIE","PRINCIPALE");assertThat(g.path("unite").asText()).isEqualTo("mg/dL");assertThat(g.path("moyenne").decimalValue()).isEqualByComparingTo("130");assertThat(g.path("minimum").decimalValue()).isEqualByComparingTo("120");assertThat(g.path("joursSansSaisie").asInt()).isEqualTo(1);
        assertThat(serie(r,"TENSION_ARTERIELLE","PRINCIPALE").path("moyenne").decimalValue()).isEqualByComparingTo("130");assertThat(serie(r,"TENSION_ARTERIELLE","DIASTOLIQUE").path("moyenne").decimalValue()).isEqualByComparingTo("85");
    }
    @Test void periodeRespecteFuseauEtBorneDeFinExclusive()throws Exception{
        var c=compte(true);mesure(c,TypeMesure.POIDS,"90",null,"kg","2026-10-19T09:59:59Z");mesure(c,TypeMesure.POIDS,"100",null,"kg","2026-10-19T10:00:00Z");mesure(c,TypeMesure.POIDS,"120",null,"kg","2026-10-20T09:59:59Z");mesure(c,TypeMesure.POIDS,"150",null,"kg","2026-10-20T10:00:00Z");
        var q=commande();q.put("au","2026-10-20");q.put("fuseau","Pacific/Kiritimati");var r=generer(c,q);assertThat(r.path("nombreMesures").asInt()).isEqualTo(2);assertThat(serie(r,"POIDS","PRINCIPALE").path("moyenne").decimalValue()).isEqualByComparingTo("110");
    }
    @Test void rapportFigeEtRecuRestentIdentiquesApresNouvelleMesure()throws Exception{
        var c=compte(true);mesure(c,TypeMesure.POIDS,"80",null,"kg","2026-10-20T08:00:00Z");var q=commande();var original=generer(c,q);
        mesure(c,TypeMesure.POIDS,"90",null,"kg","2026-10-21T08:00:00Z");assertThat(lire(c,original.path("id").asText())).isEqualTo(original);assertThat(generer(c,q)).isEqualTo(original);
        q.put("sourceRef",UUID.randomUUID().toString());assertThat(generer(c,q).path("nombreMesures").asInt()).isEqualTo(2);
    }
    @Test void absenceDeSaisieNInventeNiMoyenneNiObservance()throws Exception{
        var c=compte(true);var r=generer(c,commande());assertThat(r.path("seriesMesures").isEmpty()).isTrue();assertThat(r.path("nombreMesures").asInt()).isZero();assertThat(r.path("joursSansSaisie").asInt()).isEqualTo(3);assertThat(r.has("tauxObservance")).isFalse();assertThat(r.path("avertissements").isEmpty()).isFalse();
    }
    @Test void autrePatientEtMedecinAffecteNeLisentPasLaSynthesePersonnelle()throws Exception{
        var c=compte(false);var r=generer(c,commande());String id=r.path("id").asText();var autre=compte(true);
        mvc.perform(get(API+"/{id}",id).header("Authorization",bearer(autre.token()))).andExpect(status().isForbidden());
        mvc.perform(get(API+"/{id}",id).header("Authorization",bearer(doctorToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/rapports-suivi/{id}",id).header("Authorization",bearer(doctorToken))).andExpect(status().isForbidden());
        mvc.perform(get(API).header("Authorization",bearer(autre.token()))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/api/v1/mobile/me/consentement").header("Authorization",bearer(c.token())).contentType("application/json").content("{\"accepte\":false}")).andExpect(status().isOk());
        mvc.perform(get(API+"/{id}",id).header("Authorization",bearer(c.token()))).andExpect(status().isForbidden());
    }
    @Test void referenceReutiliseeAvecAutrePeriodeEstUnConflit()throws Exception{
        var c=compte(true);var q=commande();generer(c,q);q.put("au","2026-10-21");mvc.perform(post(API).header("Authorization",bearer(c.token())).contentType("application/json").content(json.writeValueAsString(q))).andExpect(status().isConflict());
    }
    @Test void periodeInvalideEtFuseauInconnuSontRefuses()throws Exception{
        var c=compte(true);
        for(var changement:List.of(Map.of("au","2026-10-19"),Map.of("du","2025-01-01"),Map.of("fuseau","Invalide/Zone"),Map.of("au",LocalDate.now(ZoneOffset.UTC).plusDays(2).toString()))){var q=commande();q.putAll(changement);mvc.perform(post(API).header("Authorization",bearer(c.token())).contentType("application/json").content(json.writeValueAsString(q))).andExpect(status().isBadRequest());}
    }
    @Test void reponseDejaEnregistreePourPlusTardAujourdhuiEstIncluse()throws Exception{
        var c=compte(true);LocalDate jour=LocalDate.now(ZoneOffset.UTC);Instant date=jour.atTime(23,59).toInstant(ZoneOffset.UTC);
        tx.executeWithoutResult(s->{var t=new Traitement(dossier(c),"Exemple fictif",null,jour,null,null,null);em.persist(t);var m=new Medicament("Exemple fictif","Exemple","Comprime","10","mg");em.persist(m);var p=new PriseMedicament(t,m,BigDecimal.ONE,"comprime","QUOTIDIEN",LocalTime.of(23,59),null,null,null);em.persist(p);var o=new ConfirmationPrise(p,date);o.confirmer(null,Instant.now());em.persist(o);});
        var q=commande();q.put("du",jour.toString());q.put("au",jour.toString());var r=generer(c,q);
        assertThat(r.path("reponsesPrises").size()).isEqualTo(1);assertThat(r.path("reponsesPrises").get(0).path("statut").asText()).isEqualTo("PRISE");
    }
    @Test void rubriquesConserventReponsesAlertesEtAgendaSansCreerDHistoire()throws Exception{
        var c=compte(true);
        tx.executeWithoutResult(s->{var patient=em.find(Patient.class,c.id());var d=dossier(c);
            var t=new Traitement(d,"Exemple fictif",null,LocalDate.parse("2026-10-20"),null,null,null);em.persist(t);
            var med=new Medicament("Exemple fictif","Exemple","Comprime","10","mg");em.persist(med);
            var prise=new PriseMedicament(t,med,BigDecimal.ONE,"comprime","QUOTIDIEN",LocalTime.of(8,0),null,null,null);em.persist(prise);
            var conf=new ConfirmationPrise(prise,Instant.parse("2026-10-20T08:00:00Z"));conf.confirmer(null,Instant.parse("2026-10-20T09:00:00Z"));em.persist(conf);
            em.persist(new ConfirmationPrise(prise,Instant.parse("2026-10-21T08:00:00Z")));
            var m=new MesureSante(patient,d,TypeMesure.POIDS,new BigDecimal("110"),null,"kg",Instant.parse("2026-10-20T10:00:00Z"),null,null,SourceMesure.PATIENT,"SYNCHRONISEE");em.persist(m);
            var regle=new com.nourtech.ecareplus.alert.domain.entity.RegleAlerte("Exemple fictif",null,TypeMesure.POIDS,">=",new BigDecimal("100"),null,1,60,com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.ELEVE);regle.definirPortee(c.id(),null,"PRINCIPALE","kg");regle.validerRegle(Instant.now());regle.tracerValidation(null,"DEMONSTRATION");regle.activerRegle();em.persist(regle);
            em.persist(new com.nourtech.ecareplus.alert.domain.entity.Alerte(patient,m,regle,com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte.SEUIL_MESURE,"Exemple fictif",null,com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.ELEVE,com.nourtech.ecareplus.alert.domain.enumeration.OrigineAlerte.AUTOMATIQUE));
            em.persist(new com.nourtech.ecareplus.alert.domain.entity.Alerte(patient,null,null,com.nourtech.ecareplus.alert.domain.enumeration.TypeAlerte.SEUIL_MESURE,"Signalement fictif",null,com.nourtech.ecareplus.alert.domain.enumeration.NiveauPriorite.ELEVE));
            var rdv=com.nourtech.ecareplus.communication.domain.entity.RendezVous.personnel(patient,"Agenda fictif",Instant.parse("2026-10-20T12:00:00Z"),"UTC",null,null,null);rdv.annuler();em.persist(rdv);
        });
        var r=generer(c,commande());assertThat(r.path("traitements").size()).isEqualTo(1);assertThat(r.path("traitements").get(0).path("declarationPersonnelle").asBoolean()).isTrue();assertThat(r.path("traitements").get(0).path("horaires").get(0).path("dosage").asText()).isEqualTo("10");
        assertThat(r.path("reponsesPrises").size()).isEqualTo(1);assertThat(r.path("alertes").size()).isEqualTo(1);assertThat(r.path("alertes").get(0).path("regleAppliquee").path("sourceValidation").asText()).isEqualTo("DEMONSTRATION");assertThat(r.path("rendezVous").get(0).path("statut").asText()).isEqualTo("ANNULE");
        assertThat(jdbc.queryForObject("select count(*) from confirmation_prise c join prise_medicament p on p.id=c.prise_medicament_id join traitement t on t.id=p.traitement_id join dossier_suivi d on d.id=t.dossier_id where d.patient_id=?",Integer.class,c.id())).isEqualTo(2);
        var actuel=commande();actuel.put("du",LocalDate.now(ZoneOffset.UTC).toString());actuel.put("au",LocalDate.now(ZoneOffset.UTC).toString());var maintenant=generer(c,actuel);assertThat(maintenant.path("alertes").size()).isEqualTo(1);assertThat(maintenant.path("alertes").get(0).path("origine").asText()).isEqualTo("MANUELLE");
    }
    @Test void tropDeMesuresEstRefuseSansRapportPartielNiTroncature()throws Exception{
        var c=compte(true);UUID d=tx.execute(s->dossier(c).getId());var lignes=new ArrayList<Object[]>();var now=java.sql.Timestamp.from(Instant.now());
        for(int i=0;i<5001;i++)lignes.add(new Object[]{UUID.randomUUID(),now,now,c.id(),d,java.sql.Timestamp.from(Instant.parse("2026-10-20T00:00:00Z").plusSeconds(i))});
        jdbc.batchUpdate("insert into mesure_sante(id,version,date_creation,date_modification,patient_id,dossier_id,type_mesure,valeur_principale,unite,date_mesure,source,statut_synchronisation,valeur_confirmee) values (?,0,?,?,?,?,'POIDS',80,'kg',?,'PATIENT','SYNCHRONISEE',false)",lignes);
        mvc.perform(post(API).header("Authorization",bearer(c.token())).contentType("application/json").content(json.writeValueAsString(commande()))).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from rapport_suivi r join dossier_suivi d on d.id=r.dossier_id where d.patient_id=?",Integer.class,c.id())).isZero();
    }
    @Test void requetesConcurrentesRetournentLeMemeRapport()throws Exception{
        var c=compte(true);var q=commande();var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->{start.await();return generer(c,q);});var b=pool.submit(()->{start.await();return generer(c,q);});start.countDown();assertThat(a.get(30,TimeUnit.SECONDS)).isEqualTo(b.get(30,TimeUnit.SECONDS));}finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("select count(*) from rapport_suivi r join dossier_suivi d on d.id=r.dossier_id where d.patient_id=?",Integer.class,c.id())).isEqualTo(1);
    }
}
