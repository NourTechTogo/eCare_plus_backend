package com.nourtech.ecareplus;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentMedicalIntegrationTests extends BackendFixture {
    @Test void depotAssisteParProfessionnelAffecteSansDroitCliniqueAdministratif()throws Exception{
        var patient=patientId();
        mvc.perform(multipart("/api/v1/portail/patients/{id}/documents",patient).file(fichier(PDF,"application/pdf")).param("typeDocument","ORDONNANCE").param("titre","Dépôt assisté").param("dateDocument","2026-01-01").param("sourceRef","assiste").header("Authorization",bearer(doctorToken)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.auteurId").value(doctor.toString()));
        mvc.perform(multipart("/api/v1/portail/patients/{id}/documents",patient).file(fichier(PDF,"application/pdf")).param("typeDocument","ORDONNANCE").param("titre","Refus").param("dateDocument","2026-01-01").param("sourceRef","admin").header("Authorization",bearer(adminToken))).andExpect(status().isForbidden());
    }
    static final byte[] PDF="%PDF-1.4\n1 0 obj<</Type /Catalog>>endobj\ntrailer<</Root 1 0 R>>\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    MockMultipartFile fichier(byte[] data,String mime){return new MockMultipartFile("fichier","ordonnance.pdf",mime,data);}
    @Test void deposePuisRelitDocumentApresReconnexion() throws Exception {
        var patient=patientId();var token=login();
        var document=body(mvc.perform(multipart("/api/v1/mobile/me/documents").file(fichier(PDF,"application/pdf"))
            .param("typeDocument","ORDONNANCE").param("titre","Ordonnance de suivi").param("dateDocument","2026-01-01").param("sourceRef","doc-1")
            .header("Authorization",bearer(token))).andExpect(status().isCreated()).andExpect(jsonPath("$.patientId").value(patient.toString()))
            .andExpect(jsonPath("$.cleStockage").doesNotExist()).andReturn());
        String id=document.path("id").asText();
        mvc.perform(get("/api/v1/mobile/me/documents/{id}/contenu",id).header("Authorization",bearer(login())))
            .andExpect(status().isOk()).andExpect(content().bytes(PDF)).andExpect(header().string("X-Content-Type-Options","nosniff"));
        mvc.perform(get("/api/v1/portail/patients/{patient}/documents/{id}/contenu",patient,id).header("Authorization",bearer(doctorToken))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/portail/patients/{patient}/documents/{id}/contenu",patient,id).header("Authorization",bearer(otherToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/portail/patients/{patient}/documents",patient).header("Authorization",bearer(adminToken))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/mobile/me/documents").header("Authorization",bearer(token))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test void refuseMimeMensongerEtTailleExcessive() throws Exception {
        patientId();var token=login();
        mvc.perform(multipart("/api/v1/mobile/me/documents").file(fichier("<script>alert(1)</script>".getBytes(),"application/pdf"))
            .param("typeDocument","AUTRE").param("titre","Faux PDF").param("dateDocument","2026-01-01").param("sourceRef","invalid")
            .header("Authorization",bearer(token))).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/mobile/me/documents").file(fichier(new byte[5*1024*1024+1],"application/pdf"))
            .param("typeDocument","AUTRE").param("titre","Trop grand").param("dateDocument","2026-01-01").param("sourceRef","big")
            .header("Authorization",bearer(token))).andExpect(status().isPayloadTooLarge());
    }
    @Test void reessaiDepotNeCreePasDeuxDocuments() throws Exception {
        patientId();var token=login();String id=null;
        for(int attempt=0;attempt<2;attempt++){
            var result=body(mvc.perform(multipart("/api/v1/mobile/me/documents").file(fichier(PDF,"application/pdf"))
                .param("typeDocument","ORDONNANCE").param("titre","Même document").param("dateDocument","2026-01-01").param("sourceRef","retry")
                .header("Authorization",bearer(token))).andExpect(status().isCreated()).andReturn());
            if(id==null)id=result.path("id").asText();else assertThat(result.path("id").asText()).isEqualTo(id);
        }
        mvc.perform(multipart("/api/v1/mobile/me/documents").file(fichier(PDF,"application/pdf"))
            .param("typeDocument","ORDONNANCE").param("titre","Autre titre").param("dateDocument","2026-01-01").param("sourceRef","retry")
            .header("Authorization",bearer(token))).andExpect(status().isConflict());
    }
    @Test void suppressionInterditImmediatementLaLecture() throws Exception {
        patientId();var token=login();
        var document=body(mvc.perform(multipart("/api/v1/mobile/me/documents").file(fichier(PDF,"application/pdf"))
            .param("typeDocument","AUTRE").param("titre","À supprimer").param("dateDocument","2026-01-01").param("sourceRef","delete")
            .header("Authorization",bearer(token))).andExpect(status().isCreated()).andReturn());
        var id=document.path("id").asText();
        mvc.perform(delete("/api/v1/mobile/me/documents/{id}",id).header("Authorization",bearer(token))).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/mobile/me/documents/{id}",id).header("Authorization",bearer(token))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/mobile/me/documents/{id}/contenu",id).header("Authorization",bearer(token))).andExpect(status().isNotFound());
    }
}
