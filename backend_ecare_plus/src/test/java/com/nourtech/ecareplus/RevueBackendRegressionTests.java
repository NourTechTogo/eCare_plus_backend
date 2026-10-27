package com.nourtech.ecareplus;
import com.nourtech.ecareplus.care.domain.entity.*;
import com.nourtech.ecareplus.care.domain.enumeration.*;
import com.nourtech.ecareplus.organization.domain.entity.StructureSante;
import com.nourtech.ecareplus.identity.domain.entity.Patient;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class RevueBackendRegressionTests extends BackendFixture {
    @Test void arretAvantDebutNeCreePasDateFinInvalide()throws Exception{
        var patient=patientId();var prise=prise(patient,LocalDate.now(ZoneOffset.UTC).plusDays(1),LocalTime.of(9,0));UUID traitement=tx.execute(s->{var t=em.find(PriseMedicament.class,prise).getTraitement();t.modifier(t.getLibelle(),null,null,null);return t.getId();});
        mvc.perform(patch("/api/v1/traitements/{id}/terminaison",traitement).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.dateFin").isEmpty());
    }
    UUID prise(UUID patient,LocalDate date,LocalTime heure){return tx.execute(s->{var d=em.createQuery("select d from DossierSuivi d where d.patient.id=:id and d.structure.id=:structure",DossierSuivi.class).setParameter("id",patient).setParameter("structure",structure).getSingleResult();return prise(d,date,heure).getId();});}
    PriseMedicament prise(DossierSuivi d,LocalDate date,LocalTime heure){var t=new Traitement(d,"Suivi",null,date,date,"Médecin",null);em.persist(t);var m=new Medicament("Exemple","Exemple","comprimé","10","mg");em.persist(m);var p=new PriseMedicament(t,m,BigDecimal.ONE,"comprimé","QUOTIDIEN",heure,null,"orale",null);em.persist(p);return p;}
    com.fasterxml.jackson.databind.JsonNode planning(String token,LocalDate date)throws Exception{return body(mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du",date.toString()).param("au",date.toString()).header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn());}
    @Test void changementHoraireNeDoublePasLesDosesHistoriques()throws Exception{
        var patient=patientId();var date=LocalDate.now(ZoneOffset.UTC).minusDays(1);var prise=prise(patient,date,LocalTime.of(9,0));var token=login();planning(token,date);
        mvc.perform(patch("/api/v1/traitements/prises/{id}/horaire",prise).header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"heurePrevue\":\"10:00:00\",\"frequence\":\"QUOTIDIEN\"}" )).andExpect(status().isOk());
        mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du",date.toString()).param("au",date.toString()).header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.occurrences.length()").value(1)).andExpect(jsonPath("$.occurrences[0].prevueLe").value(date+"T09:00:00Z"));
    }
    @Test void arretTraitementInvalideDoseFutureSansLectureIntermediaire()throws Exception{
        var patient=patientId();var date=LocalDate.now(ZoneOffset.UTC).plusDays(1);var prise=prise(patient,date,LocalTime.of(9,0));var token=login();var occurrence=planning(token,date).path("occurrences").get(0);var traitement=tx.execute(s->em.find(PriseMedicament.class,prise).getTraitement().getId());
        mvc.perform(patch("/api/v1/traitements/{id}/terminaison",traitement).header("Authorization",bearer(doctorToken))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/mobile/me/prises/{id}/reponse",occurrence.path("id").asText()).header("Authorization",bearer(token)).contentType("application/json").content("{\"action\":\"PRIS\",\"dateAction\":\""+Instant.now().minusSeconds(1)+"\",\"sourceRef\":\"stopped\",\"version\":"+occurrence.path("version").asLong()+"}" )).andExpect(status().isConflict());
    }
    @Test void reactivationRendDeNouveauLaDoseFutureUtilisable()throws Exception{
        var patient=patientId();var date=LocalDate.now(ZoneOffset.UTC).plusDays(1);var prise=prise(patient,date,LocalTime.of(9,0));var token=login();planning(token,date);
        mvc.perform(patch("/api/v1/traitements/prises/{id}/desactivation",prise).header("Authorization",bearer(doctorToken))).andExpect(status().isOk());planning(token,date);
        mvc.perform(patch("/api/v1/traitements/prises/{id}/activation",prise).header("Authorization",bearer(doctorToken))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du",date.toString()).param("au",date.toString()).header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.occurrences[0].annulee").value(false));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("select count(*) from revision_planning_prise where prise_id=?",Integer.class,prise)).isGreaterThanOrEqualTo(3);
    }
    @Test void prescriptionsEtNotesRefusentDossierAutreStructureDuMemePatient()throws Exception{
        var patient=patientId();UUID[] ressources=tx.execute(s->{var structure=new StructureSante("Autre","CLINIQUE","Rue","Kara","Nord","SEC-"+suffix,null,"SEC-"+suffix);structure.activer();em.persist(structure);var p=em.find(Patient.class,patient);var d=new DossierSuivi(p,structure,null);em.persist(d);var prise=prise(d,LocalDate.now(ZoneOffset.UTC),LocalTime.of(9,0));var m=new MesureSante(p,d,TypeMesure.POIDS,new BigDecimal("70"),null,"kg",Instant.now().minusSeconds(60),null,null,SourceMesure.PATIENT,"SYNCHRONISEE");em.persist(m);return new UUID[]{prise.getId(),m.getId()};});
        mvc.perform(get("/api/v1/portail/patients/{id}/planning-traitements",patient).param("du",LocalDate.now(ZoneOffset.UTC).toString()).param("au",LocalDate.now(ZoneOffset.UTC).toString()).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.traitements.length()").value(0));
        mvc.perform(put("/api/v1/portail/prises/{id}/planning",ressources[0]).header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"version\":0,\"fuseau\":\"UTC\",\"joursSemaine\":[1]}" )).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/notes").header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"typeCible\":\"MESURE\",\"cibleId\":\""+ressources[1]+"\",\"contenu\":\"Interdit\",\"visibilite\":\"PARTAGEE\"}" )).andExpect(status().isForbidden());
    }
    @Test void elementInconnuConserveLesAccusesEtLesElementsSuivants()throws Exception{
        patientId();var token=login();String date=Instant.now().minusSeconds(120).toString();
        String mesure="{\"identifiantLocal\":\"m1\",\"typeOperation\":\"CREER_MESURE\",\"versionSchema\":1,\"dateCreationLocale\":\""+date+"\",\"contenu\":{\"typeMesure\":\"POIDS\",\"valeurPrincipale\":70,\"unite\":\"kg\",\"dateMesure\":\""+date+"\"}}";
        String inconnu="{\"identifiantLocal\":\"inconnu\",\"typeOperation\":\"REPONDRE_PRISE\",\"versionSchema\":1,\"dateCreationLocale\":\""+date+"\",\"contenu\":{\"occurrenceId\":\""+UUID.randomUUID()+"\",\"action\":\"PRIS\",\"dateAction\":\""+date+"\",\"version\":0}}";
        mvc.perform(post("/api/v1/mobile/me/synchronisations").header("Authorization",bearer(token)).contentType("application/json").content("{\"identifiantAppareil\":\"review-phone\",\"elements\":["+mesure+","+inconnu+","+mesure.replace("m1","m2")+"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.elements[0].statut").value("SUCCES")).andExpect(jsonPath("$.elements[1].statut").value("REJETE")).andExpect(jsonPath("$.elements[2].statut").value("SUCCES"));
    }
}

