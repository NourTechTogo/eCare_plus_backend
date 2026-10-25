package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class MessageRetryTests extends BackendFixture {
 @Test void nouvelleTentativeNeDupliquePasEtRefuseUneCommandeDifferente() throws Exception {
  patientId();var token=login();
  String conversation=body(mvc.perform(post("/api/v1/mobile/conversations").header("Authorization",bearer(token)).contentType("application/json").content("{\"professionnelId\":\""+doctor+"\"}")).andExpect(status().isCreated()).andReturn()).path("id").asText();
  String source=UUID.randomUUID().toString(),commande="{\"contenu\":\"Essai unique\",\"sourceRef\":\""+source+"\"}";
  String id=body(mvc.perform(post("/api/v1/mobile/conversations/{id}/messages",conversation).header("Authorization",bearer(token)).contentType("application/json").content(commande)).andExpect(status().isCreated()).andReturn()).path("id").asText();
  mvc.perform(post("/api/v1/mobile/conversations/{id}/messages",conversation).header("Authorization",bearer(token)).contentType("application/json").content(commande)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
  mvc.perform(post("/api/v1/mobile/conversations/{id}/messages",conversation).header("Authorization",bearer(token)).contentType("application/json").content(commande.replace("Essai unique","Autre message"))).andExpect(status().isConflict());
  mvc.perform(get("/api/v1/mobile/conversations/{id}/messages",conversation).header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
 }
}
