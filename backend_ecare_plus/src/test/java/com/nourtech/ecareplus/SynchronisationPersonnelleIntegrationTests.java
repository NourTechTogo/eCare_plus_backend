package com.nourtech.ecareplus;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SynchronisationPersonnelleIntegrationTests extends BackendFixture {
    record Compte(UUID id,String token){}
    Compte compte()throws Exception{
        suffix=UUID.randomUUID().toString().substring(0,8);UUID id=patientId();String token=login();
        jdbc.update("delete from affectation_patient where patient_id=?",id);
        return new Compte(id,token);
    }
    Map<String,Object> agenda(){
        var c=new LinkedHashMap<String,Object>();c.put("sourceRef",UUID.randomUUID().toString());c.put("motif","Agenda fictif");c.put("dateHeure",Instant.now().plusSeconds(86400).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());c.put("fuseau","Africa/Lome");c.put("rappelMinutes",30);return c;
    }
    Map<String,Object> declaration(){return new LinkedHashMap<>(Map.of("sourceRef",UUID.randomUUID().toString(),"libelle","Exemple fictif", "dateDebut",LocalDate.now(ZoneOffset.UTC).toString(),"medicament",Map.of("nomCommercial","Exemple fictif","dosage","10","uniteDosage","mg","forme","Comprime"),"prise",Map.of("quantite",1,"unite","comprime","heurePrevue","08:00:00","fuseau","UTC","joursSemaine",List.of(1,2,3,4,5,6,7))));}
    Map<String,Object> element(String local,String type,Map<String,Object> contenu){return new LinkedHashMap<>(Map.of("identifiantLocal",local,"typeOperation",type,"versionSchema",1,"dateCreationLocale",Instant.now().minusSeconds(60).toString(),"contenu",contenu));}
    JsonNode envoyer(Compte c,List<?> elements)throws Exception{return body(mvc.perform(post("/api/v1/mobile/me/synchronisations").header("Authorization",bearer(c.token())).contentType("application/json").content(json.writeValueAsString(Map.of("identifiantAppareil","phone-personnel","elements",elements)))).andExpect(status().isOk()).andReturn()).path("elements");}
    UUID succes(JsonNode r){assertThat(r.path("statut").asText()).isEqualTo("SUCCES");return UUID.fromString(r.path("ressourceId").asText());}
    JsonNode rdv(Compte c,UUID id)throws Exception{return body(mvc.perform(get("/api/v1/mobile/me/rendez-vous/{id}",id).header("Authorization",bearer(c.token()))).andExpect(status().isOk()).andReturn());}

    @Test void creerAgendaEtRejouerNeDupliqueNiEvenementNiRappel()throws Exception{
        var c=compte();var e=element("rdv-1","CREER_RENDEZ_VOUS",agenda());var a=envoyer(c,List.of(e)).get(0);UUID id=succes(a);
        assertThat(envoyer(c,List.of(e)).get(0)).isEqualTo(a);
        assertThat(rdv(c,id).path("patientId").asText()).isEqualTo(c.id().toString());
        assertThat(jdbc.queryForObject("select count(*) from rendez_vous where patient_id=?",Integer.class,c.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from rappel where patient_id=?",Integer.class,c.id())).isEqualTo(1);
    }
    @Test void modifierAnnulerEtRejouerUnRecuAncienPreserveLaVersionOriginale()throws Exception{
        var c=compte();var contenu=agenda();var creation=element("c","CREER_RENDEZ_VOUS",contenu);JsonNode premier=envoyer(c,List.of(creation)).get(0);UUID id=succes(premier);
        var changement=agenda();changement.put("rendezVousId",id.toString());changement.put("version",premier.path("versionServeur").asLong());changement.put("motif","Agenda modifie");
        JsonNode modifie=envoyer(c,List.of(element("m","MODIFIER_RENDEZ_VOUS",changement))).get(0);succes(modifie);
        var perime=new LinkedHashMap<>(changement);perime.put("sourceRef",UUID.randomUUID().toString());
        assertThat(envoyer(c,List.of(element("perime","MODIFIER_RENDEZ_VOUS",perime))).get(0).path("statut").asText()).isEqualTo("CONFLIT");
        var annulation=element("a","ANNULER_RENDEZ_VOUS",Map.of("rendezVousId",id.toString(),"sourceRef",UUID.randomUUID().toString(),"version",modifie.path("versionServeur").asLong()));
        JsonNode annule=envoyer(c,List.of(annulation)).get(0);succes(annule);
        assertThat(rdv(c,id).path("statut").asText()).isEqualTo("ANNULE");
        assertThat(envoyer(c,List.of(creation)).get(0)).isEqualTo(premier);
        assertThat(envoyer(c,List.of(annulation)).get(0)).isEqualTo(annule);
    }
    @Test void declarationPersonnelleEtReessaiDirectPartagentLeMemeRecu()throws Exception{
        var c=compte();var contenu=declaration();var e=element("t","DECLARER_TRAITEMENT",contenu);var premier=envoyer(c,List.of(e)).get(0);UUID id=succes(premier);
        assertThat(envoyer(c,List.of(e)).get(0)).isEqualTo(premier);
        var direct=body(mvc.perform(post("/api/v1/mobile/me/traitements").header("Authorization",bearer(c.token())).contentType("application/json").content(json.writeValueAsString(contenu))).andExpect(status().isCreated()).andReturn());
        assertThat(direct.path("traitementId").asText()).isEqualTo(id.toString());
        assertThat(jdbc.queryForObject("select count(*) from traitement t join dossier_suivi d on d.id=t.dossier_id where d.patient_id=? and d.structure_id is null",Integer.class,c.id())).isEqualTo(1);
    }
    @Test void creationDirectePuisLotAvecMemeSourceNeDupliquePas()throws Exception{
        var c=compte();var contenu=agenda();var direct=body(mvc.perform(post("/api/v1/mobile/me/rendez-vous").header("Authorization",bearer(c.token())).contentType("application/json").content(json.writeValueAsString(contenu))).andExpect(status().isCreated()).andReturn());
        assertThat(succes(envoyer(c,List.of(element("direct","CREER_RENDEZ_VOUS",contenu))).get(0)).toString()).isEqualTo(direct.path("id").asText());
        assertThat(jdbc.queryForObject("select count(*) from rendez_vous where patient_id=?",Integer.class,c.id())).isEqualTo(1);
    }
    @Test void contenuChangeAvecMemeIdentifiantEstEnConflit()throws Exception{
        var c=compte();var contenu=agenda();var e=element("stable","CREER_RENDEZ_VOUS",contenu);succes(envoyer(c,List.of(e)).get(0));contenu.put("motif","Autre contenu");
        assertThat(envoyer(c,List.of(e)).get(0).path("statut").asText()).isEqualTo("CONFLIT");
    }
    @Test void erreursPartiellesNePerdentPasLesElementsValides()throws Exception{
        var c=compte();var invalide=agenda();invalide.remove("sourceRef");
        var declaration=declaration();declaration.put("prise",null);
        var annulation=Map.<String,Object>of("rendezVousId",UUID.randomUUID().toString(),"sourceRef",UUID.randomUUID().toString());
        var elements=new ArrayList<Object>();elements.add(null);elements.add(element("non","CREER_RENDEZ_VOUS",invalide));elements.add(element("declaration-non","DECLARER_TRAITEMENT",declaration));elements.add(element("annulation-non","ANNULER_RENDEZ_VOUS",annulation));elements.add(element("oui","CREER_RENDEZ_VOUS",agenda()));
        var r=envoyer(c,elements);for(int i=0;i<4;i++)assertThat(r.get(i).path("statut").asText()).isEqualTo("REJETE");succes(r.get(4));
        assertThat(jdbc.queryForObject("select count(*) from operation_synchronisation where utilisateur_id=?",Integer.class,c.id())).isEqualTo(1);
    }
    @Test void agendaAutrePatientEtProprietaireInjecteSontRefuses()throws Exception{
        var a=compte();var b=compte();UUID id=succes(envoyer(a,List.of(element("owner","CREER_RENDEZ_VOUS",agenda()))).get(0));
        var contenu=agenda();contenu.put("rendezVousId",id.toString());contenu.put("version",0);
        assertThat(envoyer(b,List.of(element("etranger","MODIFIER_RENDEZ_VOUS",contenu))).get(0).path("statut").asText()).isEqualTo("REJETE");
        var injecte=agenda();injecte.put("patientId",a.id().toString());
        assertThat(envoyer(b,List.of(element("injection","CREER_RENDEZ_VOUS",injecte))).get(0).path("statut").asText()).isEqualTo("REJETE");
        assertThat(rdv(a,id).path("motif").asText()).isEqualTo("Agenda fictif");
    }
    @Test void deuxReessaisConcurrentsOntLeMemeResultat()throws Exception{
        var c=compte();var e=element("concurrent","CREER_RENDEZ_VOUS",agenda());var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{var a=pool.submit(()->{start.await();return envoyer(c,List.of(e));});var b=pool.submit(()->{start.await();return envoyer(c,List.of(e));});start.countDown();var premier=a.get(30,TimeUnit.SECONDS);succes(premier.get(0));assertThat(b.get(30,TimeUnit.SECONDS)).isEqualTo(premier);}finally{pool.shutdownNow();}
        assertThat(jdbc.queryForObject("select count(*) from rendez_vous where patient_id=?",Integer.class,c.id())).isEqualTo(1);
    }
    @Test void fuseauInvalideNeBloquePasLeLotEtNeLaissePasDeTraitementPartiel()throws Exception{
        var c=compte();var contenu=declaration();
        var horaire=new LinkedHashMap<String,Object>((Map<String,Object>)contenu.get("prise"));horaire.put("fuseau","Inconnu/Invalide");contenu.put("prise",horaire);
        var extreme=agenda();extreme.put("dateHeure",Instant.MAX.toString());
        var r=envoyer(c,List.of(element("fuseau-non","DECLARER_TRAITEMENT",contenu),element("date-non","CREER_RENDEZ_VOUS",extreme),element("apres","CREER_RENDEZ_VOUS",agenda())));
        assertThat(r.get(0).path("statut").asText()).isEqualTo("REJETE");assertThat(r.get(1).path("statut").asText()).isEqualTo("REJETE");succes(r.get(2));
        assertThat(jdbc.queryForObject("select count(*) from traitement t join dossier_suivi d on d.id=t.dossier_id where d.patient_id=?",Integer.class,c.id())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from operation_synchronisation where utilisateur_id=?",Integer.class,c.id())).isEqualTo(1);
    }
    @Test void corpsNulEstUneErreurClient()throws Exception{
        var c=compte();mvc.perform(post("/api/v1/mobile/me/synchronisations").header("Authorization",bearer(c.token())).contentType("application/json").content("null")).andExpect(status().isBadRequest());
    }
}
