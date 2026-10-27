package com.nourtech.ecareplus;

import com.nourtech.ecareplus.care.domain.entity.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlanningTraitementIntegrationTests extends BackendFixture {
    UUID programme(UUID patient,LocalDate debut,LocalDate fin,LocalTime heure){return tx.execute(status->{
        var dossier=em.createQuery("select d from DossierSuivi d where d.patient.id=:id and d.structure.id=:structure",DossierSuivi.class).setParameter("id",patient).setParameter("structure",structure).getSingleResult();
        var traitement=new Traitement(dossier,"Traitement de suivi",null,debut,fin,"Médecin",null);em.persist(traitement);
        var medicament=new Medicament("Exemple","Exemple","comprimé","10","mg");em.persist(medicament);
        var prise=new PriseMedicament(traitement,medicament,BigDecimal.ONE,"comprimé","QUOTIDIEN",heure,null,"orale",null);em.persist(prise);return prise.getId();
    });}
    @Test void planningRespecteDebutFinEtHorairesEtIdentifiantsStables() throws Exception {
        var patient=patientId();programme(patient,LocalDate.of(2026,1,1),LocalDate.of(2026,1,2),LocalTime.of(8,0));var token=login();String premier=null;
        for(int i=0;i<2;i++){
            var result=body(mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du","2026-01-01").param("au","2026-01-03").header("Authorization",bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.occurrences.length()").value(2)).andExpect(jsonPath("$.occurrences[0].quantite").value(1)).andReturn());
            if(premier==null)premier=result.path("occurrences").get(0).path("id").asText();else assertThat(result.path("occurrences").get(0).path("id").asText()).isEqualTo(premier);
        }
        mvc.perform(get("/api/v1/portail/patients/{id}/planning-traitements",patient).param("du","2026-01-01").param("au","2026-01-03").header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());
    }
    @Test void planningStableAuChangementHeureEtVersionPerimeeRefusee() throws Exception {
        var patient=patientId();var prise=programme(patient,LocalDate.of(2026,3,29),LocalDate.of(2026,3,29),LocalTime.of(2,30));
        mvc.perform(put("/api/v1/portail/prises/{id}/planning",prise).header("Authorization",bearer(doctorToken)).contentType("application/json")
            .content("{\"version\":0,\"fuseau\":\"Europe/Paris\",\"joursSemaine\":[1,2,3,4,5,6,7]}" )).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(get("/api/v1/mobile/me/notifications").header("Authorization",bearer(login()))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].type").value("TRAITEMENT"));
        mvc.perform(put("/api/v1/portail/prises/{id}/planning",prise).header("Authorization",bearer(doctorToken)).contentType("application/json")
            .content("{\"version\":0,\"fuseau\":\"UTC\",\"joursSemaine\":[1]}" )).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du","2026-03-29").param("au","2026-03-29").header("Authorization",bearer(login())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.occurrences[0].prevueLe").value("2026-03-29T01:30:00Z"));
    }
    @Test void periodeLocaleExclutOccurrencesAdjacentesEtObservanceFutureVide() throws Exception {
        var patient=patientId();programme(patient,LocalDate.of(2026,1,1),LocalDate.of(2026,1,2),LocalTime.of(8,0));var token=login();
        mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du","2026-01-01").param("au","2026-01-02").header("Authorization",bearer(token))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du","2026-01-01").param("au","2026-01-01").header("Authorization",bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.occurrences.length()").value(1));
        mvc.perform(get("/api/v1/mobile/me/observance").param("du","2027-01-01").param("au","2027-01-01").header("Authorization",bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.prisesPrevues").value(0)).andExpect(jsonPath("$.taux").isEmpty());
    }
    @Test void historiqueConserveDosageApresCorrectionDuCatalogue()throws Exception{
        var patient=patientId();var prise=programme(patient,LocalDate.of(2026,1,1),LocalDate.of(2026,1,1),LocalTime.of(8,0));var token=login();
        mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du","2026-01-01").param("au","2026-01-01").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.occurrences[0].dosage").value("10"));
        tx.executeWithoutResult(s->em.find(PriseMedicament.class,prise).getMedicament().modifier("Autre libellé","Exemple","comprimé","20","mg"));
        mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du","2026-01-01").param("au","2026-01-01").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.occurrences[0].dosage").value("10"));
    }
    @Test void reportPuisPrisConserveUneOccurrenceEtObservanceCorrecte() throws Exception {
        var patient=patientId();programme(patient,LocalDate.of(2026,1,1),LocalDate.of(2026,1,1),LocalTime.of(8,0));var token=login();
        var planning=body(mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du","2026-01-01").param("au","2026-01-01").header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn());
        var occurrence=planning.path("occurrences").get(0);var id=occurrence.path("id").asText();
        var report=body(mvc.perform(post("/api/v1/mobile/me/prises/{id}/reponse",id).header("Authorization",bearer(token)).contentType("application/json")
            .content("{\"action\":\"REPORTER\",\"reporteeAu\":\"2026-01-01T09:00:00Z\",\"dateAction\":\"2026-01-01T08:00:00Z\",\"sourceRef\":\"report-1\",\"version\":"+occurrence.path("version").asLong()+"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id)).andReturn());
        String confirmation="{\"action\":\"PRIS\",\"dateAction\":\"2026-01-01T09:00:00Z\",\"sourceRef\":\"pris-1\",\"version\":"+report.path("version").asLong()+"}";
        for(int i=0;i<2;i++)mvc.perform(post("/api/v1/mobile/me/prises/{id}/reponse",id).header("Authorization",bearer(token)).contentType("application/json").content(confirmation)).andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("PRISE")).andExpect(jsonPath("$.dateActionClient").value("2026-01-01T09:00:00Z")).andExpect(jsonPath("$.dateConfirmation").isNotEmpty()).andExpect(jsonPath("$.reporteeAu").value("2026-01-01T09:00:00Z"));
        mvc.perform(get("/api/v1/mobile/me/observance").param("du","2026-01-01").param("au","2026-01-01").header("Authorization",bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.prisesPrevues").value(1)).andExpect(jsonPath("$.prisesConfirmees").value(1)).andExpect(jsonPath("$.taux").value(100));
        mvc.perform(get("/api/v1/mobile/me/planning-traitements").param("du","2026-01-01").param("au","2026-01-01").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.occurrences.length()").value(1));
    }
}
