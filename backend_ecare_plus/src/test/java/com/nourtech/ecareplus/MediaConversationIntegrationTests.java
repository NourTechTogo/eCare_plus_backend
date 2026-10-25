package com.nourtech.ecareplus;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class MediaConversationIntegrationTests extends BackendFixture {
    @Test void piecePriveeRattacheeAuMessageVisibleSeulementAuxParticipants() throws Exception {
        patientId();var token=login();
        String conversation=body(mvc.perform(post("/api/v1/mobile/conversations").header("Authorization",bearer(token)).contentType("application/json").content("{\"professionnelId\":\""+doctor+"\"}" )).andExpect(status().isCreated()).andReturn()).path("id").asText();
        var piece=body(mvc.perform(multipart("/api/v1/mobile/conversations/{id}/pieces-jointes",conversation).file(new MockMultipartFile("fichier","../../ordonnance.pdf","application/pdf",DocumentMedicalIntegrationTests.PDF))
            .param("sourceRef","piece-1").header("Authorization",bearer(token))).andExpect(status().isCreated()).andExpect(jsonPath("$.cleStockage").doesNotExist()).andReturn());
        String id=piece.path("id").asText();
        mvc.perform(get("/api/v1/mobile/conversations/{id}/pieces-jointes",conversation).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/mobile/conversations/{id}/pieces-jointes",conversation).header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/mobile/conversations/{id}/pieces-jointes/{piece}/contenu",conversation,id).header("Authorization",bearer(doctorToken))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/mobile/conversations/{id}/messages",conversation).header("Authorization",bearer(token)).contentType("application/json")
            .content("{\"contenu\":\"Mon document\",\"piecesJointes\":[\""+id+"\"]}" )).andExpect(status().isCreated()).andExpect(jsonPath("$.piecesJointes[0].id").value(id));
        mvc.perform(get("/api/v1/mobile/conversations/{id}/pieces-jointes/{piece}/contenu",conversation,id).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(content().bytes(DocumentMedicalIntegrationTests.PDF));
        mvc.perform(get("/api/v1/mobile/conversations/{id}/pieces-jointes/{piece}/contenu",conversation,id).header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/mobile/conversations/{id}/messages",conversation).header("Authorization",bearer(doctorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].piecesJointes[0].id").value(id));
    }
}
