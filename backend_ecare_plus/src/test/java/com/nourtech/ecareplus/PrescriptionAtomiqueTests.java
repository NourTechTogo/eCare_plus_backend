package com.nourtech.ecareplus;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PrescriptionAtomiqueTests extends BackendFixture {
    Map<String,Object> request(String ref,String zone){return Map.of("sourceRef",ref,
        "traitement",Map.of("libelle","Prescription atomique fictive","dateDebut",LocalDate.now().toString(),"consignesGenerales","Exemple fictif"),
        "medicament",Map.of("nomCommercial","Exemple "+suffix,"denomination","Exemple","forme","comprimé","dosage","10","uniteDosage","mg"),
        "prise",Map.of("quantite",1,"unite","comprimé","heurePrevue","08:00:00","voieAdministration","orale","fuseau",zone,"joursSemaine",List.of(1,2,3,4,5,6,7)));}
    String path(UUID patient){return "/api/v1/portail/patients/"+patient+"/prescriptions";}
    @Test void fuseauInvalideNeLaisseAucuneEcriture()throws Exception{
        var patient=patientId();var counts=counts();
        mvc.perform(post(path(patient)).header("Authorization",bearer(doctorToken)).contentType("application/json").content(json.writeValueAsString(request("bad-zone","Inconnue/Zone")))).andExpect(status().isBadRequest());
        assertThat(counts()).isEqualTo(counts);
    }
    @Test void repriseApresReponsePerdueRetourneLesMemesIdsEtLeBonFuseau()throws Exception{
        var patient=patientId();String payload=json.writeValueAsString(request("same-key","Europe/Paris"));
        var first=body(mvc.perform(post(path(patient)).header("Authorization",bearer(doctorToken)).contentType("application/json").content(payload)).andExpect(status().isCreated()).andReturn());
        var second=body(mvc.perform(post(path(patient)).header("Authorization",bearer(doctorToken)).contentType("application/json").content(payload)).andExpect(status().isCreated()).andReturn());
        assertThat(second).isEqualTo(first);
        String day=LocalDate.now().toString();var planning=body(mvc.perform(get("/api/v1/portail/patients/"+patient+"/planning-traitements?du="+day+"&au="+day).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andReturn());
        assertThat(planning.path("occurrences").size()).isEqualTo(1);
        assertThat(Instant.parse(planning.path("occurrences").get(0).path("prevueLe").asText())).isEqualTo(LocalDate.now().atTime(8,0).atZone(ZoneId.of("Europe/Paris")).toInstant());
    }
    @Test void memeCleAutreContenuEstUnConflit()throws Exception{
        var patient=patientId();mvc.perform(post(path(patient)).header("Authorization",bearer(doctorToken)).contentType("application/json").content(json.writeValueAsString(request("key","UTC")))).andExpect(status().isCreated());
        mvc.perform(post(path(patient)).header("Authorization",bearer(doctorToken)).contentType("application/json").content(json.writeValueAsString(request("key","Europe/Paris")))).andExpect(status().isConflict());
    }
    @Test void patientEtMedecinNonAffecteNePeuventPrescrire()throws Exception{
        var patient=patientId();String payload=json.writeValueAsString(request("key","UTC"));
        for(String token:List.of(login(),otherToken,adminToken))mvc.perform(post(path(patient)).header("Authorization",bearer(token)).contentType("application/json").content(payload)).andExpect(status().isForbidden());
    }
    @Test void concurrenceUneSeulePrescription()throws Exception{
        var patient=patientId();String payload=json.writeValueAsString(request("concurrent","UTC"));var before=counts();
        var executor=Executors.newFixedThreadPool(4);
        try{
            var futures=new ArrayList<Future<String>>();for(int i=0;i<4;i++)futures.add(executor.submit(()->body(mvc.perform(post(path(patient)).header("Authorization",bearer(doctorToken)).contentType("application/json").content(payload)).andExpect(status().isCreated()).andReturn()).path("traitementId").asText()));
            var ids=new HashSet<String>();for(var future:futures)ids.add(future.get(30,TimeUnit.SECONDS));assertThat(ids).hasSize(1);
            var after=counts();for(int i=0;i<3;i++)assertThat(after.get(i)).isEqualTo(before.get(i)+1);
        }finally{executor.shutdownNow();}
    }
    List<Long> counts(){return List.of(jdbc.queryForObject("select count(*) from traitement",Long.class),jdbc.queryForObject("select count(*) from medicament",Long.class),jdbc.queryForObject("select count(*) from prise_medicament",Long.class));}
    @Test void premiereDoseApresMinuitDansFuseauPositifNestPasOmise()throws Exception{
        var patient=patientId();var commande=new LinkedHashMap<>(request("midnight","Europe/Paris"));
        var horaire=new LinkedHashMap<>((Map<String,Object>)commande.get("prise"));horaire.put("heurePrevue","00:30:00");commande.put("prise",horaire);
        String day=LocalDate.now().toString();mvc.perform(post(path(patient)).header("Authorization",bearer(doctorToken)).contentType("application/json").content(json.writeValueAsString(commande))).andExpect(status().isCreated());
        var planning=body(mvc.perform(get("/api/v1/portail/patients/"+patient+"/planning-traitements?du="+day+"&au="+day).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andReturn());
        assertThat(planning.path("occurrences").size()).isEqualTo(1);
        assertThat(Instant.parse(planning.path("occurrences").get(0).path("prevueLe").asText())).isEqualTo(LocalDate.now().atTime(0,30).atZone(ZoneId.of("Europe/Paris")).toInstant());
    }
}
