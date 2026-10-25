package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class NoteCibleeIntegrationTests extends BackendFixture {
    @Test void notePriveePuisPartageeEtSeulAuteurPeutModifierSupprimer() throws Exception {
        patientId();var token=login();
        var mesure=body(mvc.perform(post("/api/v1/mobile/me/mesures").header("Authorization",bearer(token)).contentType("application/json").content("{\"typeMesure\":\"POIDS\",\"valeurPrincipale\":70,\"unite\":\"kg\",\"dateMesure\":\""+Instant.now().minusSeconds(60)+"\"}" )).andExpect(status().isCreated()).andReturn());
        String cible=mesure.path("id").asText();
        var note=body(mvc.perform(post("/api/v1/notes").header("Authorization",bearer(token)).contentType("application/json")
            .content("{\"typeCible\":\"MESURE\",\"cibleId\":\""+cible+"\",\"contenu\":\"Ma note\",\"visibilite\":\"PRIVEE\"}" )).andExpect(status().isCreated()).andReturn());
        String id=note.path("id").asText();
        mvc.perform(get("/api/v1/notes").param("typeCible","MESURE").param("cibleId",cible).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(patch("/api/v1/notes/{id}",id).header("Authorization",bearer(doctorToken)).contentType("application/json").content("{\"contenu\":\"Usurpation\",\"visibilite\":\"PARTAGEE\",\"version\":0}" )).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/notes/{id}",id).header("Authorization",bearer(token)).contentType("application/json").content("{\"contenu\":\"Note partagée\",\"visibilite\":\"PARTAGEE\",\"version\":0}" )).andExpect(status().isOk());
        mvc.perform(get("/api/v1/notes").param("typeCible","MESURE").param("cibleId",cible).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(delete("/api/v1/notes/{id}",id).header("Authorization",bearer(token))).andExpect(status().isNoContent());
    }
}
