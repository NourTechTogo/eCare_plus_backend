package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class DroitsDonneesIntegrationTests extends BackendFixture {
    @Test void demandeEffacementPersistanteSansEffacementIrreversibleEtPrivee()throws Exception{
        var patient=patientId();var token=login();String premier=null;
        for(int i=0;i<2;i++){
            var resultat=body(mvc.perform(post("/api/v1/compte/demandes-effacement").header("Authorization",bearer(token)).contentType("application/json").content("{\"motif\":\"Demande du titulaire\"}" )).andExpect(status().isAccepted()).andExpect(jsonPath("$.statut").value("EN_ATTENTE")).andReturn());
            String id=resultat.path("id").asText();if(premier==null)premier=id;else assertThat(id).isEqualTo(premier);
        }
        mvc.perform(get("/api/v1/compte/demandes-effacement/{id}",premier).header("Authorization",bearer(adminToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/mobile/me").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(patient.toString()));
    }
    @Test void retraitConsentementBloqueAccesCliniqueSansSupprimerProfil()throws Exception{
        var patient=patientId();var token=login();
        mvc.perform(post("/api/v1/mobile/me/consentement").header("Authorization",bearer(token)).contentType("application/json").content("{\"accepte\":false}" )).andExpect(status().isOk());
        mvc.perform(get("/api/v1/portail/patients/{id}/resume",patient).header("Authorization",bearer(doctorToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/mobile/me").header("Authorization",bearer(token))).andExpect(status().isOk());
    }
}
