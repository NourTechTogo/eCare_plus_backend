package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ReglesCdcIntegrationTests extends BackendFixture {
    @org.springframework.beans.factory.annotation.Autowired com.nourtech.ecareplus.alert.application.service.AlerteService alertes;
    void regle(UUID patient,String type,String operateur,String seuil,String priorite,int periode)throws Exception{
        String id=body(mvc.perform(post("/api/v1/regles-alerte").header("Authorization",bearer(doctorToken)).contentType("application/json")
            .content("{\"nom\":\"Règle de test\",\"patientId\":\""+patient+"\",\"typeMesure\":\""+type+"\",\"operateur\":\""+operateur+"\",\"seuilMin\":"+seuil+",\"nombreOccurrences\":1,\"periodeObservation\":"+periode+",\"niveauPriorite\":\""+priorite+"\"}" )).andExpect(status().isCreated()).andReturn()).path("id").asText();
        mvc.perform(patch("/api/v1/regles-alerte/{id}/validation",id).header("Authorization",bearer(doctorToken))).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/regles-alerte/{id}/activation",id).header("Authorization",bearer(doctorToken))).andExpect(status().isOk());
    }
    void mesure(String token,String type,String valeur,String unite,Instant date)throws Exception{
        mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization",bearer(token)).contentType("application/json")
            .content("{\"typeMesure\":\""+type+"\",\"valeurPrincipale\":"+valeur+",\"unite\":\""+unite+"\",\"dateMesure\":\""+date+"\"}" )).andExpect(status().isCreated());
    }
    @Test void critiquePrioritaireSansDoublonAttentionSurMemeMesure()throws Exception{
        var patient=patientId();var token=login();regle(patient,"GLYCEMIE",">","110","ELEVE",1);regle(patient,"GLYCEMIE",">=","200","CRITIQUE",1);
        mesure(token,"GLYCEMIE","2.5","g/L",Instant.now().minusSeconds(10));
        mvc.perform(get("/api/v1/mobile/me/alertes").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].gravite").value("CRITIQUE"));
        tx.executeWithoutResult(s->{var m=em.createQuery("select m from MesureSante m where m.patient.id=:id",com.nourtech.ecareplus.care.domain.entity.MesureSante.class).setParameter("id",patient).getSingleResult();alertes.evaluerAutomatiquement(m);});
        mvc.perform(get("/api/v1/mobile/me/alertes").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test void variationPoidsCompareAuPasseDansLaPeriodeSansSeuilAbsolu()throws Exception{
        var patient=patientId();var token=login();regle(patient,"POIDS","VARIATION_ABSOLUE","2","ELEVE",4320);
        mesure(token,"POIDS","70","kg",Instant.now().minusSeconds(2*86400));
        mesure(token,"POIDS","72.1","kg",Instant.now().minusSeconds(1));
        mvc.perform(get("/api/v1/mobile/me/alertes").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
}
