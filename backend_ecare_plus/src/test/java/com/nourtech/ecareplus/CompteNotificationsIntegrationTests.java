package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class CompteNotificationsIntegrationTests extends BackendFixture {
    @Test void boiteCommuneExposeLienVersRessourceEtLectureEstPriveeIdempotente()throws Exception{
        patientId();var token=login();String conversation=body(mvc.perform(post("/api/v1/mobile/conversations").header("Authorization",bearer(token)).contentType("application/json").content("{\"professionnelId\":\""+doctor+"\"}" )).andExpect(status().isCreated()).andReturn()).path("id").asText();
        mvc.perform(post("/api/v1/mobile/conversations/{id}/messages",conversation).header("Authorization",bearer(token)).contentType("application/json").content("{\"contenu\":\"Bonjour\"}" )).andExpect(status().isCreated());
        var notification=body(mvc.perform(get("/api/v1/compte/notifications").header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].typeRessource").value("CONVERSATION")).andExpect(jsonPath("$.content[0].ressourceId").value(conversation)).andReturn()).path("content").get(0);
        String id=notification.path("id").asText();
        mvc.perform(post("/api/v1/compte/notifications/{id}/lecture",id).header("Authorization",bearer(token))).andExpect(status().isForbidden());
        for(int i=0;i<2;i++)mvc.perform(post("/api/v1/compte/notifications/{id}/lecture",id).header("Authorization",bearer(doctorToken))).andExpect(status().isNoContent());
    }
}
